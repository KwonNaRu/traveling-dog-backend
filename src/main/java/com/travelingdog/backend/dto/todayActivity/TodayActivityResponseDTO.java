package com.travelingdog.backend.dto.todayActivity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodayActivityResponseDTO {

    private String location;
    private LocalDateTime createdAt;
    private List<ActivityRecommendation> restaurants;
    private List<ActivityRecommendation> cultureSpots;
    private List<ActivityRecommendation> shoppingSpots;
    private List<ActivityRecommendation> natureSpots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityRecommendation {
        private String locationName;
        private String category;
    }
}