package com.travelingdog.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.gemini.GeminiContent;
import com.travelingdog.backend.dto.gemini.GeminiGenerationConfig;
import com.travelingdog.backend.dto.gemini.GeminiResponse;
import com.travelingdog.backend.dto.gemini.GeminiPart;
import com.travelingdog.backend.dto.gemini.GeminiRequest;
import com.travelingdog.backend.dto.restaurant.RestaurantRecommendationRequestDTO;
import com.travelingdog.backend.dto.restaurant.RestaurantRecommendationResponseDTO;
import com.travelingdog.backend.exception.ExternalApiException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.repository.TravelPlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantRecommendationService {

    private final TravelPlanRepository travelPlanRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    /**
     * 여행 계획 ID를 기반으로 맛집 추천을 생성
     */
    public RestaurantRecommendationResponseDTO generateRestaurantRecommendations(
            Long travelPlanId,
            RestaurantRecommendationRequestDTO requestDTO) {

        // 여행 계획 조회
        TravelPlan travelPlan = travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        // 프롬프트 생성
        String prompt = createRestaurantPrompt(travelPlan, requestDTO);

        // AI 호출
        String aiResponse = callAI(prompt);

        // 응답 파싱
        return parseAIResponse(aiResponse);
    }

    private String createRestaurantPrompt(TravelPlan travelPlan, RestaurantRecommendationRequestDTO requestDTO) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("다음 여행 계획에 맞는 맛집을 추천해주세요.\n\n");
        prompt.append("=== 여행 정보 ===\n");
        prompt.append("여행지: ").append(travelPlan.getCity()).append("\n");
        prompt.append("여행 기간: ").append(travelPlan.getStartDate()).append(" ~ ").append(travelPlan.getEndDate())
                .append("\n");

        // 여행 계획의 관심사나 스타일이 있다면 추가
        if (travelPlan.getTravelStyles() != null && !travelPlan.getTravelStyles().isEmpty()) {
            prompt.append("여행 스타일: ");
            travelPlan.getTravelStyles().forEach(style -> prompt.append(style.getName()).append(", "));
            prompt.append("\n");
        }

        if (travelPlan.getInterests() != null && !travelPlan.getInterests().isEmpty()) {
            prompt.append("관심사: ");
            travelPlan.getInterests().forEach(interest -> prompt.append(interest.getName()).append(", "));
            prompt.append("\n");
        }

        prompt.append("\n=== 맛집 추천 요청 ===\n");

        if (requestDTO.getMealType() != null && !requestDTO.getMealType().isEmpty()) {
            prompt.append("식사 유형: ").append(requestDTO.getMealType()).append("\n");
        }

        if (requestDTO.getCuisine() != null && !requestDTO.getCuisine().isEmpty()) {
            prompt.append("음식 종류: ").append(requestDTO.getCuisine()).append("\n");
        }

        if (requestDTO.getPriceRange() != null && !requestDTO.getPriceRange().isEmpty()) {
            prompt.append("가격대: ").append(requestDTO.getPriceRange()).append("\n");
        }

        if (requestDTO.getNumberOfPeople() != null) {
            prompt.append("인원 수: ").append(requestDTO.getNumberOfPeople()).append("명\n");
        }

        if (requestDTO.getSpecialRequests() != null && !requestDTO.getSpecialRequests().isEmpty()) {
            prompt.append("특별 요청: ").append(requestDTO.getSpecialRequests()).append("\n");
        }

        prompt.append("\n맛집을 5-10개 정도 추천해주세요. 다음 JSON 형식으로 응답해주세요:\n\n");
        prompt.append("{\n");
        prompt.append("  \"restaurants\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"location_name\": \"정확한 맛집 이름(구글맵에서 검색 가능한 명칭)\",\n");
        prompt.append("      \"description\": \"맛집 설명 및 특징\",\n");
        prompt.append("      \"cuisine\": \"음식 종류\",\n");
        prompt.append("      \"price_range\": \"가격대 (저렴/보통/비싼)\",\n");
        prompt.append("      \"estimated_cost\": \"1인당 예상 비용\",\n");
        prompt.append("      \"address\": \"주소\",\n");
        prompt.append("      \"opening_hours\": \"운영시간\",\n");
        prompt.append("      \"phone\": \"전화번호 (있다면)\",\n");
        prompt.append("      \"rating\": 4.5,\n");
        prompt.append("      \"recommended_dishes\": [\"추천 메뉴1\", \"추천 메뉴2\"]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private String callAI(String prompt) {
        try {
            List<GeminiPart> parts = new ArrayList<>();
            parts.add(GeminiPart.builder()
                    .text(prompt)
                    .build());

            List<GeminiContent> contents = new ArrayList<>();
            contents.add(GeminiContent.builder()
                    .parts(parts)
                    .build());
            // Gemini API 요청 구성
            GeminiRequest geminiRequest = GeminiRequest.builder()
                    .contents(contents)
                    .generationConfig(GeminiGenerationConfig.builder()
                            .temperature(0.3f)
                            .topK(1)
                            .topP(1)
                            .maxOutputTokens(4096)
                            .build())
                    .build();

            GeminiResponse geminiResponse = restClient.post()
                    .uri(openaiApiUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                    .body(geminiRequest)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                return geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
            } else {
                throw new ExternalApiException("AI 응답이 비어있습니다.");
            }

        } catch (Exception e) {
            log.error("AI 호출 중 오류 발생: ", e);
            throw new ExternalApiException("AI 서비스 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private RestaurantRecommendationResponseDTO parseAIResponse(String aiResponse) {
        try {
            // JSON 응답 파싱
            return objectMapper.readValue(aiResponse, RestaurantRecommendationResponseDTO.class);
        } catch (JsonProcessingException e) {
            log.error("AI 응답 파싱 중 오류 발생: ", e);
            log.error("AI 응답 내용: {}", aiResponse);
            throw new ExternalApiException("AI 응답을 파싱하는 중 오류가 발생했습니다.");
        }
    }
}