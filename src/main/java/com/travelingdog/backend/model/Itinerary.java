package com.travelingdog.backend.model;

import java.util.ArrayList;
import java.util.List;

import com.travelingdog.backend.dto.AIRecommendedItineraryDTO;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Integer date; // 여행 일자

    @NotNull
    @Column(nullable = false)
    private String location; // 일정 위치(지역명)

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItineraryActivity> activities = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "travel_plan_id")
    private TravelPlan travelPlan; // 여행 계획과의 관계

    public void addActivity(ItineraryActivity activity) {
        activities.add(activity);
        activity.setItinerary(this);
    }

    public void removeActivity(ItineraryActivity activity) {
        activities.remove(activity);
        activity.setItinerary(null);
    }

    public static Itinerary fromDto(AIRecommendedItineraryDTO dto, TravelPlan travelPlan) {
        Itinerary itinerary = new Itinerary();
        itinerary.setDate(dto.getDate());
        itinerary.setLocation(dto.getLocation());
        itinerary.setTravelPlan(travelPlan); // TravelPlan 설정 추가

        // 활동 정보 변환
        if (dto.getActivities() != null) {
            for (AIRecommendedItineraryDTO.Location activity : dto.getActivities()) {
                ItineraryActivity activityEntity = new ItineraryActivity();
                activityEntity.setName(activity.getName());
                activityEntity.setDescription(activity.getDescription());
                activityEntity.setLocationName(activity.getLocationName());
                itinerary.addActivity(activityEntity); // 연관관계 메서드 사용
            }
        }

        return itinerary;
    }
}