package com.travelingdog.backend.dto.travelPlan;

import com.travelingdog.backend.model.AccommodationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationTypeDTO {

    private Long id;

    private String name;

    public static AccommodationTypeDTO fromEntity(AccommodationType entity) {
        return AccommodationTypeDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
