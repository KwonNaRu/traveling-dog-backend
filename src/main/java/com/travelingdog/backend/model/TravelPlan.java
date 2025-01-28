package com.travelingdog.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.travelingdog.backend.auditing.BaseTimeEntity;
import com.travelingdog.backend.status.PlanStatus;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlan extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title; // 여행 계획 제목

    @Column(name = "start_date", nullable = false)
    @FutureOrPresent(message = "Start date must be in the present or future")
    private LocalDate startDate; // 여행 시작 날짜

    @Column(name = "end_date", nullable = false)
    @Future(message = "End date must be in the future")
    private LocalDate endDate; // 여행 종료 날짜

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자와의 관계

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TravelLocation> travelLocations = new ArrayList<>(); // 여행 위치 리스트

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlanStatus status = PlanStatus.DRAFT; // 초기값 설정

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void addTravelLocation(TravelLocation travelLocation) {
        travelLocations.add(travelLocation);
        travelLocation.setTravelPlan(this);
    }

    public void removeTravelLocation(TravelLocation travelLocation) {
        travelLocations.remove(travelLocation);
        travelLocation.setTravelPlan(null);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = PlanStatus.DELETED;
    }
}