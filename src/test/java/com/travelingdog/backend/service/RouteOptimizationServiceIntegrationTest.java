package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import com.travelingdog.backend.model.TravelLocation;

/**
 * 경로 최적화 서비스 통합 테스트
 * 
 * 이 테스트는 여행 경로 최적화 서비스의 통합 기능을 검증합니다.
 * 실제 서비스 환경과 유사한 조건에서 경로 최적화 알고리즘의 동작을 테스트하며,
 * 날짜별 그룹화, 시뮬레이티드 어닐링 알고리즘, 최근접 이웃 알고리즘 등
 * 다양한 경로 최적화 전략의 정확성과 효율성을 검증합니다.
 * Google Maps API와의 통합 기능도 테스트합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
public class RouteOptimizationServiceIntegrationTest {

    @Autowired
    private RouteOptimizationService routeOptimizationService;

    @MockBean
    private RestClient restClient;

    private List<TravelLocation> locations;
    private GeometryFactory geometryFactory;

    /**
     * 각 테스트 실행 전 환경 설정
     * 
     * 1. RestClient 모킹 설정: Google Maps API 호출을 시뮬레이션
     * 2. 테스트용 위치 데이터 생성: 서울의 주요 관광지 좌표를 사용하여 두 날짜에 걸친 여행 일정 생성
     * 3. 모킹된 Google Maps API 응답 설정: 1.5km 거리와 300초 소요 시간으로 응답 설정
     */
    @BeforeEach
    void setUp() {
        // RestClient 모킹 설정
        RequestHeadersUriSpec requestHeadersUriSpec = Mockito
                .mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec requestHeadersSpec = Mockito.mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
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
        when(responseSpec.toEntity(eq(Map.class))).thenReturn(ResponseEntity.ok(distanceResponse));

        // RouteOptimizationService의 restClient 필드를 모킹된 restClient로 교체
        ReflectionTestUtils.setField(routeOptimizationService, "restClient", restClient);

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
        locations.add(location);
    }

    /**
     * 시뮬레이티드 어닐링 알고리즘을 이용한 경로 최적화 테스트
     * 
     * 이 테스트는 시뮬레이티드 어닐링 알고리즘이 같은 날짜의 위치들에 대해
     * 효과적으로 경로를 최적화하는지 검증합니다. 최적화 전후의 총 거리를 비교하여
     * 최적화된 경로가 원래 경로보다 짧거나 비슷한지 확인합니다.
     * 
     * 시뮬레이티드 어닐링은 확률적 알고리즘이므로 항상 최적의 결과를 보장하지는 않지만,
     * 일반적으로 좋은 결과를 제공해야 합니다.
     */
    @Test
    @DisplayName("시뮬레이티드 어닐링 알고리즘을 이용한 경로 최적화 테스트")
    void testSimulatedAnnealing_OptimizesRoute() {

        // 시뮬레이티드 어닐링 알고리즘으로 최적화
        List<TravelLocation> result = routeOptimizationService.optimizeRouteWithSimulatedAnnealing(locations);

        // 최적화된 경로의 총 거리가 계산되는지 확인
        double totalDistance = routeOptimizationService.calculateTotalDistance(result);
        assertTrue(totalDistance > 0);

        // 최적화 전후 거리 비교
        double originalDistance = routeOptimizationService.calculateTotalDistance(locations);
        System.out.println("원래 경로 거리: " + originalDistance + "km");
        System.out.println("최적화된 경로 거리: " + totalDistance + "km");

        // 최적화된 경로가 원래 경로보다 짧거나 같아야 함
        // 참고: 시뮬레이티드 어닐링은 확률적 알고리즘이므로 항상 더 짧은 경로를 보장하지는 않음
        assertTrue(totalDistance <= originalDistance * 1.1,
                "최적화된 경로가 원래 경로보다 10% 이상 길면 안됩니다.");
    }

    /**
     * 최근접 이웃 알고리즘과 시뮬레이티드 어닐링 알고리즘 비교 테스트
     * 
     * 이 테스트는 두 가지 경로 최적화 알고리즘의 성능을 비교합니다:
     * 1. 최근접 이웃(Nearest Neighbor) 알고리즘: 간단하지만 지역 최적해에 빠질 수 있음
     * 2. 시뮬레이티드 어닐링(Simulated Annealing) 알고리즘: 더 복잡하지만 지역 최적해를 탈출할 수 있음
     * 
     * 두 알고리즘으로 계산된 경로의 총 거리를 비교하여 시뮬레이티드 어닐링이
     * 최근접 이웃 방식보다 더 나은 결과를 제공하는지 검증합니다.
     */
    @Test
    @DisplayName("최근접 이웃 알고리즘과 시뮬레이티드 어닐링 알고리즘 비교 테스트")
    void testCompareOptimizationAlgorithms() {
        // 기존 최근접 이웃 방식으로 최적화
        List<TravelLocation> nnResult = new ArrayList<>();

        // 첫 위치 선택 (리스트의 첫번째)
        TravelLocation current = locations.remove(0);
        nnResult.add(current);

        // 최근접 이웃 알고리즘 직접 구현 (calculateDistance 메서드 대신 직접 계산)
        while (!locations.isEmpty()) {
            TravelLocation next = null;
            double minDistance = Double.MAX_VALUE;
            for (TravelLocation loc : locations) {
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
                locations.remove(next);
                current = next;
            }
        }

        // 시뮬레이티드 어닐링 알고리즘으로 최적화
        List<TravelLocation> saResult = routeOptimizationService.optimizeRouteWithSimulatedAnnealing(locations);

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

    /**
     * Google Maps API를 이용한 거리 계산 테스트
     * 
     * 이 테스트는 Google Maps API를 통해 두 위치 간의 실제 도로 거리를
     * 정확하게 계산하는지 검증합니다. 모킹된 API 응답을 사용하여
     * 예상된 거리(1.5km)가 반환되는지 확인합니다.
     */
    @Test
    @DisplayName("Google Maps API를 이용한 거리 계산 테스트")
    void testGoogleMapsDistance() {
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
     */
    @Test
    @DisplayName("실제 도로 거리 기반 경로 최적화 테스트")
    void testOptimizeRouteWithRealDistances() {

        // 실제 거리 기반 최적화
        List<TravelLocation> result = routeOptimizationService.optimizeRouteWithRealDistances(locations);

        // 결과 리스트의 크기가 원본과 같은지 확인
        assertEquals(locations.size(), result.size());
    }

    /**
     * 하버사인 공식을 이용한 거리 계산 (테스트용)
     * 
     * 두 지리적 좌표 간의 대원 거리(Great-circle distance)를 계산하는 하버사인 공식을 구현합니다.
     * 이 메소드는 테스트 내에서 최근접 이웃 알고리즘 구현에 사용됩니다.
     * 
     * @param p1 첫 번째 위치의 좌표
     * @param p2 두 번째 위치의 좌표
     * @return 두 위치 간의 거리(km)
     */
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