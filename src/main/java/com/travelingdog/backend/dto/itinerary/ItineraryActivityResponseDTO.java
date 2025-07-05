package com.travelingdog.backend.dto.itinerary;

import com.travelingdog.backend.model.ItineraryActivity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryActivityResponseDTO {

    private Long id;
    private String title;
    private String description;
    private String locationName;
    private Long itineraryId;

    /**
     * 엔티티를 DTO로 변환
     */
    public static ItineraryActivityResponseDTO fromEntity(ItineraryActivity activity) {
        return ItineraryActivityResponseDTO.builder()
                .id(activity.getId())
                .title(activity.getTitle())
                .description(activity.getDescription())
                .locationName(activity.getLocationName())
                .itineraryId(activity.getItinerary() != null ? activity.getItinerary().getId() : null)
                .build();
    }
}