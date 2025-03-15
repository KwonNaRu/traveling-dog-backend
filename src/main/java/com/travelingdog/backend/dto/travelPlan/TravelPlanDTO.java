package com.travelingdog.backend.dto.travelPlan;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.travelingdog.backend.dto.TravelLocationDTO;
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
    private String country;
    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long userId;
    private List<TravelLocationDTO> travelLocations;
    private int viewCount;
    private int likeCount;
    private PlanStatus status;
    private Boolean isShared;

    public static TravelPlanDTO fromEntity(TravelPlan entity) {
        if (entity == null) {
            return null;
        }

        return TravelPlanDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .country(entity.getCountry())
                .city(entity.getCity())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .isShared(entity.getIsShared())
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