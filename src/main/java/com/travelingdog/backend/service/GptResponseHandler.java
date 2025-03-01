package com.travelingdog.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.AIRecommendedLocationDTO;
import com.travelingdog.backend.model.FailedGptResponse;
import com.travelingdog.backend.repository.FailedGptResponseRepository;

@Service
public class GptResponseHandler {

    private static final Logger log = LoggerFactory.getLogger(GptResponseHandler.class);

    @Autowired
    private FailedGptResponseRepository failedResponseRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * GPT 응답을 파싱하여 AIRecommendedLocationDTO 리스트로 변환합니다.
     * 다양한 형태의 응답을 처리할 수 있습니다.
     */
    public List<AIRecommendedLocationDTO> parseGptResponse(String content) {
        try {
            // 응답 정규화 (코드 블록, 추가 텍스트 등 제거)
            String normalizedContent = normalizeGptResponse(content);

            // JSON 파싱
            List<AIRecommendedLocationDTO> dtoList = objectMapper.readValue(normalizedContent,
                    new TypeReference<List<AIRecommendedLocationDTO>>() {
                    });

            // 빈 응답 체크
            if (dtoList.isEmpty()) {
                logFailedResponse(content, "빈 응답이 반환되었습니다.");
                throw new IllegalArgumentException("빈 응답이 반환되었습니다.");
            }

            // 필수 필드 검증
            validateRequiredFields(dtoList, content);

            return dtoList;
        } catch (JsonProcessingException e) {
            logFailedResponse(content, "JSON 파싱 실패: " + e.getMessage());
            throw new IllegalArgumentException("JSON 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * 응답에서 JSON 배열 부분만 추출합니다.
     */
    private String normalizeGptResponse(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("응답이 비어 있습니다.");
        }

        // 코드 블록 제거 (```json ... ```)
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*(.+?)\\s*```", Pattern.DOTALL);
        Matcher codeBlockMatcher = codeBlockPattern.matcher(content);
        if (codeBlockMatcher.find()) {
            return codeBlockMatcher.group(1);
        }

        // JSON 배열 추출 ([{...}])
        Pattern jsonArrayPattern = Pattern.compile("\\[\\s*\\{.+?\\}\\s*\\]", Pattern.DOTALL);
        Matcher jsonArrayMatcher = jsonArrayPattern.matcher(content);
        if (jsonArrayMatcher.find()) {
            return jsonArrayMatcher.group();
        }

        // 정규화 실패 시 원본 반환 (파싱 시도)
        return content;
    }

    /**
     * 필수 필드가 모두 존재하는지 검증합니다.
     */
    private void validateRequiredFields(List<AIRecommendedLocationDTO> dtoList, String originalContent) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (AIRecommendedLocationDTO dto : dtoList) {
            StringBuilder errorMessage = new StringBuilder();

            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                errorMessage.append("name 필드가 누락되었습니다. ");
            }

            if (dto.getAvailableDate() == null || dto.getAvailableDate().trim().isEmpty()) {
                errorMessage.append("availableDate 필드가 누락되었습니다. ");
            } else {
                try {
                    LocalDate.parse(dto.getAvailableDate(), formatter);
                } catch (DateTimeParseException e) {
                    errorMessage.append("availableDate 형식이 잘못되었습니다(yyyy-MM-dd 형식이어야 함). ");
                }
            }

            if (errorMessage.length() > 0) {
                logFailedResponse(originalContent, "필수 필드가 누락되었거나 형식이 잘못되었습니다: " + errorMessage.toString());
                throw new IllegalArgumentException("필수 필드가 누락되었거나 형식이 잘못되었습니다: " + errorMessage.toString());
            }
        }
    }

    /**
     * 실패한 응답을 로깅하고 저장합니다.
     */
    private void logFailedResponse(String response, String errorMessage) {
        log.error("GPT 응답 처리 실패: {}", errorMessage);

        FailedGptResponse failedResponse = FailedGptResponse.builder()
                .response(response)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();

        failedResponseRepository.save(failedResponse);
    }

    /**
     * 강화된 프롬프트를 생성합니다.
     */
    public String createEnhancedPrompt(String country, String city, String startDate, String endDate) {
        return "다음 정보를 기반으로, 해당 도시의 추천 맛집 및 관광지 정보를 JSON 배열 형식으로 생성해줘. "
                + "각 객체는 반드시 다음 형식을 따라야 함: "
                + "{"
                + "\"name\": \"장소명(문자열)\", "
                + "\"latitude\": 위도(숫자), "
                + "\"longitude\": 경도(숫자), "
                + "\"availableDate\": \"yyyy-MM-dd 형식의 날짜(문자열)\""
                + "} "
                + "추가 텍스트나 설명 없이 순수 JSON 배열만 출력해줘. "
                + "입력 정보 - 국가: " + country
                + ", 도시: " + city
                + ", 여행 시작일: " + startDate
                + ", 여행 종료일: " + endDate + ".";
    }

    /**
     * GPT 응답 파싱에 실패했을 때 사용할 대체 응답을 제공합니다.
     */
    public List<AIRecommendedLocationDTO> getFallbackResponse(String country, String city, String startDate,
            String endDate) {
        List<AIRecommendedLocationDTO> fallbackList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);

        // 서울의 경우 기본 관광지 제공
        if ("Seoul".equalsIgnoreCase(city)) {
            AIRecommendedLocationDTO location1 = new AIRecommendedLocationDTO();
            location1.setName("Gyeongbokgung Palace");
            location1.setLatitude(37.5796);
            location1.setLongitude(126.9770);
            location1.setAvailableDate(start.format(formatter));
            fallbackList.add(location1);

            AIRecommendedLocationDTO location2 = new AIRecommendedLocationDTO();
            location2.setName("Namsan Tower");
            location2.setLatitude(37.5512);
            location2.setLongitude(126.9882);
            location2.setAvailableDate(start.plusDays(1).format(formatter));
            fallbackList.add(location2);
        }
        // 다른 도시의 경우 기본 응답 제공
        else {
            AIRecommendedLocationDTO location = new AIRecommendedLocationDTO();
            location.setName("Popular Attraction in " + city);
            location.setLatitude(37.5665);
            location.setLongitude(126.9780);
            location.setAvailableDate(start.format(formatter));
            fallbackList.add(location);
        }

        return fallbackList;
    }
}