package com.travelingdog.backend.dto.travelPlan;

import com.travelingdog.backend.model.AccommodationRecommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationRecommendationDTO {
    private Long id;
    private String locationName;
    private String description;

    public static AccommodationRecommendationDTO fromEntity(AccommodationRecommendation entity) {
        return AccommodationRecommendationDTO.builder()
                .id(entity.getId())
                .locationName(entity.getLocationName())
                .description(entity.getDescription())
                .build();
    }
}
