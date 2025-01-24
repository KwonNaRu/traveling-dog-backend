package com.travelingdog.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 여행 계획 제목

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // 여행 시작 날짜

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // 여행 종료 날짜

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자와의 관계

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TravelLocation> travelLocations = new ArrayList<>(); // 여행 위치 리스트

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addTravelLocation(TravelLocation travelLocation) {
        travelLocations.add(travelLocation);
        travelLocation.setTravelPlan(this);
    }

    public void removeTravelLocation(TravelLocation travelLocation) {
        travelLocations.remove(travelLocation);
        travelLocation.setTravelPlan(null);
    }
}
