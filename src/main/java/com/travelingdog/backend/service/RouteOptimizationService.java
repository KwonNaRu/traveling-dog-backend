package com.travelingdog.backend.service;

import com.travelingdog.backend.model.TravelLocation;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RouteOptimizationService {

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    private final RestClient restClient;

    // 시뮬레이티드 어닐링 파라미터
    private static final double INITIAL_TEMPERATURE = 10000;
    private static final double COOLING_RATE = 0.003;
    private static final int MAX_ITERATIONS = 1000;

    public RouteOptimizationService(Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    // 하버사인 공식을 이용해 두 좌표 사이의 거리를 계산 (단위: km)
    public double calculateDistance(Point p1, Point p2) {
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

    /**
     * 시뮬레이티드 어닐링 알고리즘을 사용하여 경로를 최적화합니다.
     * 이 알고리즘은 지역 최적해에 빠지는 것을 방지하고 전역 최적해를 찾는 데 도움이 됩니다.
     */
    public List<TravelLocation> optimizeRouteWithSimulatedAnnealing(List<TravelLocation> locations) {
        if (locations == null || locations.size() <= 1) {
            return new ArrayList<>(locations);
        }

        // 초기 경로 (입력 순서 그대로)
        List<TravelLocation> currentRoute = new ArrayList<>(locations);
        double currentDistance = calculateTotalDistance(currentRoute);

        List<TravelLocation> bestRoute = new ArrayList<>(currentRoute);
        double bestDistance = currentDistance;

        double temperature = INITIAL_TEMPERATURE;

        // 시뮬레이티드 어닐링 알고리즘 실행
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // 새로운 경로 생성 (두 위치 교환)
            List<TravelLocation> newRoute = new ArrayList<>(currentRoute);
            int pos1 = ThreadLocalRandom.current().nextInt(newRoute.size());
            int pos2 = ThreadLocalRandom.current().nextInt(newRoute.size());
            Collections.swap(newRoute, pos1, pos2);

            // 새 경로의 총 거리 계산
            double newDistance = calculateTotalDistance(newRoute);

            // 새 경로가 더 좋거나, 확률적으로 수락
            if (acceptRoute(currentDistance, newDistance, temperature)) {
                currentRoute = newRoute;
                currentDistance = newDistance;

                // 최적 경로 업데이트
                if (currentDistance < bestDistance) {
                    bestRoute = new ArrayList<>(currentRoute);
                    bestDistance = currentDistance;
                }
            }

            // 온도 감소
            temperature *= (1 - COOLING_RATE);
        }

        // 위치 순서 업데이트
        for (int i = 0; i < bestRoute.size(); i++) {
            bestRoute.get(i).setLocationOrder(i);
        }

        return bestRoute;
    }

    /**
     * 시뮬레이티드 어닐링에서 새 경로를 수락할지 결정합니다.
     */
    private boolean acceptRoute(double currentDistance, double newDistance, double temperature) {
        // 새 경로가 더 좋으면 항상 수락
        if (newDistance < currentDistance) {
            return true;
        }

        // 그렇지 않으면 확률적으로 수락 (온도가 높을수록 수락 확률 증가)
        double acceptanceProbability = Math.exp((currentDistance - newDistance) / temperature);
        return ThreadLocalRandom.current().nextDouble() < acceptanceProbability;
    }

    /**
     * 경로의 총 거리를 계산합니다.
     */
    public double calculateTotalDistance(List<TravelLocation> route) {
        double totalDistance = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            totalDistance += calculateDistance(
                    route.get(i).getCoordinates(),
                    route.get(i + 1).getCoordinates());
        }
        return totalDistance;
    }

    /**
     * Google Maps Distance Matrix API를 사용하여 실제 이동 거리를 계산합니다.
     */
    public double getGoogleMapsDistance(Point origin, Point destination) {
        if (googleMapsApiKey == null || googleMapsApiKey.isEmpty()) {
            // API 키가 없으면 하버사인 거리 반환
            return calculateDistance(origin, destination);
        }

        try {
            String originStr = origin.getY() + "," + origin.getX();
            String destStr = destination.getY() + "," + destination.getX();
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%s&destinations=%s&key=%s",
                    originStr, destStr, googleMapsApiKey);

            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(Map.class).getBody();

            if (response != null) {
                List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
                if (rows != null && !rows.isEmpty()) {
                    List<Map<String, Object>> elements = (List<Map<String, Object>>) rows.get(0).get("elements");
                    if (elements != null && !elements.isEmpty()) {
                        Map<String, Object> element = elements.get(0);
                        Map<String, Object> distance = (Map<String, Object>) element.get("distance");
                        if (distance != null) {
                            // 미터 단위를 킬로미터로 변환
                            return ((Number) distance.get("value")).doubleValue() / 1000.0;
                        }
                    }
                }
            }

            // API 응답 파싱 실패 시 하버사인 거리 반환
            return calculateDistance(origin, destination);
        } catch (Exception e) {
            // 예외 발생 시 하버사인 거리 반환
            return calculateDistance(origin, destination);
        }
    }

    /**
     * Google Maps API를 사용하여 실제 이동 거리를 기반으로 경로를 최적화합니다.
     */
    public List<TravelLocation> optimizeRouteWithRealDistances(List<TravelLocation> locations) {
        if (locations == null || locations.size() <= 1) {
            return new ArrayList<>(locations);
        }

        // 초기 경로 (입력 순서 그대로)
        List<TravelLocation> currentRoute = new ArrayList<>(locations);

        // 시뮬레이티드 어닐링 알고리즘과 유사하지만, 실제 이동 거리 사용
        double temperature = INITIAL_TEMPERATURE;

        List<TravelLocation> bestRoute = new ArrayList<>(currentRoute);
        double bestDistance = calculateTotalRealDistance(bestRoute);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // 새로운 경로 생성 (두 위치 교환)
            List<TravelLocation> newRoute = new ArrayList<>(currentRoute);
            int pos1 = ThreadLocalRandom.current().nextInt(newRoute.size());
            int pos2 = ThreadLocalRandom.current().nextInt(newRoute.size());
            Collections.swap(newRoute, pos1, pos2);

            // 새 경로의 총 실제 이동 거리 계산
            double currentDistance = calculateTotalRealDistance(currentRoute);
            double newDistance = calculateTotalRealDistance(newRoute);

            // 새 경로가 더 좋거나, 확률적으로 수락
            if (acceptRoute(currentDistance, newDistance, temperature)) {
                currentRoute = newRoute;

                // 최적 경로 업데이트
                if (newDistance < bestDistance) {
                    bestRoute = new ArrayList<>(currentRoute);
                    bestDistance = newDistance;
                }
            }

            // 온도 감소
            temperature *= (1 - COOLING_RATE);
        }

        // 위치 순서 업데이트
        for (int i = 0; i < bestRoute.size(); i++) {
            bestRoute.get(i).setLocationOrder(i);
        }

        return bestRoute;
    }

    /**
     * 경로의 총 실제 이동 거리를 계산합니다.
     */
    private double calculateTotalRealDistance(List<TravelLocation> route) {
        double totalDistance = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            totalDistance += getGoogleMapsDistance(
                    route.get(i).getCoordinates(),
                    route.get(i + 1).getCoordinates());
        }
        return totalDistance;
    }
}