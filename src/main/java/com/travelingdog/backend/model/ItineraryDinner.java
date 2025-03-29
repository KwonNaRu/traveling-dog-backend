package com.travelingdog.backend.model;

import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
public class ItineraryDinner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 장소 이름

    @Column(length = 500)
    private String description; // 장소 설명

    @Column(columnDefinition = "GEOMETRY(Point, 4326)")
    private Point coordinates;

    @OneToOne
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
