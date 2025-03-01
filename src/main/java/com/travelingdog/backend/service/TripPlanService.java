package com.travelingdog.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.AIChatMessage;
import com.travelingdog.backend.dto.AIChatRequest;
import com.travelingdog.backend.dto.AIChatResponse;
import com.travelingdog.backend.dto.AIRecommendedLocationDTO;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.dto.TravelPlanRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TripPlanService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final RouteOptimizationService routeOptimizationService;

    public TripPlanService(RouteOptimizationService routeOptimizationService) {
        this.routeOptimizationService = routeOptimizationService;
    }

    /**
     * 프론트엔드에서 전달받은 여행 정보를 바탕으로 GPT API를 호출하고,
     * 추천 장소 정보를 JSON 배열로 받아 List<TravelLocation>으로 변환한 후
     * 경로 최적화를 적용하여 반환합니다.
     */
    public List<TravelLocation> generateTripPlan(TravelPlanRequest request) {
        String prompt = "다음 정보를 기반으로, 해당 도시의 추천 맛집 및 관광지 정보를 JSON 배열 형식으로 생성해줘. "
                + "각 객체는 반드시 'name', 'latitude', 'longitude', 'availableDate' (yyyy-MM-dd 형식) 키를 포함해야 해. "
                + "추가 텍스트 없이 순수 JSON 배열만 출력해줘. "
                + "입력 정보 - 국가: " + request.getCountry()
                + ", 도시: " + request.getCity()
                + ", 여행 시작일: " + request.getStartDate()
                + ", 여행 종료일: " + request.getEndDate() + ".";

        // GPT API 요청 메시지 구성
        List<AIChatMessage> messages = new ArrayList<>();
        messages.add(new AIChatMessage("system", "You are a travel recommendation assistant."));
        messages.add(new AIChatMessage("user", prompt));

        AIChatRequest openAiRequest = new AIChatRequest();
        openAiRequest.setModel("gpt-3.5-turbo");
        openAiRequest.setMessages(messages);
        openAiRequest.setTemperature(0.3); // 안정적인 JSON 응답을 위해 낮은 temperature 설정

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<AIChatRequest> entity = new HttpEntity<>(openAiRequest, headers);
        String openAiUrl = "https://api.openai.com/v1/chat/completions";

        ResponseEntity<AIChatResponse> responseEntity = restTemplate.postForEntity(openAiUrl, entity,
                AIChatResponse.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            AIChatResponse openAiResponse = responseEntity.getBody();
            if (openAiResponse != null && openAiResponse.getChoices() != null
                    && !openAiResponse.getChoices().isEmpty()) {
                String content = openAiResponse.getChoices().get(0).getMessage().getContent();

                ObjectMapper mapper = new ObjectMapper();
                try {
                    // 응답 JSON 문자열을 RecommendedLocationDTO 리스트로 파싱
                    List<AIRecommendedLocationDTO> dtoList = mapper.readValue(content,
                            new TypeReference<List<AIRecommendedLocationDTO>>() {
                            });

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
                    // 경로 최적화 실행: 날짜별, 좌표 기반
                    return routeOptimizationService.optimizeRoute(locations);
                } catch (Exception e) {
                    throw new RuntimeException("응답 JSON 파싱 실패: " + e.getMessage());
                }
            }
        }
        throw new RuntimeException("GPT API 호출에 실패했습니다.");
    }
}