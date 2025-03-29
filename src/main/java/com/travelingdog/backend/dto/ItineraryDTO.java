package com.travelingdog.backend.dto;

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
    private int date;

    public static ItineraryDTO fromEntity(Itinerary entity) {
        if (entity == null) {
            return null;
        }

        return ItineraryDTO.builder()
                .id(entity.getId())
                .location(entity.getLocation())
                .date(entity.getDate())
                .build();
    }

    public static Itinerary toEntity(ItineraryDTO dto) {
        return Itinerary.builder()
                .location(dto.getLocation())
                .date(dto.getDate())
                .build();
    }

}