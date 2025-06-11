package com.travelingdog.backend.dto.travelPlan;

import java.util.List;
import java.util.stream.Collectors;

import com.travelingdog.backend.model.Itinerary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDTO {
    private Long id;
    private String location;
    private String date;
    private List<ItineraryActivityDTO> activities;

    public static ItineraryDTO fromEntity(Itinerary entity) {
        if (entity == null) {
            return null;
        }

        return ItineraryDTO.builder()
                .id(entity.getId())
                .location(entity.getLocation())
                .date(entity.getDate())
                .activities(entity.getActivities().stream()
                        .map(ItineraryActivityDTO::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    public static Itinerary toEntity(ItineraryDTO dto) {
        if (dto == null) {
            return null;
        }

        return Itinerary.builder()
                .id(dto.getId())
                .location(dto.getLocation() != null ? dto.getLocation() : "")
                .date(dto.getDate())
                .activities(dto.getActivities().stream()
                        .map(ItineraryActivityDTO::toEntity)
                        .collect(Collectors.toList()))
                .build();
    }

}