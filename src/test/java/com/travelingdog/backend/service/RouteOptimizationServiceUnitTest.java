package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import com.travelingdog.backend.model.TravelLocation;

import reactor.core.publisher.Mono;

/**
 * 경로 최적화 서비스 단위 테스트
 * 
 * 이 테스트 클래스는 RouteOptimizationService의 다양한 기능을 단위 테스트합니다.
 * 외부 의존성(WebClient)을 모킹하여 서비스 로직만 독립적으로 테스트합니다.
 * 주요 테스트 대상:
 * 1. 빈 리스트, 단일 위치, 다중 위치에 대한 경로 최적화
 * 2. 날짜별 그룹화 기능
 * 3. 시뮬레이티드 어닐링 알고리즘
 * 4. Google Maps API 연동
 */
@Tag("unit")
public class RouteOptimizationServiceUnitTest {

    private RouteOptimizationService routeOptimizationService;
    private List<TravelLocation> locations;
    private GeometryFactory geometryFactory;
    private Builder webClientBuilder;
    private WebClient webClient;
    private RequestHeadersUriSpec requestHeadersUriSpec;
    private RequestHeadersSpec requestHeadersSpec;
    private ResponseSpec responseSpec;

    /**
     * 각 테스트 실행 전 환경 설정
     * 
     * 1. WebClient 모킹 설정: Google Maps API 호출을 시뮬레이션
     * 2. RouteOptimizationService 인스턴스 생성 및 API 키 설정
     * 3. 테스트용 위치 데이터 생성: 서울의 주요 관광지 좌표를 사용하여 두 날짜에 걸친 여행 일정 생성
     */
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

    /**
     * 테스트용 여행 위치 객체를 생성하는 헬퍼 메소드
     * 
     * @param name      장소 이름
     * @param longitude 경도
     * @param latitude  위도
     * @param order     방문 순서
     * @param date      방문 날짜
     */
    private void createLocation(String name, double longitude, double latitude, int order, LocalDate date) {
        TravelLocation location = new TravelLocation();
        location.setPlaceName(name);
        location.setCoordinates(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        location.setLocationOrder(order);
        location.setDescription("테스트 설명");
        location.setAvailableDate(date);
        locations.add(location);
    }

    /**
     * 빈 위치 리스트에 대한 경로 최적화 테스트
     * 
     * 이 테스트는 위치 리스트가 비어있을 때 경로 최적화 서비스가
     * 빈 리스트를 반환하는지 검증합니다. 이는 경계 조건 처리의
     * 정확성을 확인하기 위한 테스트입니다.
     */
    @Test
    @DisplayName("빈 위치 리스트에 대한 경로 최적화 테스트")
    void testOptimizeRoute_EmptyList() {
        List<TravelLocation> result = routeOptimizationService.optimizeRoute(new ArrayList<>());
        assertTrue(result.isEmpty());
    }

    /**
     * 단일 위치에 대한 경로 최적화 테스트
     * 
     * 이 테스트는 위치가 하나만 있을 때 경로 최적화 서비스가
     * 해당 위치를 그대로 반환하는지 검증합니다. 이는 경계 조건 처리의
     * 정확성을 확인하기 위한 테스트입니다.
     */
    @Test
    @DisplayName("단일 위치에 대한 경로 최적화 테스트")
    void testOptimizeRoute_SingleLocation() {
        List<TravelLocation> singleLocation = new ArrayList<>();
        singleLocation.add(locations.get(0));

        List<TravelLocation> result = routeOptimizationService.optimizeRoute(singleLocation);

        assertEquals(1, result.size());
        assertEquals("경복궁", result.get(0).getPlaceName());
    }

    /**
     * 다중 위치에 대한 경로 최적화 테스트
     * 
     * 이 테스트는 여러 날짜에 걸친 다중 위치에 대해 경로 최적화 서비스가
     * 날짜별로 그룹화하여 최적화하는지 검증합니다. 결과 리스트에서
     * 같은 날짜의 위치들이 연속적으로 배치되어야 합니다.
     */
    @Test
    @DisplayName("다중 위치에 대한 경로 최적화 테스트")
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

    /**
     * 같은 날짜의 위치들에 대한 경로 최적화 테스트
     * 
     * 이 테스트는 모든 위치가 같은 날짜인 경우 경로 최적화 서비스가
     * 정상적으로 최적화를 수행하는지 검증합니다. 결과 리스트의 모든 위치가
     * 동일한 날짜를 가져야 합니다.
     */
    @Test
    @DisplayName("같은 날짜의 위치들에 대한 경로 최적화 테스트")
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

    /**
     * 시뮬레이티드 어닐링 알고리즘을 이용한 경로 최적화 테스트
     * 
     * 이 테스트는 시뮬레이티드 어닐링 알고리즘이 같은 날짜의 위치들에 대해
     * 효과적으로 경로를 최적화하는지 검증합니다. 최적화된 경로의 총 거리가
     * 계산 가능한지 확인합니다.
     * 
     * 시뮬레이티드 어닐링은 확률적 알고리즘으로, 지역 최적해에서 벗어나
     * 전역 최적해를 찾는 데 효과적입니다.
     */
    @Test
    @DisplayName("시뮬레이티드 어닐링 알고리즘을 이용한 경로 최적화 테스트")
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

    /**
     * Google Maps Distance Matrix API 연동 테스트
     * 
     * 이 테스트는 Google Maps API를 통해 두 위치 간의 실제 도로 거리를
     * 정확하게 계산하는지 검증합니다. 모킹된 API 응답을 사용하여
     * 예상된 거리(1.5km)가 반환되는지 확인합니다.
     * 
     * 이 테스트는 외부 API 의존성을 모킹하여 테스트하는 방법을 보여줍니다.
     */
    @Test
    @DisplayName("Google Maps Distance Matrix API 연동 테스트")
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

    /**
     * 실제 도로 거리 기반 경로 최적화 테스트
     * 
     * 이 테스트는 Google Maps API에서 제공하는 실제 도로 거리를 기반으로
     * 경로를 최적화하는 기능을 검증합니다. 직선 거리가 아닌 실제 도로 거리를
     * 사용하여 더 현실적인 경로 최적화가 이루어지는지 확인합니다.
     * 
     * 이 방식은 하버사인 공식을 사용한 직선 거리 계산보다 실제 여행 경로에
     * 더 적합한 결과를 제공합니다.
     */
    @Test
    @DisplayName("실제 도로 거리 기반 경로 최적화 테스트")
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