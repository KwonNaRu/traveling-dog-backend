package com.travelingdog.backend.model;

import java.util.ArrayList;
import java.util.List;

import com.travelingdog.backend.auditing.BaseTimeEntity;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = { "id", "day", "location" })
@ToString(of = { "id", "day", "location" })
@Table(name = "itinerary")
public class Itinerary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private int day; // 여행 일자

    @NotNull
    @Column(nullable = false)
    private String location; // 일정 위치(지역명)

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItineraryActivity> activities = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "lunch_id")
    private ItineraryLocation lunch;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "dinner_id")
    private ItineraryLocation dinner;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "travel_plan_id", nullable = false)
    private TravelPlan travelPlan; // 여행 계획과의 관계

    // AIRecommendedItineraryDTO를 Itinerary 엔티티로 변환하는 메소드
    public static Itinerary fromDto(AIRecommendedItineraryDTO dto, TravelPlan travelPlan) {
        Itinerary itinerary = new Itinerary();
        itinerary.setDay(dto.getDay());
        itinerary.setLocation(dto.getLocation());
        itinerary.setTravelPlan(travelPlan);

        // 점심 정보 변환
        if (dto.getLunch() != null) {
            ItineraryLocation lunch = new ItineraryLocation();
            lunch.setName(dto.getLunch().getName());
            lunch.setDescription(dto.getLunch().getDescription());
            lunch.setCoordinates(dto.getLunch().getLongitude(), dto.getLunch().getLatitude());
            lunch.setItinerary(itinerary);
            itinerary.setLunch(lunch);
        }

        // 저녁 정보 변환
        if (dto.getDinner() != null) {
            ItineraryLocation dinner = new ItineraryLocation();
            dinner.setName(dto.getDinner().getName());
            dinner.setDescription(dto.getDinner().getDescription());
            dinner.setCoordinates(dto.getDinner().getLongitude(), dto.getDinner().getLatitude());
            dinner.setItinerary(itinerary);
            itinerary.setDinner(dinner);
        }

        // 활동 정보 변환
        if (dto.getActivities() != null) {
            List<ItineraryActivity> activities = new ArrayList<>();
            int order = 0;
            for (AIRecommendedItineraryDTO.Location activity : dto.getActivities()) {
                ItineraryActivity activityEntity = new ItineraryActivity();
                activityEntity.setName(activity.getName());
                activityEntity.setDescription(activity.getDescription());
                activityEntity.setCoordinates(activity.getLongitude(), activity.getLatitude());
                activityEntity.setActivityOrder(order++);
                activityEntity.setItinerary(itinerary);
                activities.add(activityEntity);
            }
            itinerary.setActivities(activities);
        }

        return itinerary;
    }
}