package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import com.travelingdog.backend.model.TravelLocation;

@Tag("unit")
public class RouteOptimizationServiceUnitTest {

    private RouteOptimizationService routeOptimizationService;
    private List<TravelLocation> locations;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        routeOptimizationService = new RouteOptimizationService();
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
}