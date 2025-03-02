package com.travelingdog.backend.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.status.PlanStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanDTO {
    private Long id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long userId;
    private List<TravelLocationDTO> travelLocations;
    private int viewCount;
    private int likeCount;
    private PlanStatus status;

    public static TravelPlanDTO fromEntity(TravelPlan entity) {
        if (entity == null) {
            return null;
        }

        return TravelPlanDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .travelLocations(entity.getTravelLocations().stream()
                        .map(TravelLocationDTO::fromEntity)
                        .collect(Collectors.toList()))
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikes().size())
                .status(entity.getStatus())
                .build();
    }
}