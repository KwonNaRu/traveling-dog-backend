package com.travelingdog.backend.dto.travelPlan;

import com.travelingdog.backend.model.RestaurantRecommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantRecommendationDTO {

    private Long id;
    private String locationName;
    private String description;

    public static RestaurantRecommendationDTO fromEntity(RestaurantRecommendation entity) {
        return RestaurantRecommendationDTO.builder()
                .id(entity.getId())
                .locationName(entity.getLocationName())
                .description(entity.getDescription())
                .build();
    }
}
