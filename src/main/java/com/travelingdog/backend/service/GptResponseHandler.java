package com.travelingdog.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import com.travelingdog.backend.dto.travelPlan.UserSpecifiedAccommodation;
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
    public String createEnhancedPrompt(String city, LocalDate startDate, LocalDate endDate, String travelStyle,
            String interests, String accommodation, String transportation,
            List<UserSpecifiedAccommodation> userSpecifiedAccommodation) {

        String userAccommodationJson;
        try {
            userAccommodationJson = objectMapper.writeValueAsString(userSpecifiedAccommodation);
        } catch (JsonProcessingException e) {
            userAccommodationJson = "숙소 정보가 없습니다.";
            log.error("숙소 정보 변환 중 오류 발생: {}", e.getMessage());
        }

        // 값이 있는 경우에만 프롬프트에 포함할 추가 정보 생성
        StringBuilder additionalInfo = new StringBuilder();
        additionalInfo.append("입력 정보 - 여행 시작일: ").append(startDate);
        additionalInfo.append(", 여행 종료일: ").append(endDate);
        additionalInfo.append(", 도시: ").append(city);

        if (travelStyle != null && !travelStyle.trim().isEmpty()) {
            additionalInfo.append(", 여행 스타일: ").append(travelStyle);
        }

        if (interests != null && !interests.trim().isEmpty()) {
            additionalInfo.append(", 관심사: ").append(interests);
        }

        if (accommodation != null && !accommodation.trim().isEmpty()) {
            additionalInfo.append(", 숙소 유형: ").append(accommodation);
        }

        if (transportation != null && !transportation.trim().isEmpty()) {
            additionalInfo.append(", 교통 수단: ").append(transportation);
        }

        additionalInfo.append(".");

        // 숙소 관련 지시사항 - accommodation이 있는 경우에만 특별 지시
        String accommodationInstruction = "";
        if (accommodation != null && !accommodation.trim().isEmpty()) {
            accommodationInstruction = "만약 특정 날짜에 숙소 정보가 없다면, '" + accommodation
                    + "'를 기반으로 여행 전체 기간 동안 사용할 하나의 추천 숙소를 선택하여 해당 숙소 정보를 숙소가 지정되지 않은 모든 날짜의 'activities' 배열에 포함시켜줘. ";
        } else {
            accommodationInstruction = "사용자가 별도로 숙소 유형을 지정하지 않았으므로, 해당 도시에 적합한 일반적인 숙소를 추천해줘. ";
        }

        // 빈 배열 지시사항 생성
        StringBuilder emptyArrayInstructions = new StringBuilder();
        emptyArrayInstructions.append("중요: 다음 필드들은 사용자가 지정하지 않은 경우 반드시 빈 배열 []로 응답해야 합니다: ");

        if (travelStyle == null || travelStyle.trim().isEmpty()) {
            emptyArrayInstructions.append("\"travel_style\": [], ");
        }
        if (interests == null || interests.trim().isEmpty()) {
            emptyArrayInstructions.append("\"interests\": [], ");
        }
        if (accommodation == null || accommodation.trim().isEmpty()) {
            emptyArrayInstructions.append("\"accommodation\": [], ");
        }
        if (transportation == null || transportation.trim().isEmpty()) {
            emptyArrayInstructions.append("\"transportation\": [], ");
        }

        // 마지막 쉼표 제거
        String emptyArrayInstr = emptyArrayInstructions.toString();
        if (emptyArrayInstr.endsWith(", ")) {
            emptyArrayInstr = emptyArrayInstr.substring(0, emptyArrayInstr.length() - 2);
        }

        return "다음 정보를 기반으로, 사용자의 여행 계획을 JSON 형식으로 생성해줘. 각 날짜별 일정에는 활동 정보가 'activities' 배열에 포함되어야 하며, 각 항목은 활동 제목, 설명, 위치 이름을 포함해야 해."
                + "사용자가 특정 날짜에 숙소를 지정했다면, 해당 숙소 정보가 그 날짜의 'activities' 배열에 포함되어야 해."
                + accommodationInstruction
                + "점심과 저녁 식사도 각각 하나의 활동으로 포함되어야 해."
                + "여행 시작일과 종료일, 그리고 사용자가 지정한 숙소 정보 및 선호 숙소 정보를 바탕으로 여행 계획을 생성해줘."
                + emptyArrayInstr + ". "
                + "반드시 다음 형식을 따라야 하며, 추가 텍스트나 설명 없이 순수 JSON 객체만 출력해줘."
                + "{"
                + "\"trip_name\": \"여행 이름(문자열, 예: 오키나와 5박 6일 자유여행)\","
                + "\"start_date\": \"여행 시작일(YYYY-MM-DD 형식, 예: 2024-07-01)\","
                + "\"end_date\": \"여행 종료일(YYYY-MM-DD 형식, 예: 2024-07-06)\","
                + "\"travel_style\": [\"여행 스타일1(문자열, 예: 해변)\", \"여행 스타일2(문자열, 예: 자연 풍경 감상)\", ...],"
                + "\"country\": \"국가(문자열, 예: 일본)\","
                + "\"destination\": \"도시(문자열, 예: 오키나와)\","
                + "\"interests\": [\"관심사1(문자열, 예: 유명 맛집 방문)\", \"관심사2(문자열, 예: 전통 축제 참여)\", ...],"
                + "\"accommodation\": [\"숙소 유형1(문자열, 예: 캡슐호텔)\", \"숙소 유형2(문자열, 예: 료칸)\", ...],"
                + "\"transportation\": [\"교통 수단1(문자열, 예: 지하철)\", \"교통 수단2(문자열, 예: 버스)\", ...],"
                + "\"itinerary\": ["
                + "{"
                + "\"date\": 일자(숫자),"
                + "\"location\": \"위치(문자열, 예: " + city + " 시내)\","
                + "\"activities\": ["
                + "{"
                + "\"title\": \"활동명(문자열, 예: 호텔 조식)\","
                + "\"location_name\": \"정확한 위치명(문자열, 예: 하카타 엑셀 호텔 도큐 조식 뷔페)\","
                + "\"description\": \"활동 설명(문자열, 예: 호텔 조식 뷔페 이용)\","
                + "\"cost\": \"예상 비용(문자열, 예: 150000원)\""
                + "},"
                + "{"
                + "\"title\": \"활동명(문자열, 예: 캐널시티 하카타 방문)\","
                + "\"location_name\": \"정확한 위치명(문자열, 예: 캐널시티 하카타 (Canal City Hakata))\","
                + "\"description\": \"활동 설명(문자열, 예: 캐널시티 하카타 쇼핑 및 분수 쇼 관람)\","
                + "\"cost\": \"예상 비용(문자열, 예: 10000원)\""
                + "},"
                + "{"
                + "\"title\": \"활동명(문자열, 예: 점심 식사: 잇푸도 라멘 하카타점)\","
                + "\"location_name\": \"정확한 위치명(문자열, 예: 잇푸도 라멘 하카타점 (Ippudo Ramen Hakata Branch))\","
                + "\"description\": \"활동 설명(문자열, 예: 일본 라멘 체인점 잇푸도에서 점심 식사)\","
                + "\"cost\": \"예상 비용(문자열, 예: 15000원)\""
                + "},"
                // + "{"
                // + "\"title\": \"활동명(문자열, 예: 후쿠오카 공항 이동)\","
                // + "\"location_name\": \"정확한 위치명(문자열, 예: 후쿠오카 공항 (Fukuoka Airport))\","
                // + "\"description\": \"활동 설명(문자열, 예: 후쿠오카 공항으로 이동 및 출국 준비)\","
                // + "\"cost\": \"예상 비용(문자열, 예: 6000원)\""
                // + "},"
                // + "{"
                // + "\"title\": \"활동명(문자열, 예: 후쿠오카 공항 출국)\","
                // + "\"location_name\": \"정확한 위치명(문자열, 예: 후쿠오카 공항 (Fukuoka Airport))\","
                // + "\"description\": \"활동 설명(문자열, 예: 후쿠오카 공항에서 출국)\","
                // + "\"cost\": \"예상 비용(문자열, 예: 120000원)\""
                // + "},"
                + "...]"
                + "},"
                + "...],"
                + "\"transportation_tips\": \"교통 팁(문자열, 예: 오키나와는 지하철이 없고 버스와 모노레일을 주로 이용합니다. 오키나와 버스는 한국의 티머니와 같은 개념의 오키카 카드를 구매하여 사용하는 것이 편합니다.)\""
                + "}"
                + "각 장소의 정확한 위치를 구글맵 기준으로 제공하고, 실제 존재하는 장소만 추천해줘. "
                + "여행 시작일과 종료일을 바탕으로 " + city + "의 날씨와 여행 시기를 고려하여 활동과 추천 장소를 포함해주세요."
                + "'activities' 배열의 각 항목은 활동 제목, 정확한 위치 이름, 설명을 포함해야 합니다. 점심과 저녁 식사는 'activities' 배열에 포함해주세요."
                + "점심과 저녁 식사의 title은 '점심 식사: 잇푸도 라멘 하카타점'과 같이 식사 장소의 이름을 포함해야 합니다."
                + "특히 'location_name' 필드에는 구글맵에서 정확하게 검색될 수 있는 위치의 명칭을 기재해야 합니다."
                + "사용자가 다음과 같이 숙소를 지정했습니다:" + userAccommodationJson + "."
                + "각 날짜별로 지정된 숙소가 있다면, 해당 숙소 이름을 해당 날짜의 'activities' 배열에 포함시켜줘."
                + additionalInfo.toString();
    }

    /**
     * GPT 응답 파싱에 실패했을 때 사용할 대체 응답을 제공합니다.
     */
    public AIRecommendedTravelPlanDTO getFallbackResponse(String city, LocalDate startDate, LocalDate endDate) {
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
