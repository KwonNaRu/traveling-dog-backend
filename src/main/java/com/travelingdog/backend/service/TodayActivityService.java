package com.travelingdog.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.gemini.GeminiContent;
import com.travelingdog.backend.dto.gemini.GeminiGenerationConfig;
import com.travelingdog.backend.dto.gemini.GeminiPart;
import com.travelingdog.backend.dto.gemini.GeminiRequest;
import com.travelingdog.backend.dto.gemini.GeminiResponse;
import com.travelingdog.backend.dto.todayActivity.SaveActivityRequestDTO;
import com.travelingdog.backend.dto.todayActivity.SavedActivityResponseDTO;
import com.travelingdog.backend.dto.todayActivity.TodayActivityRequestDTO;
import com.travelingdog.backend.dto.todayActivity.TodayActivityResponseDTO;
import com.travelingdog.backend.exception.ExternalApiException;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.model.SavedActivity;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.SavedActivityRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodayActivityService {

    private static final Logger log = LoggerFactory.getLogger(TodayActivityService.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final RestClient restClient;
    private final SavedActivityRepository savedActivityRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 당일 활동 추천을 생성합니다.
     */
    public TodayActivityResponseDTO generateTodayActivity(TodayActivityRequestDTO request) {
        try {
            // 1. AI 프롬프트 생성
            String prompt = createTodayActivityPrompt(request);

            // 2. Gemini API 호출
            String aiResponse = callGeminiApi(prompt);

            // 3. AI 응답 파싱
            return parseAiResponse(aiResponse, request);

        } catch (ExternalApiException e) {
            log.error("AI API 호출 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("당일 활동 추천 생성 중 오류 발생: {}", e.getMessage());
            throw new InvalidRequestException("당일 활동 추천 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 당일 활동 추천을 위한 AI 프롬프트를 생성합니다.
     */
    private String createTodayActivityPrompt(TodayActivityRequestDTO request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 여행 전문가입니다. 다음 조건에 맞는 당일 활동을 추천해주세요.\n\n");

        prompt.append("**위치**: ").append(request.getLocation()).append("\n");
        prompt.append("**요청 개수**:\n");
        prompt.append("- 맛집: ").append(request.getRestaurantCount()).append("개\n");
        prompt.append("- 관광/문화: ").append(request.getCultureCount()).append("개\n");
        prompt.append("- 쇼핑/엔터테인먼트: ").append(request.getShoppingCount()).append("개\n");
        prompt.append("- 자연/휴식: ").append(request.getNatureCount()).append("개\n\n");

        prompt.append("**응답 조건**:\n");
        prompt.append("1. 각 장소의 정확한 위치를 구글맵 기준으로 제공하고, 반드시 실제로 존재하는 장소만 추천해주세요.\n");
        prompt.append("2. 현재 위치에서 접근 가능한 장소들을 우선적으로 추천해주세요.\n\n");

        prompt.append("**JSON 형식으로 응답해주세요**:\n");
        prompt.append("{\n");
        prompt.append("  \"restaurants\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"locationName\": \"정확한 위치명(문자열, 예: 잇푸도 라멘 하카타점 (Ippudo Ramen Hakata Branch))\",\n");
        prompt.append("      \"category\": \"(문자열, 예: 일식)\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"cultureSpots\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"locationName\": \"정확한 위치명(문자열, 예: 후쿠오카 타워 (Fukuoka Tower))\",\n");
        prompt.append("      \"category\": \"(문자열, 예: 관광)\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"shoppingSpots\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"locationName\": \"정확한 위치명(문자열, 예: 캐널시티 하카타 (Canal City Hakata))\",\n");
        prompt.append("      \"category\": \"(문자열, 예: 쇼핑)\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"natureSpots\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"locationName\": \"정확한 위치명(문자열, 예: 모모치 해변 (Momochibashi Beach))\",\n");
        prompt.append("      \"category\": \"(문자열, 예: 자연/휴식)\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        prompt.append("각 카테고리에서 요청된 개수만큼 정확히 추천해주세요. 개수가 0인 경우 빈 배열 []로 응답해주세요.");

        return prompt.toString();
    }

    /**
     * Gemini API를 호출합니다.
     */
    private String callGeminiApi(String prompt) {
        try {
            // Gemini API 요청 구성
            GeminiRequest geminiRequest = new GeminiRequest();

            List<GeminiContent> contents = new ArrayList<>();
            GeminiContent content = new GeminiContent();

            List<GeminiPart> parts = new ArrayList<>();
            GeminiPart part = new GeminiPart();
            part.setText(prompt);
            parts.add(part);

            content.setParts(parts);
            contents.add(content);
            geminiRequest.setContents(contents);

            // Generation config 설정
            GeminiGenerationConfig generationConfig = new GeminiGenerationConfig();
            generationConfig.setTemperature(0.3f);
            generationConfig.setTopK(40);
            generationConfig.setTopP(1);
            generationConfig.setMaxOutputTokens(8192);
            geminiRequest.setGenerationConfig(generationConfig);

            // API 호출
            GeminiResponse geminiResponse = restClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(geminiRequest)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (geminiResponse != null &&
                    geminiResponse.getCandidates() != null &&
                    !geminiResponse.getCandidates().isEmpty() &&
                    geminiResponse.getCandidates().get(0).getContent() != null &&
                    geminiResponse.getCandidates().get(0).getContent().getParts() != null &&
                    !geminiResponse.getCandidates().get(0).getContent().getParts().isEmpty()) {

                return geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
            }

            throw new ExternalApiException("Gemini API로부터 유효한 응답을 받지 못했습니다.");

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생: {}", e.getMessage());
            throw new ExternalApiException("AI API 호출에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * AI 응답을 파싱하여 TodayActivityResponseDTO로 변환합니다.
     */
    private TodayActivityResponseDTO parseAiResponse(String aiResponse, TodayActivityRequestDTO request) {
        try {
            // JSON 응답에서 코드 블록 제거
            String cleanedResponse = aiResponse;
            if (aiResponse.contains("```json")) {
                cleanedResponse = aiResponse.substring(
                        aiResponse.indexOf("```json") + 7,
                        aiResponse.lastIndexOf("```")).trim();
            } else if (aiResponse.contains("```")) {
                cleanedResponse = aiResponse.substring(
                        aiResponse.indexOf("```") + 3,
                        aiResponse.lastIndexOf("```")).trim();
            }

            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(cleanedResponse);

            TodayActivityResponseDTO response = new TodayActivityResponseDTO();
            response.setLocation(request.getLocation());
            response.setCreatedAt(LocalDateTime.now());

            // 각 카테고리별 파싱
            response.setRestaurants(parseActivityList(rootNode.get("restaurants")));
            response.setCultureSpots(parseActivityList(rootNode.get("cultureSpots")));
            response.setShoppingSpots(parseActivityList(rootNode.get("shoppingSpots")));
            response.setNatureSpots(parseActivityList(rootNode.get("natureSpots")));

            return response;

        } catch (JsonProcessingException e) {
            log.error("AI 응답 JSON 파싱 실패: {}", e.getMessage());
            throw new InvalidRequestException("AI 응답을 처리하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * JsonNode를 ActivityRecommendation 리스트로 변환합니다.
     */
    private List<TodayActivityResponseDTO.ActivityRecommendation> parseActivityList(JsonNode arrayNode) {
        List<TodayActivityResponseDTO.ActivityRecommendation> activities = new ArrayList<>();

        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                TodayActivityResponseDTO.ActivityRecommendation activity = new TodayActivityResponseDTO.ActivityRecommendation();

                activity.setLocationName(getStringValue(node, "locationName"));
                activity.setCategory(getStringValue(node, "category"));

                activities.add(activity);
            }
        }

        return activities;
    }

    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }

    private Integer getIntegerValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asInt() : null;
    }

    private Double getDoubleValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asDouble() : null;
    }

    /**
     * 활동을 저장합니다.
     */
    public SavedActivityResponseDTO saveActivity(SaveActivityRequestDTO request, User user) {
        try {
            // 중복 저장 확인
            Optional<SavedActivity> existingActivity = savedActivityRepository
                    .findByUserAndLocationNameAndCategory(user, request.getLocationName(), request.getCategory());

            if (existingActivity.isPresent()) {
                throw new InvalidRequestException("이미 저장된 활동입니다.");
            }

            // 새로운 활동 저장
            SavedActivity savedActivity = SavedActivity.builder()
                    .user(user)
                    .locationName(request.getLocationName())
                    .category(request.getCategory())
                    .savedLocation(request.getSavedLocation())
                    .build();

            SavedActivity saved = savedActivityRepository.save(savedActivity);

            log.info("활동 저장 완료 - 사용자: {}, 활동명: {}, 카테고리: {}",
                    user.getEmail(), request.getLocationName(), request.getCategory());

            return SavedActivityResponseDTO.fromEntity(saved);

        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("활동 저장 중 오류 발생: {}", e.getMessage());
            throw new InvalidRequestException("활동 저장에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자의 저장된 활동 목록을 조회합니다.
     */
    public List<SavedActivityResponseDTO> getSavedActivities(User user) {
        try {
            List<SavedActivity> savedActivities = savedActivityRepository.findByUserOrderByCreatedAtDesc(user);

            return savedActivities.stream()
                    .map(SavedActivityResponseDTO::fromEntity)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("저장된 활동 조회 중 오류 발생: {}", e.getMessage());
            throw new InvalidRequestException("저장된 활동 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 카테고리별 저장된 활동 목록을 조회합니다.
     */
    public List<SavedActivityResponseDTO> getSavedActivitiesByCategory(User user, String category) {
        try {
            List<SavedActivity> savedActivities = savedActivityRepository
                    .findByUserAndCategoryOrderByCreatedAtDesc(user, category);

            return savedActivities.stream()
                    .map(SavedActivityResponseDTO::fromEntity)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("카테고리별 저장된 활동 조회 중 오류 발생: {}", e.getMessage());
            throw new InvalidRequestException("저장된 활동 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 저장된 활동을 삭제합니다.
     */
    public void deleteSavedActivity(Long activityId, User user) {
        try {
            SavedActivity savedActivity = savedActivityRepository.findById(activityId)
                    .orElseThrow(() -> new ResourceNotFoundException("저장된 활동을 찾을 수 없습니다."));

            // 소유자 확인
            if (!savedActivity.getUser().getId().equals(user.getId())) {
                throw new InvalidRequestException("삭제 권한이 없습니다.");
            }

            savedActivityRepository.delete(savedActivity);

            log.info("저장된 활동 삭제 완료 - 사용자: {}, 활동 ID: {}", user.getEmail(), activityId);

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("저장된 활동 삭제 중 오류 발생: {}", e.getMessage());
            throw new InvalidRequestException("저장된 활동 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자의 저장된 활동 개수를 조회합니다.
     */
    public long getSavedActivityCount(User user) {
        return savedActivityRepository.countByUser(user);
    }
}