package com.travelingdog.backend.model;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = { "id", "name", "description" })
public class ItineraryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String name; // 장소 이름

    @Column(length = 500)
    private String description; // 장소 설명

    @NotNull
    @Column(columnDefinition = "GEOMETRY(Point, 4326)")
    private Point coordinates;

    @ManyToOne
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    public void setCoordinates(double longitude, double latitude) {
        this.coordinates = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(longitude, latitude));
    }

    // coordinates 필드 문자열 반환
    public String getCoordinatesString() {
        if (coordinates == null) {
            return "null";
        }
        return "Point(" + coordinates.getX() + ", " + coordinates.getY() + ")";
    }
}