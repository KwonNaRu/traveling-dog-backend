package com.travelingdog.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.travelingdog.backend.dto.AIRecommendedItineraryDTO;
import com.travelingdog.backend.dto.AIRecommendedTravelPlanDTO;
import com.travelingdog.backend.dto.gemini.GeminiContent;
import com.travelingdog.backend.dto.gemini.GeminiGenerationConfig;
import com.travelingdog.backend.dto.gemini.GeminiPart;
import com.travelingdog.backend.dto.gemini.GeminiRequest;
import com.travelingdog.backend.dto.gemini.GeminiResponse;
import com.travelingdog.backend.dto.gpt.AIChatMessage;
import com.travelingdog.backend.dto.gpt.AIChatRequest;
import com.travelingdog.backend.dto.gpt.AIChatResponse;
import com.travelingdog.backend.dto.travelPlan.ItineraryDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanUpdateRequest;
import com.travelingdog.backend.exception.ExternalApiException;
import com.travelingdog.backend.exception.ForbiddenResourceAccessException;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.model.Itinerary;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.ItineraryRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.status.PlanStatus;

import jakarta.transaction.Transactional;

@Service
public class TravelPlanService {

    private static final Logger log = LoggerFactory.getLogger(TravelPlanService.class);

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final RestClient restClient;
    private final GptResponseHandler gptResponseHandler;
    private final TravelPlanRepository travelPlanRepository;
    private final ItineraryRepository itineraryRepository;

    public TravelPlanService(GptResponseHandler gptResponseHandler,
            RestClient restClient, TravelPlanRepository travelPlanRepository,
            ItineraryRepository itineraryRepository) {
        this.gptResponseHandler = gptResponseHandler;
        this.restClient = restClient;
        this.travelPlanRepository = travelPlanRepository;
        this.itineraryRepository = itineraryRepository;
    }

    @Transactional
    public TravelPlanDTO createTravelPlan(TravelPlanRequest request, User user) {
        try {
            // 1. AI 추천 먼저 받아오기
            AIRecommendedTravelPlanDTO aiRecommendedPlan = generateTripPlan(request);

            // 2. TravelPlan 객체 생성
            TravelPlan travelPlan = TravelPlan.builder()
                    .title(request.getTitle())
                    .country(request.getCountry())
                    .city(request.getCity())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .user(user)
                    .build();

            // 3. Itinerary 객체들 생성 및 연결
            // List<Itinerary> itineraries = new ArrayList<>();
            for (AIRecommendedItineraryDTO dto : aiRecommendedPlan.getItinerary()) {
                Itinerary itinerary = Itinerary.fromDto(dto, travelPlan);
                travelPlan.addItinerary(itinerary);
                // itineraries.add(itinerary);
            }
            // travelPlan.setItineraries(itineraries);

            // 4. 한 번에 저장
            travelPlanRepository.save(travelPlan);

            // 5. DTO 반환
            return TravelPlanDTO.fromEntity(travelPlan);
        } catch (ExternalApiException e) {
            log.error("AI 추천 실패: {}", e.getMessage());
            throw new InvalidRequestException("AI 추천을 받지 못했습니다: " + e.getMessage());
        }
    }

