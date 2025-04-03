package com.travelingdog.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIRecommendedItineraryDTO {
    private int date;
    private String location;
    private List<Location> activities;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Location {
        private String name;
        private double latitude;
        private double longitude;
        private String description;
    }
}