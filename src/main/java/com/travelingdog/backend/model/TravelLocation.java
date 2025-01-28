package com.travelingdog.backend.model;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

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
    private String placeName; // 장소 이름

    @Column(columnDefinition = "GEOMETRY(Point, 4326)")
    private Point coordinates;

    @Column(length = 500)
    private String description; // 장소 설명

    @Column(name = "location_order", nullable = false)
    @PositiveOrZero(message = "Order must be positive number")
    private int locationOrder; // 여행 계획 내 순서

    @ManyToOne
    @JoinColumn(name = "travel_plan_id", nullable = false)
    private TravelPlan travelPlan; // 여행 계획과의 관계

    public void setCoordinates(double longitude, double latitude) {
        this.coordinates = new GeometryFactory().createPoint(new Coordinate(longitude, latitude));
    }
}
