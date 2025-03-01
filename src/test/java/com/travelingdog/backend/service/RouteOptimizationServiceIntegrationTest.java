package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.travelingdog.backend.model.TravelLocation;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
public class RouteOptimizationServiceIntegrationTest {

    @Autowired
    private RouteOptimizationService routeOptimizationService;

    @MockBean
    private WebClient webClient;

    private List<TravelLocation> locations;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        // WebClient 모킹 설정
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = org.mockito.Mockito
                .mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = org.mockito.Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = org.mockito.Mockito.mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

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

        // RouteOptimizationService의 webClient 필드를 모킹된 webClient로 교체
        ReflectionTestUtils.setField(routeOptimizationService, "webClient", webClient);

        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        locations = new ArrayList<>();

        // 서울의 주요 관광지 좌표 (실제 좌표와 유사)
        // 첫째 날 - 북쪽 지역
        createLocation("경복궁", 126.9770, 37.5796, 0, LocalDate.now());
        createLocation("창덕궁", 126.9910, 37.5792, 1, LocalDate.now());
        createLocation("북촌한옥마을", 126.9850, 37.5826, 2, LocalDate.now());
        createLocation("인사동", 126.9850, 37.5740, 3, LocalDate.now());

        // 둘째 날 - 남쪽 지역
        createLocation("남산타워", 126.9883, 37.5511, 4, LocalDate.now().plusDays(1));
        createLocation("명동", 126.9822, 37.5636, 5, LocalDate.now().plusDays(1));
        createLocation("동대문디자인플라자", 127.0094, 37.5669, 6, LocalDate.now().plusDays(1));
        createLocation("코엑스", 127.0586, 37.5126, 7, LocalDate.now().plusDays(1));
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
    void testOptimizeRoute_GroupsByDate() {
        List<TravelLocation> result = routeOptimizationService.optimizeRoute(locations);

        // 결과 리스트의 크기가 원본과 같은지 확인
        assertEquals(locations.size(), result.size());

        // 날짜별로 그룹화되어 있는지 확인
        Map<LocalDate, List<TravelLocation>> grouped = result.stream()
                .collect(Collectors.groupingBy(TravelLocation::getAvailableDate));

        assertEquals(2, grouped.size());
        assertTrue(grouped.containsKey(LocalDate.now()));
        assertTrue(grouped.containsKey(LocalDate.now().plusDays(1)));
    }

    @Test
    void testSimulatedAnnealing_OptimizesRoute() {
        // 같은 날짜의 위치만 포함하는 리스트 생성
        List<TravelLocation> sameDay = locations.stream()
                .filter(loc -> loc.getAvailableDate().equals(LocalDate.now()))
                .collect(Collectors.toList());

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

        // 최적화 전후 거리 비교
        double originalDistance = routeOptimizationService.calculateTotalDistance(sameDay);
        System.out.println("원래 경로 거리: " + originalDistance + "km");
        System.out.println("최적화된 경로 거리: " + totalDistance + "km");

        // 최적화된 경로가 원래 경로보다 짧거나 같아야 함
        // 참고: 시뮬레이티드 어닐링은 확률적 알고리즘이므로 항상 더 짧은 경로를 보장하지는 않음
        assertTrue(totalDistance <= originalDistance * 1.1,
                "최적화된 경로가 원래 경로보다 10% 이상 길면 안됩니다.");
    }

    @Test
    void testCompareOptimizationAlgorithms() {
        // 같은 날짜의 위치만 포함하는 리스트 생성
        List<TravelLocation> sameDay = locations.stream()
                .filter(loc -> loc.getAvailableDate().equals(LocalDate.now()))
                .collect(Collectors.toList());

        // 기존 최근접 이웃 방식으로 최적화
        List<TravelLocation> nnResult = new ArrayList<>();
        List<TravelLocation> dailyList = new ArrayList<>(sameDay);

        // 첫 위치 선택 (리스트의 첫번째)
        TravelLocation current = dailyList.remove(0);
        nnResult.add(current);

        // 최근접 이웃 알고리즘 직접 구현 (calculateDistance 메서드 대신 직접 계산)
        while (!dailyList.isEmpty()) {
            TravelLocation next = null;
            double minDistance = Double.MAX_VALUE;
            for (TravelLocation loc : dailyList) {
                // 하버사인 공식으로 직접 거리 계산
                double distance = calculateHaversineDistance(
                        current.getCoordinates(), loc.getCoordinates());
                if (distance < minDistance) {
                    minDistance = distance;
                    next = loc;
                }
            }
            if (next != null) {
                nnResult.add(next);
                dailyList.remove(next);
                current = next;
            }
        }

        // 시뮬레이티드 어닐링 알고리즘으로 최적화
        List<TravelLocation> saResult = routeOptimizationService.optimizeRouteWithSimulatedAnnealing(sameDay);

        // 두 알고리즘의 결과 비교
        double nnDistance = routeOptimizationService.calculateTotalDistance(nnResult);
        double saDistance = routeOptimizationService.calculateTotalDistance(saResult);

        System.out.println("최근접 이웃 방식 경로 거리: " + nnDistance + "km");
        System.out.println("시뮬레이티드 어닐링 경로 거리: " + saDistance + "km");
        System.out.println("개선율: " + ((nnDistance - saDistance) / nnDistance * 100) + "%");

        // 시뮬레이티드 어닐링이 최근접 이웃보다 더 나은 결과를 제공하는지 확인
        // 참고: 시뮬레이티드 어닐링은 확률적 알고리즘이므로 항상 더 짧은 경로를 보장하지는 않음
        assertTrue(saDistance <= nnDistance * 1.1,
                "시뮬레이티드 어닐링 경로가 최근접 이웃 경로보다 10% 이상 길면 안됩니다.");
    }

    @Test
    void testGoogleMapsDistance() {
        // 두 위치 간의 실제 거리 계산
        double distance = routeOptimizationService.getGoogleMapsDistance(
                locations.get(0).getCoordinates(),
                locations.get(1).getCoordinates());

        // 모킹된 응답에 따라 1.5km가 반환되어야 함
        assertEquals(1.5, distance, 0.01);
    }

    @Test
    void testOptimizeRouteWithRealDistances() {
        // 같은 날짜의 위치만 포함하는 리스트 생성
        List<TravelLocation> sameDay = locations.stream()
                .filter(loc -> loc.getAvailableDate().equals(LocalDate.now()))
                .collect(Collectors.toList());

        // 실제 거리 기반 최적화
        List<TravelLocation> result = routeOptimizationService.optimizeRouteWithRealDistances(sameDay);

        // 결과 리스트의 크기가 원본과 같은지 확인
        assertEquals(sameDay.size(), result.size());

        // 모든 위치가 같은 날짜인지 확인
        for (TravelLocation loc : result) {
            assertEquals(LocalDate.now(), loc.getAvailableDate());
        }
    }

    // 하버사인 공식을 이용한 거리 계산 (테스트용)
    private double calculateHaversineDistance(org.locationtech.jts.geom.Point p1, org.locationtech.jts.geom.Point p2) {
        double lat1 = p1.getY();
        double lon1 = p1.getX();
        double lat2 = p2.getY();
        double lon2 = p2.getX();
        double earthRadius = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}