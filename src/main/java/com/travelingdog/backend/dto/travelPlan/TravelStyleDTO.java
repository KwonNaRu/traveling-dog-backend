package com.travelingdog.backend.dto.travelPlan;

import com.travelingdog.backend.model.TravelStyle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelStyleDTO {

    private Long id;

    private String name;

    public static TravelStyleDTO fromEntity(TravelStyle entity) {
        return TravelStyleDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
