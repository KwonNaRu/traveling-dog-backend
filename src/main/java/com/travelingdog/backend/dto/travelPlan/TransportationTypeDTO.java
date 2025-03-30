package com.travelingdog.backend.dto.travelPlan;

import com.travelingdog.backend.model.Transportation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportationTypeDTO {

    private Long id;

    private String name;

    public static TransportationTypeDTO fromEntity(Transportation entity) {
        return TransportationTypeDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
