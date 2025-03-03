package com.travelingdog.backend.dto;

import java.time.LocalDate;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import com.travelingdog.backend.model.TravelLocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelLocationDTO {
    private Long id;
    private String placeName;
    private double longitude;
    private double latitude;
    private String description;
    private int locationOrder;
    private LocalDate availableDate;

    public static TravelLocationDTO fromEntity(TravelLocation entity) {
        if (entity == null) {
            return null;
        }

        return TravelLocationDTO.builder()
                .id(entity.getId())
                .placeName(entity.getPlaceName())
                .longitude(entity.getCoordinates() != null ? entity.getCoordinates().getX() : 0)
                .latitude(entity.getCoordinates() != null ? entity.getCoordinates().getY() : 0)
                .description(entity.getDescription())
                .locationOrder(entity.getLocationOrder())
                .availableDate(entity.getAvailableDate())
                .build();
    }

    public static TravelLocation toEntity(TravelLocationDTO dto) {
        return TravelLocation.builder()
                .placeName(dto.getPlaceName())
                .coordinates(new GeometryFactory().createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude())))
                .description(dto.getDescription())
                .locationOrder(dto.getLocationOrder())
                .availableDate(dto.getAvailableDate())
                .build();
    }

}