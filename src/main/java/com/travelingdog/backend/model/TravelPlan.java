package com.travelingdog.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.travelingdog.backend.auditing.BaseTimeEntity;
import com.travelingdog.backend.status.PlanStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
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
public class TravelPlan extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, length = 100)
    private String title; // 여행 계획 제목

    @Column(nullable = false, length = 100)
    private String country; // 여행 국가

    @Column(nullable = false, length = 100)
    private String city; // 여행 도시

    @NotNull
    @Column(name = "start_date", nullable = false)
    @FutureOrPresent(message = "Start date must be in the present or future")
    private LocalDate startDate; // 여행 시작 날짜

    @NotNull
    @Column(name = "end_date", nullable = false)
    @Future(message = "End date must be in the future")
    private LocalDate endDate; // 여행 종료 날짜

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // 사용자와의 관계

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TravelLocation> travelLocations = new ArrayList<>(); // 여행 위치 리스트

    @NotNull
    @Column(name = "is_shared", nullable = false)
    private Boolean isShared;

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PlanLike> likes = new ArrayList<>();

    @Column(name = "view_count")
    @Builder.Default
    private int viewCount = 0;

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

    public void addLike(PlanLike planLike) {
        likes.add(planLike);
        planLike.setTravelPlan(this);
    }

    public void removeLike(PlanLike planLike) {
        likes.remove(planLike);
        planLike.setTravelPlan(null);
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

}