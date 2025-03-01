package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.AIChatMessage;
import com.travelingdog.backend.dto.AIChatResponse;
import com.travelingdog.backend.dto.AIRecommendedLocationDTO;
import com.travelingdog.backend.dto.TravelPlanRequest;
import com.travelingdog.backend.model.TravelLocation;

import reactor.core.publisher.Mono;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
public class GPTResponseIntegrationTest {

        @Autowired
        private TripPlanService tripPlanService;

        @Autowired
        private GptResponseHandler gptResponseHandler;

        @MockBean
        private WebClient webClient;

        @MockBean
        private RouteOptimizationService routeOptimizationService;

        private TravelPlanRequest request;
        private AIChatResponse mockResponse;
        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private ObjectMapper objectMapper = new ObjectMapper();
        private LocalDate today;

        @BeforeEach
        void setUp() {
                // 테스트 요청 데이터 설정
                today = LocalDate.now();
                LocalDate endDate = today.plusDays(3);

                request = new TravelPlanRequest();
                request.setCountry("South Korea");
                request.setCity("Seoul");
                request.setStartDate(today.format(formatter));
                request.setEndDate(endDate.format(formatter));

                // 모의 GPT 응답 데이터 설정
                mockResponse = new AIChatResponse();
                AIChatResponse.Choice choice = new AIChatResponse.Choice();
                AIChatMessage message = new AIChatMessage();

                // 여러 장소를 포함하는 JSON 응답 생성
                String jsonResponse = createMockGptResponse(today);
                message.setContent(jsonResponse);

                choice.setMessage(message);
                List<AIChatResponse.Choice> choices = new ArrayList<>();
                choices.add(choice);
                mockResponse.setChoices(choices);

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

        private String createMockGptResponse(LocalDate startDate) {
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("[");

                // 첫째 날 장소들
                jsonBuilder.append(
                                "{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770,\"availableDate\":\"")
                                .append(startDate.format(formatter)).append("\"},");
                jsonBuilder.append(
                                "{\"name\":\"Insadong\",\"latitude\":37.5746,\"longitude\":126.9850,\"availableDate\":\"")
                                .append(startDate.format(formatter)).append("\"},");

                // 둘째 날 장소들
                LocalDate secondDay = startDate.plusDays(1);
                jsonBuilder
                                .append("{\"name\":\"Namsan Tower\",\"latitude\":37.5512,\"longitude\":126.9882,\"availableDate\":\"")
                                .append(secondDay.format(formatter)).append("\"},");
                jsonBuilder.append(
                                "{\"name\":\"Myeongdong\",\"latitude\":37.5635,\"longitude\":126.9850,\"availableDate\":\"")
                                .append(secondDay.format(formatter)).append("\"}");

                jsonBuilder.append("]");
                return jsonBuilder.toString();
        }

        private List<TravelLocation> createMockLocations() {
                List<TravelLocation> locations = new ArrayList<>();

                // 첫째 날 장소들
                TravelLocation location1 = new TravelLocation();
                location1.setPlaceName("Gyeongbokgung Palace");
                location1.setCoordinates(126.9770, 37.5796);
                location1.setLocationOrder(0);
                location1.setDescription("경복궁");
                location1.setAvailableDate(today);
                locations.add(location1);

                TravelLocation location2 = new TravelLocation();
                location2.setPlaceName("Insadong");
                location2.setCoordinates(126.9850, 37.5746);
                location2.setLocationOrder(1);
                location2.setDescription("인사동");
                location2.setAvailableDate(today);
                locations.add(location2);

                // 둘째 날 장소들
                LocalDate secondDay = today.plusDays(1);
                TravelLocation location3 = new TravelLocation();
                location3.setPlaceName("Namsan Tower");
                location3.setCoordinates(126.9882, 37.5512);
                location3.setLocationOrder(2);
                location3.setDescription("남산타워");
                location3.setAvailableDate(secondDay);
                locations.add(location3);

                TravelLocation location4 = new TravelLocation();
                location4.setPlaceName("Myeongdong");
                location4.setCoordinates(126.9850, 37.5635);
                location4.setLocationOrder(3);
                location4.setDescription("명동");
                location4.setAvailableDate(secondDay);
                locations.add(location4);

                return locations;
        }

        @Test
        void testGptResponseToTravelPlanAndLocations() throws JsonProcessingException {
                // 경로 최적화 서비스 모킹
                List<TravelLocation> mockLocations = createMockLocations();
                when(routeOptimizationService.optimizeRoute(any())).thenReturn(mockLocations);

                // When
                List<TravelLocation> result = tripPlanService.generateTripPlan(request);

                // Then
                assertNotNull(result);
                assertEquals(4, result.size());

                // 장소 이름 확인
                assertTrue(result.stream().anyMatch(loc -> loc.getPlaceName().equals("Gyeongbokgung Palace")));
                assertTrue(result.stream().anyMatch(loc -> loc.getPlaceName().equals("Insadong")));
                assertTrue(result.stream().anyMatch(loc -> loc.getPlaceName().equals("Namsan Tower")));
                assertTrue(result.stream().anyMatch(loc -> loc.getPlaceName().equals("Myeongdong")));

                // 날짜별 장소 확인
                LocalDate firstDay = LocalDate.parse(request.getStartDate(), formatter);
                LocalDate secondDay = firstDay.plusDays(1);

                List<TravelLocation> firstDayLocations = result.stream()
                                .filter(loc -> loc.getAvailableDate().equals(firstDay))
                                .toList();

                List<TravelLocation> secondDayLocations = result.stream()
                                .filter(loc -> loc.getAvailableDate().equals(secondDay))
                                .toList();

                assertEquals(2, firstDayLocations.size());
                assertEquals(2, secondDayLocations.size());
        }

        @Test
        void testGptResponseHandlerParseValidJson() {
                // Given
                String validJson = "[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770,\"availableDate\":\""
                                + today.format(formatter) + "\"}]";

                // When
                List<AIRecommendedLocationDTO> result = gptResponseHandler.parseGptResponse(validJson);

                // Then
                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals("Gyeongbokgung Palace", result.get(0).getName());
        }
}