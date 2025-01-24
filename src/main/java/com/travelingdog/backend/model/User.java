package com.travelingdog.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "preferred_travel_style")
    private String preferredTravelStyle; // 예: "Adventure", "Relaxation", "Cultural"

    @ElementCollection
    @CollectionTable(name = "favorite_destinations", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "destination")
    @Builder.Default
    private List<String> favoriteDestinations = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TravelPlan> travelPlans = new ArrayList<>();

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

    public void addTravelPlan(TravelPlan travelPlan) {
        this.travelPlans.add(travelPlan);
        travelPlan.setUser(this);
    }

    public void removeTravelPlan(TravelPlan travelPlan) {
        this.travelPlans.remove(travelPlan);
        travelPlan.setUser(null);
    }

}
