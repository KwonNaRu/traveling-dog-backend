package com.travelingdog.backend.model;

import java.time.LocalDate;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import com.travelingdog.backend.dto.TravelLocationDTO;

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
@EqualsAndHashCode(of = { "id", "placeName", "locationOrder" })
@ToString(of = { "id", "placeName", "description", "locationOrder" })
public class TravelLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String placeName; // 장소 이름

    @NotNull
    @Column(columnDefinition = "GEOMETRY(Point, 4326)")
    private Point coordinates;

    @Column(length = 500)
    private String description; // 장소 설명

    @Column(name = "location_order", nullable = false)
    @PositiveOrZero(message = "Order must be positive number")
    private int locationOrder; // 여행 계획 내 순서

    @NotNull
    @ManyToOne
    @JoinColumn(name = "travel_plan_id", nullable = false)
    private TravelPlan travelPlan; // 여행 계획과의 관계

    public void setCoordinates(double longitude, double latitude) {
        this.coordinates = new GeometryFactory().createPoint(new Coordinate(longitude, latitude));
    }

    // coordinates 필드를 toString에 포함시키기 위한 커스텀 메서드
    public String getCoordinatesString() {
        if (coordinates == null) {
            return "null";
        }
        return "Point(" + coordinates.getX() + ", " + coordinates.getY() + ")";
    }
}
