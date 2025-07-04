package com.travelingdog.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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
import com.travelingdog.backend.dto.travelPlan.TravelPlanSearchRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanSearchResponse;
import com.travelingdog.backend.dto.travelPlan.TravelPlanUpdateRequest;
import com.travelingdog.backend.exception.ExternalApiException;
import com.travelingdog.backend.exception.ForbiddenResourceAccessException;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.model.Itinerary;
import com.travelingdog.backend.model.PlanLike;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.TravelStyle;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.model.Interest;
import com.travelingdog.backend.model.AccommodationType;
import com.travelingdog.backend.model.Transportation;

import com.travelingdog.backend.repository.ItineraryRepository;
import com.travelingdog.backend.repository.PlanLikeRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.status.PlanStatus;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
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
    private final PlanLikeRepository planLikeRepository;

    @Transactional
    public TravelPlanDTO createTravelPlan(TravelPlanRequest request, User user) {
        try {
            // 1. AI 추천 먼저 받아오기
            AIRecommendedTravelPlanDTO aiRecommendedPlan = generateTripPlanWithGemini(request);

            // 2. TravelPlan 객체 생성 (fromDTO는 TravelPlan만 생성)
            TravelPlan travelPlan = TravelPlan.fromDTO(aiRecommendedPlan);
            travelPlan.setUser(user);

            // 3. 연관 엔티티 add 메서드로 추가 (양방향 세팅)
            // TravelStyle
            if (aiRecommendedPlan.getTravelStyle() != null) {
                for (String style : aiRecommendedPlan.getTravelStyle()) {
                    TravelStyle travelStyle = TravelStyle.builder().name(style).build();
                    travelPlan.addTravelStyle(travelStyle);
                }
            }
            // Interest
            if (aiRecommendedPlan.getInterests() != null) {
                for (String interest : aiRecommendedPlan.getInterests()) {
                    Interest interestEntity = Interest.builder().name(interest).build();
                    travelPlan.addInterest(interestEntity);
                }
            }
            // AccommodationType
            if (aiRecommendedPlan.getAccommodation() != null) {
                for (String accommodation : aiRecommendedPlan.getAccommodation()) {
                    AccommodationType accommodationType = AccommodationType.builder().name(accommodation).build();
                    travelPlan.addAccommodationType(accommodationType);
                }
            }
            // Transportation
            if (aiRecommendedPlan.getTransportation() != null) {
                for (String transportation : aiRecommendedPlan.getTransportation()) {
                    Transportation transportationType = Transportation.builder().name(transportation).build();
                    travelPlan.addTransportation(transportationType);
                }
            }

            // Itinerary (fromDto에서 travelPlan 세팅됨)
            List<Itinerary> itineraries = aiRecommendedPlan.getItinerary().stream()
                    .map(dto -> Itinerary.fromDto(dto, travelPlan))
                    .collect(Collectors.toList());
            travelPlan.setItineraries(itineraries);

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
                    request.getCity(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getTravelStyle(),
                    request.getInterests(),
                    request.getAccommodation(),
                    request.getTransportation(),
                    request.getUserSpecifiedAccommodations());

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
                    request.getCity(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getTravelStyle(),
                    request.getInterests(),
                    request.getAccommodation(),
                    request.getTransportation(),
                    request.getUserSpecifiedAccommodations());

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
    public TravelPlanDTO getTravelPlanDetail(Long id, User user) {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        // 공개된 여행 계획은 누구나 조회 가능
        if (travelPlan.getStatus().equals(PlanStatus.PUBLISHED)) {
            return TravelPlanDTO.fromEntity(travelPlan);
        }

        // 비공개 여행 계획은 작성자만 조회 가능
        if (user == null || !travelPlan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenResourceAccessException("접근 금지된 여행 계획입니다.");
        }

        return TravelPlanDTO.fromEntity(travelPlan);
    }

    /**
     * 여행 계획 수정
     */
    public TravelPlanDTO updateTravelPlan(Long id, TravelPlanUpdateRequest request, User user) {
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
    @Transactional
    public void deleteTravelPlan(Long id, User user) {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        if (!travelPlan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenResourceAccessException("삭제할 수 없는 여행 계획입니다.");
        }

        travelPlan.softDelete();
    }

    /**
     * 여행 계획 공개
     */
    @Transactional
    public TravelPlanDTO publishTravelPlan(Long id, User user) {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        if (!travelPlan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenResourceAccessException("공개할 수 없는 여행 계획입니다.");
        }

        travelPlan.setStatus(PlanStatus.PUBLISHED);
        return TravelPlanDTO.fromEntity(travelPlan);
    }

    /**
     * 여행 계획 비공개
     */
    @Transactional
    public TravelPlanDTO unpublishTravelPlan(Long id, User user) {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        if (!travelPlan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenResourceAccessException("비공개할 수 없는 여행 계획입니다.");
        }

        travelPlan.setStatus(PlanStatus.PRIVATE);
        return TravelPlanDTO.fromEntity(travelPlan);
    }

    // 기존의 getPopularTravelPlanList()와 getRecentTravelPlanList() 메서드는
    // 이제 searchTravelPlans() 메서드로 통합되어 제거되었습니다.

    /**
     * 여행 계획 좋아요 토글 (추가/취소)
     * 이미 좋아요를 누른 상태면 취소하고, 아니면 추가합니다.
     */
    @Transactional
    public boolean toggleLike(Long id, User user) {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        // 자신의 여행 계획에는 좋아요를 누를 수 없음
        if (travelPlan.getUser().getId().equals(user.getId())) {
            throw new InvalidRequestException("자신의 여행 계획에는 좋아요를 누를 수 없습니다.");
        }

        // 이미 좋아요를 누른 상태인지 확인
        Optional<PlanLike> existingLike = planLikeRepository.findByUserAndTravelPlan(user, travelPlan);

        if (existingLike.isPresent()) {
            // 좋아요 취소
            travelPlan.removeLike(existingLike.get());
            planLikeRepository.delete(existingLike.get());
            travelPlanRepository.save(travelPlan);
            return false; // 좋아요 취소됨
        } else {
            // 좋아요 추가
            PlanLike planLike = PlanLike.builder()
                    .user(user)
                    .likedAt(LocalDateTime.now())
                    .build();

            travelPlan.addLike(planLike);
            travelPlanRepository.save(travelPlan);
            return true; // 좋아요 추가됨
        }
    }

    /**
     * 여행 계획 좋아요 추가 (기존 메서드 - 토글 방식으로 리다이렉트)
     */
    @Transactional
    public void addLike(Long id, User user) {
        toggleLike(id, user);
    }

    /**
     * 여행 계획 좋아요 취소
     */
    @Transactional
    public void removeLike(Long id, User user) {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        PlanLike planLike = planLikeRepository.findByUserAndTravelPlan(user, travelPlan)
                .orElseThrow(() -> new ResourceNotFoundException("좋아요를 찾을 수 없습니다."));

        travelPlan.removeLike(planLike);
        planLikeRepository.delete(planLike);
        travelPlanRepository.save(travelPlan);
    }

    /**
     * 여행 계획 좋아요 상태 확인
     */
    public boolean isLiked(Long id, User user) {
        TravelPlan travelPlan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("여행 계획을 찾을 수 없습니다."));

        return planLikeRepository.existsByUserAndTravelPlan(user, travelPlan);
    }

    /**
     * 여행 계획 좋아요 조회
     */
    public List<TravelPlanDTO> getLikedTravelPlanList(User user) {
        return planLikeRepository.findByUser(user).stream()
                .map(PlanLike::getTravelPlan)
                .filter(travelPlan -> !travelPlan.getStatus().equals(PlanStatus.DELETED)) // 삭제된 계획 제외
                .map(TravelPlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 여행 계획 검색
     */
    public TravelPlanSearchResponse searchTravelPlans(TravelPlanSearchRequest searchRequest) {
        PageRequest pageRequest = PageRequest.of(searchRequest.getPage(), searchRequest.getSize());
        Page<TravelPlan> travelPlansPage;

        // 키워드가 있는 경우 키워드 검색
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().trim().isEmpty()) {
            travelPlansPage = searchByKeyword(searchRequest.getKeyword(), searchRequest.getSortBy(), pageRequest);
        }
        // 도시나 국가 필터가 있는 경우 위치 필터링
        else if ((searchRequest.getCity() != null && !searchRequest.getCity().trim().isEmpty()) ||
                (searchRequest.getCountry() != null && !searchRequest.getCountry().trim().isEmpty())) {
            travelPlansPage = searchByLocation(searchRequest.getCity(), searchRequest.getCountry(),
                    searchRequest.getSortBy(), pageRequest);
        }
        // 조건이 없는 경우 전체 목록을 정렬 기준에 따라 조회
        else {
            travelPlansPage = getAllTravelPlans(searchRequest.getSortBy(), pageRequest);
        }

        // Page를 TravelPlanSearchResponse로 변환
        return TravelPlanSearchResponse.builder()
                .content(travelPlansPage.getContent().stream()
                        .map(TravelPlanDTO::fromEntity)
                        .collect(Collectors.toList()))
                .page(travelPlansPage.getNumber())
                .size(travelPlansPage.getSize())
                .totalElements(travelPlansPage.getTotalElements())
                .totalPages(travelPlansPage.getTotalPages())
                .first(travelPlansPage.isFirst())
                .last(travelPlansPage.isLast())
                .sortBy(searchRequest.getSortBy())
                .keyword(searchRequest.getKeyword())
                .build();
    }

    /**
     * 키워드로 여행 계획 검색
     */
    private Page<TravelPlan> searchByKeyword(String keyword, String sortBy, PageRequest pageRequest) {
        switch (sortBy.toLowerCase()) {
            case "popular":
                return travelPlanRepository.searchByKeywordOrderByPopular(keyword, PlanStatus.PUBLISHED, pageRequest);
            case "oldest":
                return travelPlanRepository.searchByKeywordOrderByOldest(keyword, PlanStatus.PUBLISHED, pageRequest);
            case "recent":
            default:
                return travelPlanRepository.searchByKeywordOrderByRecent(keyword, PlanStatus.PUBLISHED, pageRequest);
        }
    }

    /**
     * 위치로 여행 계획 필터링
     */
    private Page<TravelPlan> searchByLocation(String city, String country, String sortBy, PageRequest pageRequest) {
        // null이나 빈 문자열을 안전하게 처리
        String safeCity = (city != null && !city.trim().isEmpty()) ? city.trim() : "";
        String safeCountry = (country != null && !country.trim().isEmpty()) ? country.trim() : "";

        switch (sortBy.toLowerCase()) {
            case "popular":
                return travelPlanRepository.findByLocationOrderByPopular(safeCity, safeCountry, PlanStatus.PUBLISHED,
                        pageRequest);
            case "oldest":
                return travelPlanRepository.findByLocationOrderByOldest(safeCity, safeCountry, PlanStatus.PUBLISHED,
                        pageRequest);
            case "recent":
            default:
                return travelPlanRepository.findByLocationOrderByRecent(safeCity, safeCountry, PlanStatus.PUBLISHED,
                        pageRequest);
        }
    }

    /**
     * 전체 여행 계획 조회 (정렬 기준에 따라)
     */
    private Page<TravelPlan> getAllTravelPlans(String sortBy, PageRequest pageRequest) {
        // null 대신 빈 문자열 전달
        switch (sortBy.toLowerCase()) {
            case "popular":
                return travelPlanRepository.findByLocationOrderByPopular("", "", PlanStatus.PUBLISHED, pageRequest);
            case "oldest":
                return travelPlanRepository.findByLocationOrderByOldest("", "", PlanStatus.PUBLISHED, pageRequest);
            case "recent":
            default:
                return travelPlanRepository.findByLocationOrderByRecent("", "", PlanStatus.PUBLISHED, pageRequest);
        }
    }

}