    private AIRecommendedTravelPlanDTO generateTripPlan(TravelPlanRequest request) {
        try {
            // 강화된 프롬프트 생성
            String prompt = gptResponseHandler.createEnhancedPrompt(
                    request.getCountry(),
                    request.getCity(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getSeason(),
                    request.getTravelStyle(),
                    request.getBudget(),
                    request.getInterests(),
                    request.getAccommodation(),
                    request.getTransportation());

            // GPT API 요청 메시지 구성
            List<AIChatMessage> messages = new ArrayList<>();
            messages.add(new AIChatMessage("system", "You are a travel recommendation assistant."));
            messages.add(new AIChatMessage("user", prompt));

            AIChatRequest openAiRequest = new AIChatRequest();
            openAiRequest.setModel("gpt-3.5-turbo");
            openAiRequest.setMessages(messages);
            openAiRequest.setTemperature(0.3); // 안정적인 JSON 응답을 위해 낮은 temperature 설정

            String openAiUrl = "https://api.openai.com/v1/chat/completions";

            AIChatResponse openAiResponse = restClient.post()
                    .uri(openAiUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                    .body(openAiRequest)
                    .retrieve()
                    .body(AIChatResponse.class);

            if (openAiResponse != null && openAiResponse.getChoices() != null
                    && !openAiResponse.getChoices().isEmpty()) {
                String content = openAiResponse.getChoices().get(0).getMessage().getContent();

                try {
                    // 응답 파싱 및 검증
                    return gptResponseHandler.parseGptResponse(content);
                } catch (Exception e) {
                    log.error("GPT 응답 처리 중 오류 발생: {}", e.getMessage());
                    // 대체 응답 사용
                    return gptResponseHandler.getFallbackResponse(
                            request.getCountry(),
                            request.getCity(),
                            request.getStartDate(),
                            request.getEndDate());
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

    /**
     * Gemini API를 사용하여 여행 계획을 생성합니다.
     */
    private AIRecommendedTravelPlanDTO generateTripPlanWithGemini(TravelPlanRequest request) {
        try {
            // 강화된 프롬프트 생성 (기존 프롬프트 재사용)
            String prompt = gptResponseHandler.createEnhancedPrompt(
                    request.getCountry(),
                    request.getCity(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getSeason(),
                    request.getTravelStyle(),
                    request.getBudget(),
                    request.getInterests(),
                    request.getAccommodation(),
                    request.getTransportation());

            // Gemini API 요청 구성
            GeminiRequest geminiRequest = GeminiRequest.builder()
                    .contents(List.of(
                            GeminiContent.builder()
                                    .parts(List.of(
                                            GeminiPart.builder()
                                                    .text(prompt)
                                                    .build()))
                                    .role("user")
                                    .build()))
                    .generationConfig(GeminiGenerationConfig.builder()
                            .temperature(0.3f)
                            .topK(1)
                            .topP(1)
                            .maxOutputTokens(2048)
                            .build())
                    .build();

            // Gemini API 호출
            GeminiResponse geminiResponse = restClient.post()
                    .uri(geminiApiUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("x-goog-api-key", geminiApiKey)
                    .body(geminiRequest)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (geminiResponse != null &&
                    geminiResponse.getCandidates() != null &&
                    !geminiResponse.getCandidates().isEmpty()) {

                String content = geminiResponse.getCandidates().get(0)
                        .getContent().getParts().get(0).getText();

                try {
                    // 응답 파싱 및 검증 (기존 파서 재사용)
                    return gptResponseHandler.parseGptResponse(content);
                } catch (Exception e) {
                    log.error("Gemini 응답 처리 중 오류 발생: {}", e.getMessage());
                    // 대체 응답 사용
                    return gptResponseHandler.getFallbackResponse(
                            request.getCountry(),
                            request.getCity(),
                            request.getStartDate(),
                            request.getEndDate());
                }
            }
            throw new ExternalApiException("Gemini API 호출에 실패했습니다.");
        } catch (ExternalApiException e) {
            log.error("외부 API 호출 중 오류 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("여행 계획 생성 중 오류 발생: {}", e.getMessage());
            throw new InvalidRequestException("여행 계획 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 다른 유저의 리스트는 조회할 수 없음.
     */
    public List<TravelPlanDTO> getTravelPlanList(User user) {
        List<TravelPlan> travelPlans = travelPlanRepository.findAllByUser(user);
        return travelPlans.stream()
                .map(TravelPlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 여행 계획 상세 조회
     */
    public TravelPlanDTO getTravelPlanDetail(Long id, User user)
            throws ForbiddenResourceAccessException, ResourceNotFoundException {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        if (!travelPlan.getStatus().equals(PlanStatus.PUBLISHED)
                && !travelPlan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenResourceAccessException("접근 금지된 여행 계획입니다.");
        }

        return TravelPlanDTO.fromEntity(travelPlan);
    }

    /**
     * 여행 계획 수정
     */
    public TravelPlanDTO updateTravelPlan(Long id, TravelPlanUpdateRequest request, User user)
            throws ForbiddenResourceAccessException, ResourceNotFoundException {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        if (!travelPlan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenResourceAccessException("수정할 수 없는 여행 계획입니다.");
        }

        // 기존 여행 계획의 속성만 업데이트
        travelPlan.setTitle(request.getTitle());
        travelPlan.setStartDate(request.getStartDate());
        travelPlan.setEndDate(request.getEndDate());

        // itineraries가 null이 아닌 경우에만 업데이트
        if (request.getItineraries() != null) {
            // 기존 itineraries 삭제 (orphanRemoval=true로 자동 삭제됨)
            travelPlan.getItineraries().clear();

            // 새 itineraries 추가
            for (ItineraryDTO itineraryDTO : request.getItineraries()) {
                if (itineraryDTO != null) {
                    Itinerary itinerary = ItineraryDTO.toEntity(itineraryDTO);
                    if (itinerary != null) {
                        travelPlan.addItinerary(itinerary);
                    }
                }
            }
        }

        // 변경사항 저장
        TravelPlan updatedTravelPlan = travelPlanRepository.save(travelPlan);

        return TravelPlanDTO.fromEntity(updatedTravelPlan);
    }

    /**
     * 여행 계획 삭제
     */
    public void deleteTravelPlan(Long id, User user)
            throws ForbiddenResourceAccessException, ResourceNotFoundException {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        if (!travelPlan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenResourceAccessException("삭제할 수 없는 여행 계획입니다.");
        }

        travelPlanRepository.delete(travelPlan);
    }

    /**
     * 여행 장소의 순서를 변경합니다.
     *
     * @param locationId 이동할 장소 ID
     * @param newOrder   새로운 순서 위치
     * @return 업데이트된 여행 계획 DTO
     * @throws ResourceNotFoundException 장소를 찾을 수 없는 경우
     */
    @Transactional
    public TravelPlanDTO changeTravelLocationOrder(Long locationId, int newOrder) throws ResourceNotFoundException {
        // 1. 이동할 장소 찾기
        Itinerary locationToMove = itineraryRepository.findById(locationId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("해당 여행 장소를 찾을 수 없습니다: " + locationId));

        // 2. 해당 장소가 속한 여행 계획 가져오기
        TravelPlan travelPlan = locationToMove.getTravelPlan();

        // 3. 현재 순서 저장
        int currentOrder = locationToMove.getDate();

        // 4. 같은 순서면 아무것도 하지 않음
        if (currentOrder == newOrder) {
            return TravelPlanDTO.fromEntity(travelPlan);
        }

        // 5. 모든 장소 가져와서 순서대로 정렬
        List<Itinerary> itineraries = travelPlan.getItineraries()
                .stream()
                .sorted(Comparator.comparing(Itinerary::getDate))
                .collect(Collectors.toList());

        // 6. 순서 재배치
        if (currentOrder < newOrder) {
            // 현재 위치보다 뒤로 이동하는 경우 (예: 1→3)
            // 중간에 있는 장소들은 한 칸씩 앞으로 당김 (2→1, 3→2)
            for (Itinerary loc : itineraries) {
                int order = loc.getDate();
                if (order > currentOrder && order <= newOrder) {
                    loc.setDate(order - 1);
                }
            }
        } else {
            // 현재 위치보다 앞으로 이동하는 경우 (예: 3→1)
            // 중간에 있는 장소들은 한 칸씩 뒤로 밀림 (1→2, 2→3)
            for (Itinerary loc : itineraries) {
                int order = loc.getDate();
                if (order >= newOrder && order < currentOrder) {
                    loc.setDate(order + 1);
                }
            }
        }

        // 7. 이동할 장소의 순서 변경
        locationToMove.setDate(newOrder);

        // 8. 변경사항 저장
        itineraryRepository.saveAll(itineraries);

        // 9. 업데이트된 여행 계획 DTO 반환
        return TravelPlanDTO.fromEntity(travelPlan);
    }

}
