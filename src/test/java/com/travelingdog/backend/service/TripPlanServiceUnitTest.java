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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.travelingdog.backend.dto.AIChatMessage;
import com.travelingdog.backend.dto.AIChatRequest;
import com.travelingdog.backend.dto.AIChatResponse;
import com.travelingdog.backend.dto.TravelPlanRequest;
import com.travelingdog.backend.model.TravelLocation;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class TripPlanServiceUnitTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RouteOptimizationService routeOptimizationService;

    @InjectMocks
    private TripPlanService tripPlanService;

    private TravelPlanRequest request;
    private AIChatResponse mockResponse;
    private List<TravelLocation> mockLocations;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @BeforeEach
    void setUp() {
        // API 키 설정
        ReflectionTestUtils.setField(tripPlanService, "openAiApiKey", "test-api-key");
        ReflectionTestUtils.setField(tripPlanService, "restTemplate", restTemplate);

        // 테스트 요청 데이터 설정
        LocalDate today = LocalDate.now();
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
        message.setContent(
                "[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770,\"availableDate\":\""
                        + today.format(formatter) + "\"}]");
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
        mockLocations.add(location);
    }

    @Test
    void testGenerateTripPlan() {
        // Given
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        when(routeOptimizationService.optimizeRoute(any())).thenReturn(mockLocations);

        // When
        List<TravelLocation> result = tripPlanService.generateTripPlan(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gyeongbokgung Palace", result.get(0).getPlaceName());
        assertNotNull(result.get(0).getAvailableDate());
        assertEquals(LocalDate.now(), result.get(0).getAvailableDate());
    }
}