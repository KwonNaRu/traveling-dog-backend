package com.travelingdog.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.travelingdog.backend.dto.AIChatMessage;
import com.travelingdog.backend.dto.AIChatRequest;
import com.travelingdog.backend.dto.AIChatResponse;
import com.travelingdog.backend.dto.AIRecommendedLocationDTO;
import com.travelingdog.backend.dto.TravelPlanRequest;
import com.travelingdog.backend.exception.ExternalApiException;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.model.TravelLocation;

@Service
public class TripPlanService {

    private static final Logger log = LoggerFactory.getLogger(TripPlanService.class);

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final WebClient webClient;
    private final RouteOptimizationService routeOptimizationService;
    private final GptResponseHandler gptResponseHandler;

    @Autowired
    public TripPlanService(RouteOptimizationService routeOptimizationService, GptResponseHandler gptResponseHandler,
            WebClient webClient) {
        this.routeOptimizationService = routeOptimizationService;
        this.gptResponseHandler = gptResponseHandler;
        this.webClient = webClient;
    }

    /**
     * 프론트엔드에서 전달받은 여행 정보를 바탕으로 GPT API를 호출하고,
     * 추천 장소 정보를 JSON 배열로 받아 List<TravelLocation>으로 변환한 후
     * 경로 최적화를 적용하여 반환합니다.
     */
    public List<TravelLocation> generateTripPlan(TravelPlanRequest request) {
        try {
            // 강화된 프롬프트 생성
            String prompt = gptResponseHandler.createEnhancedPrompt(
                    request.getCountry(),
                    request.getCity(),
                    request.getStartDate(),
                    request.getEndDate());

            // GPT API 요청 메시지 구성
            List<AIChatMessage> messages = new ArrayList<>();
            messages.add(new AIChatMessage("system", "You are a travel recommendation assistant."));
            messages.add(new AIChatMessage("user", prompt));

            AIChatRequest openAiRequest = new AIChatRequest();
            openAiRequest.setModel("gpt-3.5-turbo");
            openAiRequest.setMessages(messages);
            openAiRequest.setTemperature(0.3); // 안정적인 JSON 응답을 위해 낮은 temperature 설정

            String openAiUrl = "https://api.openai.com/v1/chat/completions";

            AIChatResponse openAiResponse = webClient.post()
                    .uri(openAiUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                    .body(BodyInserters.fromValue(openAiRequest))
                    .retrieve()
                    .bodyToMono(AIChatResponse.class)
                    .block();

            if (openAiResponse != null && openAiResponse.getChoices() != null
                    && !openAiResponse.getChoices().isEmpty()) {
                String content = openAiResponse.getChoices().get(0).getMessage().getContent();

                try {
                    // 응답 파싱 및 검증
                    List<AIRecommendedLocationDTO> dtoList = gptResponseHandler.parseGptResponse(content);

                    // DTO → TravelLocation 변환
                    List<TravelLocation> locations = new ArrayList<>();
                    int order = 0;
                    for (AIRecommendedLocationDTO dto : dtoList) {
                        TravelLocation location = new TravelLocation();
                        location.setPlaceName(dto.getName());
                        location.setCoordinates(dto.getLongitude(), dto.getLatitude());
                        location.setLocationOrder(order++);
                        location.setDescription("");
                        location.setAvailableDate(LocalDate.parse(dto.getAvailableDate()));
                        // travelPlan 연관 관계는 저장할 때 할당
                        locations.add(location);
                    }

                    // 경로 최적화 실행: 시뮬레이티드 어닐링 알고리즘 사용
                    return routeOptimizationService.optimizeRouteWithSimulatedAnnealing(locations);

                    // 실제 교통 정보를 고려한 경로 최적화를 사용하려면 아래 코드 사용
                    // return routeOptimizationService.optimizeRouteWithRealDistances(locations);
                } catch (Exception e) {
                    log.error("GPT 응답 처리 중 오류 발생: {}", e.getMessage());
                    // 대체 응답 사용
                    List<AIRecommendedLocationDTO> fallbackList = gptResponseHandler.getFallbackResponse(
                            request.getCountry(),
                            request.getCity(),
                            request.getStartDate(),
                            request.getEndDate());

                    // 대체 응답으로 TravelLocation 생성
                    List<TravelLocation> fallbackLocations = new ArrayList<>();
                    int order = 0;
                    for (AIRecommendedLocationDTO dto : fallbackList) {
                        TravelLocation location = new TravelLocation();
                        location.setPlaceName(dto.getName());
                        location.setCoordinates(dto.getLongitude(), dto.getLatitude());
                        location.setLocationOrder(order++);
                        location.setDescription("대체 추천 장소");
                        location.setAvailableDate(LocalDate.parse(dto.getAvailableDate()));
                        fallbackLocations.add(location);
                    }
                    return routeOptimizationService.optimizeRouteWithSimulatedAnnealing(fallbackLocations);
                }
            }
            throw new ExternalApiException("GPT API 호출에 실패했습니다.");
        } catch (ExternalApiException e) {
            log.error("외부 API 호출 중 오류 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("여행 계획 생성 중 오류 발생: {}", e.getMessage());
            throw new InvalidRequestException("여행 계획 생성에 실패했습니다: " + e.getMessage());
        }
    }
}