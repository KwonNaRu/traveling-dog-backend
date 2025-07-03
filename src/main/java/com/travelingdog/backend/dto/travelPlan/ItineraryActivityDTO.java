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

    private String title; // 활동 이름

    private String description; // 활동 설명

    private String locationName; // 활동 위치 이름

    private String cost; // 예상 비용

    public static ItineraryActivityDTO fromEntity(ItineraryActivity entity) {
        return ItineraryActivityDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .locationName(entity.getLocationName())
                .cost(entity.getCost())
                .build();
    }

    public static ItineraryActivity toEntity(ItineraryActivityDTO dto) {
        return ItineraryActivity.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .locationName(dto.getLocationName())
                .cost(dto.getCost())
                .build();
    }
}
