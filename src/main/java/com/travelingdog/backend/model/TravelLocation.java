package com.travelingdog.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
public class TravelLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int day; // 여행 일자

    @Column(nullable = false)
    private String location; // 일정 위치(지역명)

    // @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval =
    // true)
    // @Builder.Default
    // private List<ItineraryActivity> activities = new ArrayList<>();

    // @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    // private ItineraryLocation lunch;

    // @OneToOne(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval =
    // true)
    // private ItineraryLocation dinner;

    @ManyToOne
    @JoinColumn(name = "travel_plan_id", nullable = false)
    private TravelPlan travelPlan; // 여행 계획과의 관계
}
