package com.travelingdog.backend.dto.travelPlan;

import com.travelingdog.backend.model.Interest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestDTO {
    private Long id;
    private String name;

    public static InterestDTO fromEntity(Interest entity) {
        return InterestDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
