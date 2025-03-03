package com.travelingdog.backend.dto.travelPlan;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TravelPlanRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Start date is required")
    private LocalDate startDate;

    @NotBlank(message = "End date is required")
    private LocalDate endDate;
}