package com.travelingdog.backend.dto.travelPlan;

import java.time.LocalDate;
import java.util.List;

import com.travelingdog.backend.status.PlanStatus;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanUpdateRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotNull(message = "시작 날짜는 필수입니다")
    @FutureOrPresent(message = "시작 날짜는 현재 또는 미래여야 합니다")
    private LocalDate startDate;

    @NotNull(message = "종료 날짜는 필수입니다")
    @Future(message = "종료 날짜는 미래여야 합니다")
    private LocalDate endDate;

    private List<ItineraryDTO> itineraries;
}