package com.travelingdog.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRecommendedTravelPlanDTO {

    @JsonProperty("trip_name")
    private String tripName;

    @JsonProperty("start_date")
    private String startDate;

    @JsonProperty("end_date")
    private String endDate;

    private String season;

    @JsonProperty("travel_style")
    private List<String> travelStyle;

    private String budget;

    private String destination;

    private List<String> interests;

    private List<String> accommodation;

    private List<String> transportation;

    private List<AIRecommendedItineraryDTO> itinerary;

    @JsonProperty("restaurant_recommendations")
    private List<LocationDTO> restaurantRecommendations;

    @JsonProperty("accommodation_recommendations")
    private List<LocationDTO> accommodationRecommendations;

    @JsonProperty("transportation_tips")
    private String transportationTips;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDTO {
        private String name;
        private String locationName;
        private String description;
    }

}
