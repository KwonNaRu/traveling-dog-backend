package com.travelingdog.backend.model;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 활동 이름

    @Column
    private String description; // 활동 설명

    @Column(columnDefinition = "GEOMETRY(Point, 4326)")
    private Point coordinates;

    @Column(nullable = false)
    @PositiveOrZero(message = "Order must be positive number")
    private int activityOrder; // 활동 순서

    @ManyToOne
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    // public void setCoordinates(double longitude, double latitude) {
    // this.coordinates = new GeometryFactory(new PrecisionModel(), 4326)
    // .createPoint(new Coordinate(longitude, latitude));
    // }

    // // coordinates 필드 문자열 반환
    // public String getCoordinatesString() {
    // if (coordinates == null) {
    // return "null";
    // }
    // return "Point(" + coordinates.getX() + ", " + coordinates.getY() + ")";
    // }
}