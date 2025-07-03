package com.travelingdog.backend.dto.restaurant;

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
public class RestaurantRecommendationResponseDTO {

    private List<RestaurantDTO> restaurants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantDTO {

        @JsonProperty("location_name")
        private String locationName;

        private String description;

        private String cuisine; // 음식 종류

        @JsonProperty("price_range")
        private String priceRange; // 가격대

        @JsonProperty("estimated_cost")
        private String estimatedCost; // 예상 비용

        private String address; // 주소

        @JsonProperty("opening_hours")
        private String openingHours; // 운영시간

        private String phone; // 전화번호

        private Double rating; // 평점

        @JsonProperty("recommended_dishes")
        private List<String> recommendedDishes; // 추천 메뉴
    }
}