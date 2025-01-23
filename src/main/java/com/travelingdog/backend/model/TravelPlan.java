package com.travelingdog.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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
}
