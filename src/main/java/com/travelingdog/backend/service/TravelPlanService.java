package com.travelingdog.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.travelingdog.backend.dto.TravelLocationDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanUpdateRequest;
import com.travelingdog.backend.exception.ExternalApiException;
import com.travelingdog.backend.exception.ForbiddenResourceAccessException;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelLocationRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;

import jakarta.transaction.Transactional;

@Service
public class TravelPlanService {

    private static final Logger log = LoggerFactory.getLogger(TravelPlanService.class);

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final WebClient webClient;
    private final RouteOptimizationService routeOptimizationService;
    private final GptResponseHandler gptResponseHandler;
    private final TravelPlanRepository travelPlanRepository;
    private final TravelLocationRepository travelLocationRepository;

    public TravelPlanService(RouteOptimizationService routeOptimizationService, GptResponseHandler gptResponseHandler,
            WebClient webClient, TravelPlanRepository travelPlanRepository,
            TravelLocationRepository travelLocationRepository) {
        this.routeOptimizationService = routeOptimizationService;
        this.gptResponseHandler = gptResponseHandler;
        this.webClient = webClient;
        this.travelPlanRepository = travelPlanRepository;
        this.travelLocationRepository = travelLocationRepository;
    }

    /**
     * 프론트엔드에서 전달받은 여행 정보를 바탕으로 GPT API를 호출하고, 추천 장소 정보를 JSON 배열로 받아
     * List<TravelLocation>으로 변환한 후 경로 최적화를 적용하여 반환합니다.
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

    public TravelPlanDTO createTravelPlan(TravelPlanRequest request, User user) {
        // 1. 먼저 TravelPlan 객체 생성 (빈 travelLocations 리스트로)
        TravelPlan travelPlan = TravelPlan.builder()
                .title(request.getTitle())
                .country(request.getCountry()) // country와 city도 설정해야 함
                .city(request.getCity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isShared(request.getIsShared())
                .user(user)
                .travelLocations(new ArrayList<>()) // 빈 리스트로 초기화
                .build();

        // 2. TravelPlan 저장 (ID 생성)
        TravelPlan savedTravelPlan = travelPlanRepository.save(travelPlan);

        // 3. GPT API 호출하여 추천 장소 가져오기
        List<TravelLocation> locations = generateTripPlanLocations(request, savedTravelPlan);

        // 4. 각 TravelLocation에 travelPlan 설정 및 저장
        for (TravelLocation location : locations) {
            location.setTravelPlan(savedTravelPlan);
            travelLocationRepository.save(location);
        }

        // 5. TravelPlan에 locations 설정
        savedTravelPlan.setTravelLocations(locations);

        // 6. 최종 결과 반환
        return TravelPlanDTO.fromEntity(savedTravelPlan);
    }

    // generateTripPlan 메서드를 수정하여 TravelPlan을 매개변수로 받도록 함
    private List<TravelLocation> generateTripPlanLocations(TravelPlanRequest request, TravelPlan travelPlan) {
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
                        location.setCoordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                .createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude())));
                        location.setLocationOrder(order++);
                        location.setDescription("");
                        location.setAvailableDate(LocalDate.parse(dto.getAvailableDate()));
                        location.setTravelPlan(travelPlan);  // 여기서 travelPlan 설정
                        locations.add(location);
                    }

                    // 경로 최적화 실행: 시뮬레이티드 어닐링 알고리즘 사용
                    return routeOptimizationService.optimizeRouteWithSimulatedAnnealing(locations);
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
                        location.setTravelPlan(travelPlan);  // 여기서 travelPlan 설정
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
                .orElseThrow(() -> new ResourceNotFoundException("travelPlan", "여행 계획을 찾을 수 없습니다."));

        if (!travelPlan.getIsShared() && !travelPlan.getUser().getId().equals(user.getId())) {
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
                .orElseThrow(() -> new ResourceNotFoundException("travelPlan", "여행 계획을 찾을 수 없습니다."));

        if (!travelPlan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenResourceAccessException("수정할 수 없는 여행 계획입니다.");
        }

        // travelLocations가 null일 경우 빈 리스트로 처리
        List<TravelLocation> travelLocations = request.getTravelLocations() == null
                ? new ArrayList<>()
                : request.getTravelLocations().stream()
                        .map(TravelLocationDTO::toEntity)
                        .collect(Collectors.toList());

        TravelPlan newTravelPlan = TravelPlan.builder()
                .id(travelPlan.getId())
                .title(request.getTitle())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .travelLocations(travelLocations)
                .build();
        TravelPlan updatedTravelPlan = travelPlanRepository.save(newTravelPlan);

        return TravelPlanDTO.fromEntity(updatedTravelPlan);
    }

    /**
     * 여행 계획 삭제
     */
    public void deleteTravelPlan(Long id, User user)
            throws ForbiddenResourceAccessException, ResourceNotFoundException {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("travelPlan", "여행 계획을 찾을 수 없습니다."));

        if (!travelPlan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenResourceAccessException("삭제할 수 없는 여행 계획입니다.");
        }

        travelPlanRepository.delete(travelPlan);
    }

    /**
     * 여행 장소의 순서를 변경합니다.
     *
     * @param locationId 이동할 장소 ID
     * @param newOrder 새로운 순서 위치
     * @return 업데이트된 여행 계획 DTO
     * @throws ResourceNotFoundException 장소를 찾을 수 없는 경우
     */
    @Transactional
    public TravelPlanDTO changeTravelLocationOrder(Long locationId, int newOrder) throws ResourceNotFoundException {
        // 1. 이동할 장소 찾기
        TravelLocation locationToMove = travelLocationRepository.findById(locationId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("travelLocation", "해당 여행 장소를 찾을 수 없습니다: " + locationId));

        // 2. 해당 장소가 속한 여행 계획 가져오기
        TravelPlan travelPlan = locationToMove.getTravelPlan();

        // 3. 현재 순서 저장
        int currentOrder = locationToMove.getLocationOrder();

        // 4. 같은 순서면 아무것도 하지 않음
        if (currentOrder == newOrder) {
            return TravelPlanDTO.fromEntity(travelPlan);
        }

        // 5. 모든 장소 가져와서 순서대로 정렬
        List<TravelLocation> locations = travelPlan.getTravelLocations()
                .stream()
                .sorted(Comparator.comparing(TravelLocation::getLocationOrder))
                .collect(Collectors.toList());

        // 6. 순서 재배치
        if (currentOrder < newOrder) {
            // 현재 위치보다 뒤로 이동하는 경우 (예: 1→3)
            // 중간에 있는 장소들은 한 칸씩 앞으로 당김 (2→1, 3→2)
            for (TravelLocation loc : locations) {
                int order = loc.getLocationOrder();
                if (order > currentOrder && order <= newOrder) {
                    loc.setLocationOrder(order - 1);
                }
            }
        } else {
            // 현재 위치보다 앞으로 이동하는 경우 (예: 3→1)
            // 중간에 있는 장소들은 한 칸씩 뒤로 밀림 (1→2, 2→3)
            for (TravelLocation loc : locations) {
                int order = loc.getLocationOrder();
                if (order >= newOrder && order < currentOrder) {
                    loc.setLocationOrder(order + 1);
                }
            }
        }

        // 7. 이동할 장소의 순서 변경
        locationToMove.setLocationOrder(newOrder);

        // 8. 변경사항 저장
        travelLocationRepository.saveAll(locations);

        // 9. 업데이트된 여행 계획 DTO 반환
        return TravelPlanDTO.fromEntity(travelPlan);
    }

}
