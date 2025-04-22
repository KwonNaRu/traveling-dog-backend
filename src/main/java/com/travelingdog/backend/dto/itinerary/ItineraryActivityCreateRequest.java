package com.travelingdog.backend.dto.itinerary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryActivityCreateRequest {

    @NotBlank(message = "활동 제목은 필수입니다")
    @Size(min = 2, max = 100, message = "활동 제목은 2-100자 사이여야 합니다")
    private String title;

    @Size(max = 500, message = "설명은 최대 500자까지 가능합니다")
    private String description;

    @NotBlank(message = "활동 위치명은 필수입니다")
    @Size(min = 2, max = 100, message = "활동 위치명은 2-100자 사이여야 합니다")
    private String locationName;

    @NotNull(message = "일정 ID는 필수입니다")
    private Long itineraryId;
}