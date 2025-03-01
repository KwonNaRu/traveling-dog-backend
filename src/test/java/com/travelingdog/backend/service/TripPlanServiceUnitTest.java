package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.travelingdog.backend.dto.AIChatMessage;
import com.travelingdog.backend.dto.AIChatResponse;
import com.travelingdog.backend.dto.TravelPlanRequest;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.model.TravelPlan;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class TripPlanServiceUnitTest {

        @Mock
        private WebClient webClient;

        @Mock
        private RouteOptimizationService routeOptimizationService;

        @Mock
        private GptResponseHandler gptResponseHandler;

        @InjectMocks
        private TripPlanService tripPlanService;

        private TravelPlanRequest request;
        private AIChatResponse mockResponse;
        private List<TravelLocation> mockLocations;
        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private LocalDate today;

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
                request.setStartDate(today.format(formatter));
                request.setEndDate(endDate.format(formatter));

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
                TravelPlan dummyPlan = TravelPlan.builder()
                                .id(1L)
                                .title("Test Plan")
                                .startDate(today)
                                .endDate(endDate)
                                .build();

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
                WebClient.RequestBodyUriSpec requestBodyUriSpec = org.mockito.Mockito
                                .mock(WebClient.RequestBodyUriSpec.class);
                WebClient.RequestBodySpec requestBodySpec = org.mockito.Mockito.mock(WebClient.RequestBodySpec.class);
                WebClient.RequestHeadersSpec requestHeadersSpec = org.mockito.Mockito
                                .mock(WebClient.RequestHeadersSpec.class);
                WebClient.ResponseSpec responseSpec = org.mockito.Mockito.mock(WebClient.ResponseSpec.class);

                when(webClient.post()).thenReturn(requestBodyUriSpec);
                when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
                when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
                when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
                when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
                when(responseSpec.bodyToMono(AIChatResponse.class)).thenReturn(Mono.just(mockResponse));
        }

        @Test
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

        // 테스트용 DTO 생성 헬퍼 메서드
        private com.travelingdog.backend.dto.AIRecommendedLocationDTO createMockLocationDTO(
                        String name, double latitude, double longitude, String availableDate) {
                com.travelingdog.backend.dto.AIRecommendedLocationDTO dto = new com.travelingdog.backend.dto.AIRecommendedLocationDTO();
                dto.setName(name);
                dto.setLatitude(latitude);
                dto.setLongitude(longitude);
                dto.setAvailableDate(availableDate);
                return dto;
        }
}