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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import com.travelingdog.backend.dto.AIChatMessage;
import com.travelingdog.backend.dto.AIChatResponse;
import com.travelingdog.backend.dto.AIRecommendedLocationDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.repository.TravelPlanRepository;

import reactor.core.publisher.Mono;

/**
 * 여행 계획 서비스 단위 테스트
 * 
 * 이 테스트 클래스는 TripPlanService의 기능을 단위 테스트합니다.
 * 외부 의존성(WebClient, RouteOptimizationService, GptResponseHandler)을
 * 모킹하여 서비스 로직만 독립적으로 테스트합니다.
 * 
 * 주요 테스트 대상:
 * 1. 여행 계획 생성 요청 처리
 * 2. GPT 응답 처리 및 위치 데이터 변환
 * 3. 경로 최적화 서비스 연동
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class TravelPlanServiceUnitTest {

        @Mock
        private WebClient webClient;

        @Mock
        private RouteOptimizationService routeOptimizationService;

        @Mock
        private GptResponseHandler gptResponseHandler;

        @Mock
        private TravelPlanRepository travelPlanRepository;

        @InjectMocks
        private TravelPlanService tripPlanService;

        private TravelPlanRequest request;
        private AIChatResponse mockResponse;
        private List<TravelLocation> mockLocations;
        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private LocalDate today;

        /**
         * 각 테스트 실행 전 환경 설정
         * 
         * 1. API 키 설정: ReflectionTestUtils를 사용하여 테스트용 API 키 설정
         * 2. 테스트용 여행 계획 요청 데이터 생성
         * 3. 모의 GPT 응답 데이터 설정
         * 4. 모의 위치 데이터 설정
         * 5. WebClient 모킹 설정: OpenAI API 호출을 시뮬레이션
         * 
         * 이 설정을 통해 실제 외부 서비스를 호출하지 않고도
         * TripPlanService의 로직을 테스트할 수 있습니다.
         */
        @BeforeEach
        void setUp() {
                // API 키 설정
                ReflectionTestUtils.setField(tripPlanService, "openAiApiKey", "test-api-key");

                // 테스트 요청 데이터 설정
                today = LocalDate.now();
                LocalDate endDate = today.plusDays(3);

                request = new TravelPlanRequest();
                request.setCountry("South Korea");
                request.setCity("Seoul");
                request.setStartDate(today);
                request.setEndDate(endDate);

                // 모의 응답 데이터 설정
                mockResponse = new AIChatResponse();
                AIChatResponse.Choice choice = new AIChatResponse.Choice();
                AIChatMessage message = new AIChatMessage();
                String jsonContent = "[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770,\"availableDate\":\""
                                + today.format(formatter) + "\"}]";
                message.setContent(jsonContent);
                choice.setMessage(message);
                List<AIChatResponse.Choice> choices = new ArrayList<>();
                choices.add(choice);
                mockResponse.setChoices(choices);

                // 모의 위치 데이터 설정
                mockLocations = new ArrayList<>();

                TravelLocation location = new TravelLocation();
                location.setPlaceName("Gyeongbokgung Palace");
                location.setCoordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                .createPoint(new Coordinate(126.9770, 37.5796)));
                location.setLocationOrder(0);
                location.setDescription("");
                location.setAvailableDate(today);
                // TravelPlan은 실제 저장 시 설정되므로 테스트에서는 필요 없음
                mockLocations.add(location);

                // WebClient 모킹 설정
                RequestBodyUriSpec requestBodyUriSpec = Mockito
                                .mock(RequestBodyUriSpec.class);
                RequestBodySpec requestBodySpec = Mockito
                                .mock(RequestBodySpec.class);
                RequestHeadersSpec requestHeadersSpec = Mockito
                                .mock(RequestHeadersSpec.class);
                ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);

                when(webClient.post()).thenReturn(requestBodyUriSpec);
                when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
                when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
                when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
                when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
                when(responseSpec.bodyToMono(AIChatResponse.class)).thenReturn(Mono.just(mockResponse));
        }

        /**
         * 여행 계획 생성 기능 테스트
         * 
         * 이 테스트는 TripPlanService의 generateTripPlan 메소드가
         * 여행 계획 요청을 처리하여 여행 위치 목록을 생성하는 과정을 검증합니다.
         * 
         * 테스트 과정:
         * 1. GptResponseHandler 모킹: GPT 응답 파싱 및 프롬프트 생성 기능 모킹
         * 2. RouteOptimizationService 모킹: 경로 최적화 기능 모킹
         * 3. 여행 계획 생성 요청
         * 4. 결과 검증: 위치 수, 위치 이름, 날짜 정보
         * 
         * 이 테스트는 여행 계획 생성의 전체 흐름이 올바르게 작동하는지 검증합니다.
         */
        @Test
        @DisplayName("여행 계획 생성 기능 테스트")
        void testGenerateTripPlan() {
                // GptResponseHandler 모킹 추가
                when(gptResponseHandler.parseGptResponse(any(String.class)))
                                .thenReturn(List
                                                .of(createMockLocationDTO("Gyeongbokgung Palace", 37.5796, 126.9770,
                                                                today.format(formatter))));

                when(gptResponseHandler.createEnhancedPrompt(any(), any(), any(), any()))
                                .thenReturn("테스트 프롬프트");

                // 시뮬레이티드 어닐링 알고리즘 사용으로 변경
                when(routeOptimizationService.optimizeRouteWithSimulatedAnnealing(any())).thenReturn(mockLocations);

                // When
                List<TravelLocation> result = tripPlanService.generateTripPlan(request);

                // Then
                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals("Gyeongbokgung Palace", result.get(0).getPlaceName());
                assertNotNull(result.get(0).getAvailableDate());
                assertEquals(today, result.get(0).getAvailableDate());
        }

        /**
         * 여행 계획 생성 후 저장 기능 테스트
         * 
         * 이 테스트는 TravelPlanService가 여행 계획을 생성하고 저장하는 과정을 검증합니다.
         * 
         * 테스트 과정:
         * 1. 여행 계획 생성 요청
         * 2. 여행 계획 저장
         * 3. 결과 검증: 여행 계획 저장 결과
         * 
         */
        @Test
        @DisplayName("여행 계획 생성 후 저장 기능 테스트")
        void testCreateTravelPlanDTO() {
                // Given
                TravelPlan savedTravelPlan = TravelPlan.builder()
                                .id(1L)
                                .title(request.getTitle())
                                .startDate(request.getStartDate())
                                .endDate(request.getEndDate())
                                .country(request.getCountry())
                                .city(request.getCity())
                                .build();

                // GptResponseHandler 모킹
                when(gptResponseHandler.parseGptResponse(any(String.class)))
                                .thenReturn(List.of(createMockLocationDTO("Gyeongbokgung Palace", 37.5796, 126.9770,
                                                today.format(formatter))));
                when(gptResponseHandler.createEnhancedPrompt(any(), any(), any(), any()))
                                .thenReturn("테스트 프롬프트");

                // RouteOptimizationService 모킹
                when(routeOptimizationService.optimizeRouteWithSimulatedAnnealing(any())).thenReturn(mockLocations);

                // TravelPlanRepository 모킹
                when(travelPlanRepository.save(any(TravelPlan.class))).thenReturn(savedTravelPlan);

                // When
                TravelPlanDTO result = tripPlanService.createTravelPlan(request);

                // Then
                assertNotNull(result);
                assertEquals(request.getTitle(), result.getTitle());
                assertEquals(request.getStartDate(), result.getStartDate());
                assertEquals(request.getEndDate(), result.getEndDate());
                assertEquals(request.getCountry(), result.getCountry());
                assertEquals(request.getCity(), result.getCity());

                // 저장소 호출 검증
                verify(travelPlanRepository).save(any(TravelPlan.class));
        }

        /**
         * 여행 계획 수정 기능 테스트
         * 
         * 이 테스트는 TravelPlanService가 여행 계획을 수정하는 과정을 검증합니다.
         * 도시와 국가는 수정 불가능하며, 여행 계획의 제목, 시작일, 종료일만 수정 가능합니다.
         * 
         * 테스트 과정:
         * 1. 여행 계획 수정 요청
         * 2. 여행 계획 수정
         * 3. 결과 검증: 여행 계획 수정 결과
         */
        @Test
        @DisplayName("여행 계획 수정 기능 테스트")
        void testUpdateTravelPlan() {
                // Given
                Long travelPlanId = 1L;

                TravelPlan existingPlan = TravelPlan.builder()
                                .id(travelPlanId)
                                .title("Original Title")
                                .startDate(today)
                                .endDate(today.plusDays(3))
                                .country("South Korea")
                                .city("Seoul")
                                .build();

                TravelPlanRequest updateRequest = new TravelPlanRequest();
                updateRequest.setTitle("Updated Title");
                updateRequest.setStartDate(today.plusDays(1));
                updateRequest.setEndDate(today.plusDays(4));

                TravelPlan updatedPlan = TravelPlan.builder()
                                .id(travelPlanId)
                                .title(updateRequest.getTitle())
                                .startDate(updateRequest.getStartDate())
                                .endDate(updateRequest.getEndDate())
                                .country(existingPlan.getCountry())
                                .city(existingPlan.getCity())
                                .build();

                // Repository 모킹
                when(travelPlanRepository.findById(travelPlanId)).thenReturn(Optional.of(existingPlan));
                when(travelPlanRepository.save(any(TravelPlan.class))).thenReturn(updatedPlan);

                // When
                TravelPlanDTO result = tripPlanService.updateTravelPlan(travelPlanId, updateRequest);

                // Then
                assertNotNull(result);
                assertEquals(updateRequest.getTitle(), result.getTitle());
                assertEquals(updateRequest.getStartDate(), result.getStartDate());
                assertEquals(updateRequest.getEndDate(), result.getEndDate());
                assertEquals(existingPlan.getCountry(), result.getCountry()); // 국가는 변경되지 않아야 함
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
         * 테스트 과정:
         * 1. 여행 계획 삭제 요청
         * 2. 여행 계획 삭제
         * 3. 결과 검증: 여행 계획 삭제 결과
         */
        @Test
        @DisplayName("여행 계획 삭제 기능 테스트")
        void testDeleteTravelPlan() {
                // Given
                Long travelPlanId = 1L;

                TravelPlan existingPlan = TravelPlan.builder()
                                .id(travelPlanId)
                                .title("Travel Plan to Delete")
                                .build();

                // Repository 모킹
                when(travelPlanRepository.findById(travelPlanId))
                                .thenReturn(Optional.of(existingPlan)) // 첫 번째 호출에서는 엔티티 반환
                                .thenReturn(Optional.empty()); // 삭제 후 두 번째 호출에서는 빈 Optional 반환

                // When
                tripPlanService.deleteTravelPlan(travelPlanId);

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
         * 이 메소드는 테스트에 사용할 AIRecommendedLocationDTO 객체를 생성합니다.
         * GPT 응답에서 파싱된 위치 정보를 시뮬레이션하는 데 사용됩니다.
         * 
         * @param name          장소 이름
         * @param latitude      위도
         * @param longitude     경도
         * @param availableDate 방문 가능 날짜 (문자열 형식)
         * @return 모의 AIRecommendedLocationDTO 객체
         */
        private AIRecommendedLocationDTO createMockLocationDTO(
                        String name, double latitude, double longitude, String availableDate) {
                AIRecommendedLocationDTO dto = new AIRecommendedLocationDTO();
                dto.setName(name);
                dto.setLatitude(latitude);
                dto.setLongitude(longitude);
                dto.setAvailableDate(availableDate);
                return dto;
        }
}