package com.travelingdog.backend.dto.travelPlan;

import com.travelingdog.backend.model.ItineraryActivity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryActivityDTO {
    private Long id;

    private String name; // 활동 이름

    private String description; // 활동 설명

    private Double latitude;
    private Double longitude;

    private int activityOrder; // 활동 순서

    public static ItineraryActivityDTO fromEntity(ItineraryActivity entity) {
        return ItineraryActivityDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .latitude(entity.getCoordinates().getX())
                .longitude(entity.getCoordinates().getY())
                .activityOrder(entity.getActivityOrder())
                .build();
    }
}
