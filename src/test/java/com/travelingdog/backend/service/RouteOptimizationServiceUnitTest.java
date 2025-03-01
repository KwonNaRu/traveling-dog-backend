package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.travelingdog.backend.model.TravelLocation;

@Tag("unit")
public class RouteOptimizationServiceUnitTest {

    private RouteOptimizationService routeOptimizationService;
    private List<TravelLocation> locations;
    private GeometryFactory geometryFactory;
    private WebClient.Builder webClientBuilder;
    private WebClient webClient;
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        // Mock WebClient
        webClientBuilder = mock(WebClient.Builder.class);
        webClient = mock(WebClient.class);
        requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        routeOptimizationService = new RouteOptimizationService(webClientBuilder);
        // API 키 설정
        ReflectionTestUtils.setField(routeOptimizationService, "googleMapsApiKey", "test-api-key");

        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        locations = new ArrayList<>();

        // 서울의 주요 관광지 좌표 (실제 좌표와 유사)
        // 첫째 날 - 북쪽 지역
        createLocation("경복궁", 126.9770, 37.5796, 0, LocalDate.now());
        createLocation("창덕궁", 126.9910, 37.5792, 1, LocalDate.now());
        createLocation("북촌한옥마을", 126.9850, 37.5826, 2, LocalDate.now());

        // 둘째 날 - 남쪽 지역
        createLocation("남산타워", 126.9883, 37.5511, 3, LocalDate.now().plusDays(1));
        createLocation("명동", 126.9822, 37.5636, 4, LocalDate.now().plusDays(1));
        createLocation("동대문디자인플라자", 127.0094, 37.5669, 5, LocalDate.now().plusDays(1));
    }

    private void createLocation(String name, double longitude, double latitude, int order, LocalDate date) {
        TravelLocation location = new TravelLocation();
        location.setPlaceName(name);
        location.setCoordinates(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        location.setLocationOrder(order);
        location.setDescription("테스트 설명");
        location.setAvailableDate(date);
        locations.add(location);
    }

    @Test
    void testOptimizeRoute_EmptyList() {
        List<TravelLocation> result = routeOptimizationService.optimizeRoute(new ArrayList<>());
        assertTrue(result.isEmpty());
    }

    @Test
    void testOptimizeRoute_SingleLocation() {
        List<TravelLocation> singleLocation = new ArrayList<>();
        singleLocation.add(locations.get(0));

        List<TravelLocation> result = routeOptimizationService.optimizeRoute(singleLocation);

        assertEquals(1, result.size());
        assertEquals("경복궁", result.get(0).getPlaceName());
    }

    @Test
    void testOptimizeRoute_MultipleLocations() {
        List<TravelLocation> result = routeOptimizationService.optimizeRoute(locations);

        // 결과 리스트의 크기가 원본과 같은지 확인
        assertEquals(locations.size(), result.size());

        // 날짜별로 그룹화되어 있는지 확인
        LocalDate firstDay = LocalDate.now();
        LocalDate secondDay = LocalDate.now().plusDays(1);

        // 첫째 날 위치들이 먼저 오는지 확인
        for (int i = 0; i < 3; i++) {
            assertEquals(firstDay, result.get(i).getAvailableDate());
        }

        // 둘째 날 위치들이 나중에 오는지 확인
        for (int i = 3; i < 6; i++) {
            assertEquals(secondDay, result.get(i).getAvailableDate());
        }
    }

    @Test
    void testOptimizeRoute_LocationsWithSameDate() {
        // 같은 날짜의 위치만 포함하는 리스트 생성
        List<TravelLocation> sameDay = new ArrayList<>();
        for (TravelLocation loc : locations) {
            if (loc.getAvailableDate().equals(LocalDate.now())) {
                sameDay.add(loc);
            }
        }

        List<TravelLocation> result = routeOptimizationService.optimizeRoute(sameDay);

        // 결과 리스트의 크기가 원본과 같은지 확인
        assertEquals(sameDay.size(), result.size());

        // 모든 위치가 같은 날짜인지 확인
        for (TravelLocation loc : result) {
            assertEquals(LocalDate.now(), loc.getAvailableDate());
        }
    }

    @Test
    void testSimulatedAnnealing_OptimizesRoute() {
        // 같은 날짜의 위치만 포함하는 리스트 생성
        List<TravelLocation> sameDay = new ArrayList<>();
        for (TravelLocation loc : locations) {
            if (loc.getAvailableDate().equals(LocalDate.now())) {
                sameDay.add(loc);
            }
        }

        // 시뮬레이티드 어닐링 알고리즘으로 최적화
        List<TravelLocation> result = routeOptimizationService.optimizeRouteWithSimulatedAnnealing(sameDay);

        // 결과 리스트의 크기가 원본과 같은지 확인
        assertEquals(sameDay.size(), result.size());

        // 모든 위치가 같은 날짜인지 확인
        for (TravelLocation loc : result) {
            assertEquals(LocalDate.now(), loc.getAvailableDate());
        }

        // 최적화된 경로의 총 거리가 계산되는지 확인
        double totalDistance = routeOptimizationService.calculateTotalDistance(result);
        assertTrue(totalDistance > 0);
    }

    @Test
    void testGoogleMapsDistanceMatrix_ReturnsValidDistances() {
        // 테스트 데이터 준비
        Map<String, Object> distanceResponse = Map.of(
                "rows", List.of(
                        Map.of(
                                "elements", List.of(
                                        Map.of(
                                                "distance", Map.of("value", 1500),
                                                "duration", Map.of("value", 300))))));

        // Google Maps API 응답 모킹
        when(responseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(distanceResponse));

        // 두 위치 간의 실제 거리 계산
        double distance = routeOptimizationService.getGoogleMapsDistance(
                locations.get(0).getCoordinates(),
                locations.get(1).getCoordinates());

        // 모킹된 응답에 따라 1.5km가 반환되어야 함
        assertEquals(1.5, distance, 0.01);
    }

    @Test
    void testOptimizeRouteWithRealDistances() {
        // 테스트 데이터 준비
        Map<String, Object> distanceResponse = Map.of(
                "rows", List.of(
                        Map.of(
                                "elements", List.of(
                                        Map.of(
                                                "distance", Map.of("value", 1500),
                                                "duration", Map.of("value", 300))))));

        // Google Maps API 응답 모킹
        when(responseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(distanceResponse));

        // 같은 날짜의 위치만 포함하는 리스트 생성
        List<TravelLocation> sameDay = new ArrayList<>();
        for (TravelLocation loc : locations) {
            if (loc.getAvailableDate().equals(LocalDate.now())) {
                sameDay.add(loc);
            }
        }

        // 실제 거리 기반 최적화
        List<TravelLocation> result = routeOptimizationService.optimizeRouteWithRealDistances(sameDay);

        // 결과 리스트의 크기가 원본과 같은지 확인
        assertEquals(sameDay.size(), result.size());

        // 모든 위치가 같은 날짜인지 확인
        for (TravelLocation loc : result) {
            assertEquals(LocalDate.now(), loc.getAvailableDate());
        }
    }
}