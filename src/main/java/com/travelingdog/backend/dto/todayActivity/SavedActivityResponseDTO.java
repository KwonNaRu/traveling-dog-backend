package com.travelingdog.backend.dto.todayActivity;

import com.travelingdog.backend.model.SavedActivity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedActivityResponseDTO {

    private Long id;
    private String locationName;
    private String category;
    private String savedLocation;
    private LocalDateTime createdAt;

    /**
     * Entity를 DTO로 변환하는 정적 메서드
     */
    public static SavedActivityResponseDTO fromEntity(SavedActivity savedActivity) {
        return new SavedActivityResponseDTO(
                savedActivity.getId(),
                savedActivity.getLocationName(),
                savedActivity.getCategory(),
                savedActivity.getSavedLocation(),
                savedActivity.getCreatedAt());
    }
}