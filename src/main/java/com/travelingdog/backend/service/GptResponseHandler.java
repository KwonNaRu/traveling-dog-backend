package com.travelingdog.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.AIRecommendedItineraryDTO;
import com.travelingdog.backend.dto.AIRecommendedTravelPlanDTO;
import com.travelingdog.backend.exception.ExternalApiException;
import com.travelingdog.backend.model.FailedGptResponse;
import com.travelingdog.backend.repository.FailedGptResponseRepository;

@Service
public class GptResponseHandler {

    private static final Logger log = LoggerFactory.getLogger(GptResponseHandler.class);

    @Autowired
    private FailedGptResponseRepository failedResponseRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * GPT 응답을 파싱하여 AIRecommendedLocationDTO 리스트로 변환합니다. 다양한 형태의 응답을 처리할 수 있습니다.
     */
    public AIRecommendedTravelPlanDTO parseGptResponse(String content) {
        try {
            // 응답 정규화 (코드 블록, 추가 텍스트 등 제거)
            String normalizedContent = normalizeGptResponse(content);

            // 전체 JSON을 AIRecommendedTravelPlanDTO로 파싱
            AIRecommendedTravelPlanDTO travelPlanDTO = objectMapper.readValue(normalizedContent,
                    AIRecommendedTravelPlanDTO.class);

            // 필수 필드 검증
            validateRequiredFields(travelPlanDTO);

            return travelPlanDTO;
        } catch (JsonProcessingException e) {
            logFailedResponse(content, "JSON 파싱 실패: " + e.getMessage());
            throw new ExternalApiException("JSON 파싱 실패: " + e.getMessage());
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            logFailedResponse(content, "응답 처리 중 오류 발생: " + e.getMessage());
            throw new ExternalApiException("응답 처리 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 응답에서 JSON 객체 부분만 추출합니다.
     */
    private String normalizeGptResponse(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ExternalApiException("응답이 비어 있습니다.");
        }

        // 코드 블록 제거 (```json ... ```)
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*(.+?)\\s*```", Pattern.DOTALL);
        Matcher codeBlockMatcher = codeBlockPattern.matcher(content);
        if (codeBlockMatcher.find()) {
            return codeBlockMatcher.group(1);
        }

        // JSON 객체 추출 - 전체 JSON 구조를 찾기 위해 trip_name부터 transportation_tips까지 포함하는 패턴 사용
        Pattern jsonObjectPattern = Pattern.compile("\\{.*?\"trip_name\".*?\"transportation_tips\".*?\\}",
                Pattern.DOTALL);
        Matcher jsonObjectMatcher = jsonObjectPattern.matcher(content);
        if (jsonObjectMatcher.find()) {
            return jsonObjectMatcher.group();
        }

        // 정규화 실패 시 원본 반환 (파싱 시도)
        return content;
    }

    /**
     * 필수 필드가 모두 존재하는지 검증합니다.
     */
    private void validateRequiredFields(AIRecommendedTravelPlanDTO travelPlanDTO) {
        StringBuilder errorMessage = new StringBuilder();

        if (travelPlanDTO.getTripName() == null || travelPlanDTO.getTripName().trim().isEmpty()) {
            errorMessage.append("trip_name 필드가 누락되었습니다. ");
        }

        if (travelPlanDTO.getItinerary() == null || travelPlanDTO.getItinerary().isEmpty()) {
            errorMessage.append("itinerary 필드가 누락되었거나 비어 있습니다. ");
        } else {
            // itinerary 내부 항목 검증
            for (AIRecommendedItineraryDTO dto : travelPlanDTO.getItinerary()) {
                if (dto.getLocation() == null || dto.getLocation().trim().isEmpty()) {
                    errorMessage.append("itinerary 항목 중 location 필드가 누락되었습니다. ");
                    break;
                }
            }
        }

        if (errorMessage.length() > 0) {
            throw new ExternalApiException("필수 필드가 누락되었거나 형식이 잘못되었습니다: " + errorMessage.toString());
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
    public String createEnhancedPrompt(String country, String city, LocalDate startDate, LocalDate endDate,
            String season, String travelStyle, String budget, String interests, String accommodation,
            String transportation) {
        return "다음 정보를 기반으로, 사용자의 여행 계획을 JSON 형식으로 생성해줘. 각 날짜별 일정에는 점심과 저녁 식사 정보, 그리고 지도에 표시할 수 있도록 각 장소의 정확한 위도(latitude)와 경도(longitude) 정보를 포함해야 해. 반드시 다음 형식을 따라야 하며, 추가 텍스트나 설명 없이 순수 JSON 객체만 출력해줘."
                + "{"
                + "\"trip_name\": \"여행 이름(문자열, 예: 오키나와 5박 6일 자유여행)\","
                + "\"start_date\": \"여행 시작일(YYYY-MM-DD 형식, 예: 2024-07-01)\","
                + "\"end_date\": \"여행 종료일(YYYY-MM-DD 형식, 예: 2024-07-06)\","
                + "\"season\": \"여행 계절(문자열, 예: 여름)\","
                + "\"travel_style\": [\"여행 스타일1(문자열, 예: 해변)\", \"여행 스타일2(문자열, 예: 자연 풍경 감상)\", ...],"
                + "\"budget\": \"예산(문자열, 예: 100만원)\","
                + "\"destination\": \"도시(문자열, 예: 오키나와)\","
                + "\"interests\": [\"관심사1(문자열, 예: 유명 맛집 방문)\", \"관심사2(문자열, 예: 전통 축제 참여)\", ...],"
                + "\"accommodation\": [\"숙소 유형1(문자열, 예: 캡슐호텔)\", \"숙소 유형2(문자열, 예: 료칸)\", ...],"
                + "\"transportation\": [\"교통 수단1(문자열, 예: 지하철)\", \"교통 수단2(문자열, 예: 버스)\", ...],"
                + "\"itinerary\": ["
                + "{"
                + "\"date\": 일자(숫자),"
                + "\"location\": \"위치(문자열, 예: 나하 시내)\","
                + "\"activities\": ["
                + "{"
                + "\"name\": \"장소명(문자열, 예: 나하 공항)\","
                + "\"latitude\": 위도(숫자, 구글맵 기준, 예: 26.2097),"
                + "\"longitude\": 경도(숫자, 구글맵 기준, 예: 127.6465),"
                + "\"description\": \"활동 설명(문자열, 예: 나하 공항 도착 및 캡슐호텔 체크인)\""
                + "},"
                + "...],"
                + "\"lunch\": {"
                + "\"name\": \"장소명(문자열, 예: 키시모토 식당)\","
                + "\"latitude\": 위도(숫자, 구글맵 기준, 예: 26.2122),"
                + "\"longitude\": 경도(숫자, 구글맵 기준, 예: 127.6861),"
                + "\"description\": \"점심 식사 정보(문자열, 예: 오키나와 소바 점심 식사)\""
                + "},"
                + "\"dinner\": {"
                + "\"name\": \"장소명(문자열, 예: 얏빠리 스테이크)\","
                + "\"latitude\": 위도(숫자, 구글맵 기준, 예: 26.2130),"
                + "\"longitude\": 경도(숫자, 구글맵 기준, 예: 127.6859),"
                + "\"description\": \"저녁 식사 정보(문자열, 예: 오키나와 스테이크 저녁 식사)\""
                + "},"
                + "...]"
                + "},"
                + "...],"
                + "\"restaurant_recommendations\": ["
                + "{"
                + "\"name\": \"장소명(문자열, 예: 얏빠리 스테이크)\","
                + "\"latitude\": 위도(숫자, 구글맵 기준, 예: 26.2130),"
                + "\"longitude\": 경도(숫자, 구글맵 기준, 예: 127.6859),"
                + "\"description\": \"맛집 설명(문자열, 예: 오키나와 스테이크 맛집)\""
                + "},"
                + "...],"
                + "\"accommodation_recommendations\": ["
                + "{"
                + "\"name\": \"장소명(문자열, 예: 더 캡슐 호텔 & 커피 나하)\","
                + "\"latitude\": 위도(숫자, 구글맵 기준, 예: 26.2144),"
                + "\"longitude\": 경도(숫자, 구글맵 기준, 예: 127.6841),"
                + "\"description\": \"숙소 설명(문자열, 예: 캡슐호텔)\""
                + "},"
                + "...],"
                + "\"transportation_tips\": \"교통 팁(문자열, 예: 오키나와는 지하철이 없고 버스와 모노레일을 주로 이용합니다. 오키나와 버스는 한국의 티머니와 같은 개념의 오키카 카드를 구매하여 사용하는 것이 편합니다.)\""
                + "}"
                + "위도와 경도는 반드시 구글맵에서 사용하는 좌표계(WGS84)를 기준으로 정확한 값을 제공해야 함. "
                + "각 장소의 정확한 위치를 구글맵 기준으로 제공하고, 실제 존재하는 장소만 추천해줘. "
                + "입력 정보 - 여행 시작일: " + startDate
                + ", 여행 종료일: " + endDate
                + ", 여행 계절: " + season
                + ", 여행 스타일: " + travelStyle
                + ", 예산: " + budget
                + ", 도시: " + city
                + ", 관심사: " + interests
                + ", 숙소 유형: " + accommodation
                + ", 교통 수단: " + transportation + ".";
    }

    /**
     * GPT 응답 파싱에 실패했을 때 사용할 대체 응답을 제공합니다.
     */
    public AIRecommendedTravelPlanDTO getFallbackResponse(String country, String city, LocalDate startDate,
            LocalDate endDate) {
        AIRecommendedTravelPlanDTO fallbackList = new AIRecommendedTravelPlanDTO();
        fallbackList.setTripName(city + " 여행");
        fallbackList.setItinerary(new ArrayList<>());

        // 서울의 경우 기본 관광지 제공
        if ("Seoul".equalsIgnoreCase(city)) {
            AIRecommendedItineraryDTO location1 = new AIRecommendedItineraryDTO();
            location1.setLocation("Gyeongbokgung Palace");
            fallbackList.getItinerary().add(location1);

            AIRecommendedItineraryDTO location2 = new AIRecommendedItineraryDTO();
            location2.setLocation("Namsan Tower");
            fallbackList.getItinerary().add(location2);
        } // 다른 도시의 경우 기본 응답 제공
        else {
            AIRecommendedItineraryDTO location = new AIRecommendedItineraryDTO();
            location.setLocation("Popular Attraction in " + city);
            fallbackList.getItinerary().add(location);
        }

        return fallbackList;
    }
}
