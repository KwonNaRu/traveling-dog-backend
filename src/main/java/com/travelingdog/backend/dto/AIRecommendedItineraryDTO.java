package com.travelingdog.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    private String date;
    private String location;
    private List<Location> activities;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Location {
        private String title;

        @JsonProperty("location_name")
        private String locationName;

        private String description;

        private String cost; // 예상 비용 추가
    }
}