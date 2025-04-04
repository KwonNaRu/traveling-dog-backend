package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import com.travelingdog.backend.dto.AIRecommendedItineraryDTO;
import com.travelingdog.backend.dto.AIRecommendedItineraryDTO.Location;
import com.travelingdog.backend.dto.AIRecommendedTravelPlanDTO;
import com.travelingdog.backend.dto.AIRecommendedTravelPlanDTO.LocationDTO;
import com.travelingdog.backend.dto.gemini.GeminiCandidate;
import com.travelingdog.backend.dto.gemini.GeminiContent;
import com.travelingdog.backend.dto.gemini.GeminiPart;
import com.travelingdog.backend.dto.gemini.GeminiRequest;
import com.travelingdog.backend.dto.gemini.GeminiResponse;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanUpdateRequest;
import com.travelingdog.backend.model.Itinerary;
import com.travelingdog.backend.model.ItineraryActivity;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.ItineraryRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.status.PlanStatus;

/**
 * 여행 계획 서비스 단위 테스트
 *
 * 이 테스트 클래스는 TripPlanService의 기능을 단위 테스트합니다. 외부 의존성(RestClient,
 * RouteOptimizationService, GptResponseHandler)을 모킹하여 서비스 로직만 독립적으로 테스트합니다.
 *
 * 주요 테스트 대상: 1. 여행 계획 생성 요청 처리 2. GPT 응답 처리 및 위치 데이터 변환 3. 경로 최적화 서비스 연동
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class TravelPlanServiceUnitTest {

        @Mock
        private RestClient restClient;

        @Mock
        private GptResponseHandler gptResponseHandler;

        @Mock
        private TravelPlanRepository travelPlanRepository;

        @Mock
        private ItineraryRepository itineraryRepository;

        @InjectMocks
        private TravelPlanService tripPlanService;

        private TravelPlanRequest request;
        private TravelPlan travelPlan;
        private GeminiResponse mockResponse;
        private List<Itinerary> mockLocations;
        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private LocalDate today;
        private User user;
        private ItineraryActivity activity1;
        private ItineraryActivity activity2;

        /**
         * 각 테스트 실행 전 환경 설정
         *
         * 1. API 키 설정: ReflectionTestUtils를 사용하여 테스트용 API 키 설정 2. 테스트용 여행 계획 요청 데이터
         * 생성 3. 모의 GPT 응답 데이터 설정 4. 모의 위치 데이터 설정 5. RestClient 모킹 설정: OpenAI API
         * 호출을 시뮬레이션
         *
         * 이 설정을 통해 실제 외부 서비스를 호출하지 않고도 TripPlanService의 로직을 테스트할 수 있습니다.
         */
        @BeforeEach
        void setUp() {
                user = User.builder()
                                .id(1L)
                                .email("test@test.com")
                                .password("password123!")
                                .build();

                // API 키 설정
                ReflectionTestUtils.setField(tripPlanService, "openAiApiKey", "test-api-key");
                ReflectionTestUtils.setField(tripPlanService, "geminiApiUrl", "test-api-url");

                // 테스트 요청 데이터 설정
                today = LocalDate.now();
                LocalDate endDate = today.plusDays(3);

                request = new TravelPlanRequest();
                request.setCity("Seoul");
                request.setStartDate(today);
                request.setEndDate(endDate);
                request.setTravelStyle("Adventure");
                request.setBudget("Budget");
                request.setInterests("Interests");
                request.setAccommodation("Accommodation");
                request.setTransportation("Transportation");

                // 모의 응답 데이터 설정
                mockResponse = new GeminiResponse();
                GeminiCandidate candidate = new GeminiCandidate();
                GeminiContent content = new GeminiContent();
                List<GeminiPart> parts = new ArrayList<>();
                String jsonContent = "[{\"name\":\"Gyeongbokgung Palace\",\"type\":\"LOCATION\",\"description\":\"Gyeongbokgung Palace is a large palace complex that was the main royal palace of the Joseon Dynasty.\"}]";
                parts.add(GeminiPart.builder()
                                .text(jsonContent)
                                .build());
                content.setParts(parts);
                candidate.setContent(content);
                List<GeminiCandidate> candidates = new ArrayList<>();
                candidates.add(candidate);
                mockResponse.setCandidates(candidates);

                // 모의 위치 데이터 설정
                mockLocations = new ArrayList<>();

                travelPlan = TravelPlan.builder()
                                .id(1L)
                                .title("Test Travel Plan")
                                .startDate(today)
                                .endDate(endDate)
                                .city("Seoul")
                                .build();

                activity1 = ItineraryActivity.builder()
                                .name("Activity")
                                .description("Activity")
                                .locationName("Test Location Name")
                                .build();

                activity2 = ItineraryActivity.builder()
                                .name("test2")
                                .description("test2")
                                .locationName("Test Location Name")
                                .build();

                Itinerary itinerary = new Itinerary();
                itinerary.setLocation("Gyeongbokgung Palace");
                itinerary.setActivities(Arrays.asList(activity1, activity2));
                itinerary.setDate(0);
                itinerary.setTravelPlan(travelPlan);
                // TravelPlan은 실제 저장 시 설정되므로 테스트에서는 필요 없음
                mockLocations.add(itinerary);
        }

        /**
         * 여행 계획 생성 후 저장 기능 테스트
         *
         * 이 테스트는 TravelPlanService가 여행 계획을 생성하고 저장하는 과정을 검증합니다.
         *
         * 테스트 과정: 1. 여행 계획 생성 요청 2. 여행 계획 저장 3. 결과 검증: 여행 계획 저장 결과
         *
         */
        @Test
        @DisplayName("여행 계획 생성 후 저장 기능 테스트")
        void testCreateTravelPlan() {

                // RestClient 모킹 설정 - 이 테스트에서 필요한 경우에만 설정
                RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
                RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
                ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);

                when(restClient.post()).thenReturn(requestBodyUriSpec);
                when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
                when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
                when(requestBodySpec.body(any(GeminiRequest.class))).thenReturn(requestBodySpec);
                when(requestBodySpec.retrieve()).thenReturn(responseSpec);
                when(responseSpec.body(GeminiResponse.class)).thenReturn(mockResponse);

                // Given
                // TravelPlan savedTravelPlan = TravelPlan.builder()
                // .id(1L)
                // .title(request.getTitle())
                // .startDate(request.getStartDate())
                // .endDate(request.getEndDate())
                // .country(request.getCountry())
                // .city(request.getCity())
                // .build();

                AIRecommendedTravelPlanDTO aiRecommendedTravelPlanDTO = createMockTravelPlanDTO(
                                "Seoul");

                TravelPlan savedTravelPlan = TravelPlan.fromDTO(aiRecommendedTravelPlanDTO);

                // GptResponseHandler 모킹
                when(gptResponseHandler.parseGptResponse(any(String.class)))
                                .thenReturn(aiRecommendedTravelPlanDTO);
                when(gptResponseHandler.createEnhancedPrompt(any(), any(), any(), any(), any(), any(), any(),
                                any()))
                                .thenReturn("테스트 프롬프트");
                // TravelPlanRepository 모킹
                when(travelPlanRepository.save(any(TravelPlan.class))).thenReturn(savedTravelPlan);

                // When
                TravelPlanDTO result = tripPlanService.createTravelPlan(request, user);

                // Then
                assertNotNull(result);
                assertEquals(request.getStartDate(), result.getStartDate());
                assertEquals(request.getEndDate(), result.getEndDate());
                assertEquals(request.getCity(), result.getCity());

                // 저장소 호출 검증
                verify(travelPlanRepository).save(any(TravelPlan.class));
        }

        /**
         * 여행 계획 수정 기능 테스트
         *
         * 이 테스트는 TravelPlanService가 여행 계획을 수정하는 과정을 검증합니다. 도시와 국가는 수정 불가능하며, 여행 계획의
         * 제목, 시작일, 종료일만 수정 가능합니다.
         *
         * 테스트 과정: 1. 여행 계획 수정 요청 2. 여행 계획 수정 3. 결과 검증: 여행 계획 수정 결과
         */
        @Test
        @DisplayName("여행 계획 수정 기능 테스트")
        void testUpdateTravelPlan() {
                // Given
                Long travelPlanId = 1L;

                TravelPlan existingPlan = TravelPlan.builder()
                                .id(travelPlanId)
                                .title("Original Title")
                                .user(user)
                                .startDate(today)
                                .endDate(today.plusDays(3))
                                .status(PlanStatus.PUBLISHED)
                                .city("Seoul")
                                .build();

                TravelPlanUpdateRequest updateRequest = new TravelPlanUpdateRequest();
                updateRequest.setTitle("Updated Title");
                updateRequest.setStartDate(today.plusDays(1));
                updateRequest.setEndDate(today.plusDays(4));

                TravelPlan updatedPlan = TravelPlan.builder()
                                .id(travelPlanId)
                                .title(updateRequest.getTitle())
                                .user(existingPlan.getUser())
                                .startDate(updateRequest.getStartDate())
                                .endDate(updateRequest.getEndDate())
                                .status(PlanStatus.PUBLISHED)
                                .city(existingPlan.getCity())
                                .build();

                // Repository 모킹
                when(travelPlanRepository.findById(travelPlanId)).thenReturn(Optional.of(existingPlan));
                when(travelPlanRepository.save(any(TravelPlan.class))).thenReturn(updatedPlan);

                // When
                TravelPlanDTO result = tripPlanService.updateTravelPlan(travelPlanId, updateRequest, user);

                // Then
                assertNotNull(result);
                assertEquals(updateRequest.getTitle(), result.getTitle());
                assertEquals(updateRequest.getStartDate(), result.getStartDate());
                assertEquals(updateRequest.getEndDate(), result.getEndDate());
                assertEquals(existingPlan.getCity(), result.getCity()); // 도시는 변경되지 않아야 함

                // 저장소 호출 검증
                verify(travelPlanRepository).findById(travelPlanId);
                verify(travelPlanRepository).save(any(TravelPlan.class));
        }

        /**
         * 여행 계획 삭제 기능 테스트
         *
         * 이 테스트는 TravelPlanService가 여행 계획을 삭제하는 과정을 검증합니다.
         *
         * 테스트 과정: 1. 여행 계획 삭제 요청 2. 여행 계획 삭제 3. 결과 검증: 여행 계획 삭제 결과
         */
        @Test
        @DisplayName("여행 계획 삭제 기능 테스트")
        void testDeleteTravelPlan() {
                // Given
                Long travelPlanId = 1L;

                TravelPlan existingPlan = TravelPlan.builder()
                                .id(travelPlanId)
                                .title("Travel Plan to Delete")
                                .city("Seoul")
                                .user(user)
                                .status(PlanStatus.PUBLISHED)
                                .build();

                // Repository 모킹
                when(travelPlanRepository.findById(travelPlanId))
                                .thenReturn(Optional.of(existingPlan)) // 첫 번째 호출에서는 엔티티 반환
                                .thenReturn(Optional.empty()); // 삭제 후 두 번째 호출에서는 빈 Optional 반환

                // When
                tripPlanService.deleteTravelPlan(travelPlanId, user);

                // Then
                // 삭제 메서드 호출 검증
                verify(travelPlanRepository).delete(existingPlan);

                // 삭제 후 엔티티가 존재하지 않는지 확인
                Optional<TravelPlan> deletedPlan = travelPlanRepository.findById(travelPlanId);
                assertTrue(deletedPlan.isEmpty(), "삭제된 여행 계획은 조회되지 않아야 합니다");
        }

        /**
         * 테스트용 위치 DTO 생성 헬퍼 메소드
         *
         * 이 메소드는 테스트에 사용할 AIRecommendedLocationDTO 객체를 생성합니다. GPT 응답에서 파싱된 위치 정보를
         * 시뮬레이션하는 데 사용됩니다.
         *
         * @param name 장소 이름
         * @return 모의 AIRecommendedLocationDTO 객체
         */
        private AIRecommendedTravelPlanDTO createMockTravelPlanDTO(
                        String name) {

                Location location = new Location();
                location.setName(name);
                location.setDescription("Description");
                location.setLocationName("Test Location Name");

                AIRecommendedItineraryDTO itinerary = new AIRecommendedItineraryDTO();
                itinerary.setLocation(name);
                itinerary.setDate(0);
                itinerary.setActivities(Arrays.asList(location, location));

                LocationDTO locationDTO = new LocationDTO();
                locationDTO.setName(name);
                locationDTO.setDescription("Description");

                AIRecommendedTravelPlanDTO dto = new AIRecommendedTravelPlanDTO();
                dto.setDestination(name);
                dto.setItinerary(Arrays.asList(itinerary, itinerary));
                dto.setRestaurantRecommendations(Arrays.asList(locationDTO, locationDTO));
                dto.setAccommodationRecommendations(Arrays.asList(locationDTO, locationDTO));
                dto.setTransportationTips("Transportation Tips");
                dto.setTripName("Test Travel Plan");
                dto.setStartDate(today.toString());
                dto.setEndDate(today.plusDays(3).toString());
                dto.setSeason("Spring");
                dto.setTravelStyle(Arrays.asList("Adventure", "Relaxation"));
                dto.setBudget("Budget");
                dto.setInterests(Arrays.asList("Interests"));
                dto.setAccommodation(Arrays.asList("Accommodation"));
                dto.setTransportation(Arrays.asList("Transportation"));

                return dto;
        }
}
