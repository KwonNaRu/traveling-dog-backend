package com.travelingdog.backend.dto.travelPlan;

import com.travelingdog.backend.model.ItineraryLunch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryLunchDTO {
    private Long id;
    private String name; // 장소 이름
    private String description; // 장소 설명
    private Double latitude;
    private Double longitude;

    public static ItineraryLunchDTO fromEntity(ItineraryLunch entity) {
        return ItineraryLunchDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .latitude(entity.getCoordinates().getX())
                .longitude(entity.getCoordinates().getY())
                .build();
    }
}
