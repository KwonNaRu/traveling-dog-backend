package com.travelingdog.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.travelingdog.backend.auditing.BaseTimeEntity;
import com.travelingdog.backend.dto.AIRecommendedTravelPlanDTO;
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

    @NotNull
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

    @Column(name = "budget", length = 100)
    private String budget; // 예산

    @Column(name = "transportation_tips", length = 500)
    private String transportationTips; // 교통 팁

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // 사용자와의 관계

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Itinerary> itineraries = new ArrayList<>(); // 여행 위치 리스트

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TravelStyle> travelStyles = new ArrayList<>(); // 여행 스타일 리스트

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Interest> interests = new ArrayList<>(); // 관심사 리스트

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccommodationType> accommodationTypes = new ArrayList<>(); // 숙소 유형 리스트

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transportation> transportationTypes = new ArrayList<>(); // 교통 수단 리스트

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RestaurantRecommendation> restaurantRecommendations = new ArrayList<>(); // 맛집 추천 리스트

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PlanLike> likes = new ArrayList<>();

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlanStatus status = PlanStatus.DRAFT; // 초기값 설정

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void addItinerary(Itinerary itinerary) {
        itineraries.add(itinerary);
        itinerary.setTravelPlan(this);
    }

    public void removeItinerary(Itinerary itinerary) {
        itineraries.remove(itinerary);
        itinerary.setTravelPlan(null);
    }

    public void addTravelStyle(TravelStyle style) {
        travelStyles.add(style);
        style.setTravelPlan(this);
    }

    public void addInterest(Interest interest) {
        interests.add(interest);
        interest.setTravelPlan(this);
    }

    public void addAccommodationType(AccommodationType type) {
        accommodationTypes.add(type);
        type.setTravelPlan(this);
    }

    public void addTransportation(Transportation transportation) {
        transportationTypes.add(transportation);
        transportation.setTravelPlan(this);
    }

    public void addRestaurantRecommendation(RestaurantRecommendation recommendation) {
        restaurantRecommendations.add(recommendation);
        recommendation.setTravelPlan(this);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = PlanStatus.DELETED;
    }

    public void addLike(PlanLike planLike) {
        likes.add(planLike);
        planLike.setTravelPlan(this);
        this.likeCount++;
    }

    public void removeLike(PlanLike planLike) {
        likes.remove(planLike);
        planLike.setTravelPlan(null);
        this.likeCount--;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public static TravelPlan fromDTO(AIRecommendedTravelPlanDTO aiRecommendedPlan) {
        return TravelPlan.builder()
                .title(aiRecommendedPlan.getTripName())
                .country(aiRecommendedPlan.getCountry())
                .city(aiRecommendedPlan.getDestination())
                .startDate(LocalDate.parse(aiRecommendedPlan.getStartDate()))
                .endDate(LocalDate.parse(aiRecommendedPlan.getEndDate()))
                .budget(aiRecommendedPlan.getBudget())
                .transportationTips(aiRecommendedPlan.getTransportationTips())
                .build();
    }
}