package com.travelingdog.backend.service;

import com.travelingdog.backend.model.TravelLocation;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteOptimizationService {

    // 하버사인 공식을 이용해 두 좌표 사이의 거리를 계산 (단위: km)
    private double calculateDistance(Point p1, Point p2) {
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
     * TravelLocation 리스트를 availableDate별로 그룹화한 후,
     * 각 날짜 내에서 최근접 이웃 방식으로 정렬하여 전체 리스트를 반환합니다.
     */
    public List<TravelLocation> optimizeRoute(List<TravelLocation> locations) {
        if (locations == null || locations.isEmpty()) {
            return Collections.emptyList();
        }
        // availableDate 기준 그룹화
        Map<LocalDate, List<TravelLocation>> grouped = locations.stream()
                .collect(Collectors.groupingBy(TravelLocation::getAvailableDate));

        List<LocalDate> sortedDates = new ArrayList<>(grouped.keySet());
        Collections.sort(sortedDates);

        List<TravelLocation> result = new ArrayList<>();
        for (LocalDate date : sortedDates) {
            List<TravelLocation> dailyList = new ArrayList<>(grouped.get(date));
            if (dailyList.isEmpty())
                continue;
            List<TravelLocation> sortedDaily = new ArrayList<>();
            // 첫 위치 선택 (리스트의 첫번째)
            TravelLocation current = dailyList.remove(0);
            sortedDaily.add(current);
            while (!dailyList.isEmpty()) {
                TravelLocation next = null;
                double minDistance = Double.MAX_VALUE;
                for (TravelLocation loc : dailyList) {
                    double distance = calculateDistance(current.getCoordinates(), loc.getCoordinates());
                    if (distance < minDistance) {
                        minDistance = distance;
                        next = loc;
                    }
                }
                if (next != null) {
                    sortedDaily.add(next);
                    dailyList.remove(next);
                    current = next;
                }
            }
            result.addAll(sortedDaily);
        }
        return result;
    }
}