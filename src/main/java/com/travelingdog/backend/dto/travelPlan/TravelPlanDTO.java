package com.travelingdog.backend.dto.travelPlan;

import java.time.LocalDate;
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
        private String country;
        private String city;
        private LocalDate startDate;
        private LocalDate endDate;
        private List<TravelStyleDTO> travelStyles;
        private List<InterestDTO> interests;
        private List<AccommodationTypeDTO> accommodation;
        private List<TransportationTypeDTO> transportation;
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
                                .travelStyles(entity.getTravelStyles().stream()
                                                .map(TravelStyleDTO::fromEntity)
                                                .collect(Collectors.toList()))
                                .interests(entity.getInterests().stream()
                                                .map(InterestDTO::fromEntity)
                                                .collect(Collectors.toList()))
                                .accommodation(entity.getAccommodationTypes().stream()
                                                .map(AccommodationTypeDTO::fromEntity)
                                                .collect(Collectors.toList()))
                                .transportation(entity.getTransportationTypes().stream()
                                                .map(TransportationTypeDTO::fromEntity)
                                                .collect(Collectors.toList()))
                                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                                .nickname(entity.getUser() != null ? entity.getUser().getNickname() : null)
                                .itineraries(entity.getItineraries().stream()
                                                .map(ItineraryDTO::fromEntity)
                                                .collect(Collectors.toList()))
                                .viewCount(entity.getViewCount())
                                .likeCount(entity.getLikeCount())
                                .status(entity.getStatus())

                                .build();
        }

}