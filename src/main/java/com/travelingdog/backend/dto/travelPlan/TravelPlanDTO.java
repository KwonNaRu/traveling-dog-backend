package com.travelingdog.backend.dto.travelPlan;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.travelingdog.backend.dto.ItineraryDTO;
import com.travelingdog.backend.model.AccommodationType;
import com.travelingdog.backend.model.Interest;
import com.travelingdog.backend.model.Transportation;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.TravelStyle;
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
    private String season;
    private String travelStyle;
    private String budget;
    private String interests;
    private String accommodation;
    private String transportation;
    private Long userId;
    private String nickname;
    private List<ItineraryDTO> itineraries;
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
                .country(entity.getCountry())
                .city(entity.getCity())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .season(entity.getSeason())
                .travelStyle(entity.getTravelStyles().stream()
                        .map(TravelStyle::getName)
                        .collect(Collectors.joining(", ")))
                .budget(entity.getBudget())
                .interests(entity.getInterests().stream()
                        .map(Interest::getName)
                        .collect(Collectors.joining(", ")))
                .accommodation(entity.getAccommodationTypes().stream()
                        .map(AccommodationType::getName)
                        .collect(Collectors.joining(", ")))
                .transportation(entity.getTransportationTypes().stream()
                        .map(Transportation::getName)
                        .collect(Collectors.joining(", ")))
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .nickname(entity.getUser() != null ? entity.getUser().getNickname() : null)
                .itineraries(entity.getItineraries().stream()
                        .map(ItineraryDTO::fromEntity)
                        .collect(Collectors.toList()))
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikes().size())
                .status(entity.getStatus())
                .build();
    }

}