package com.travelingdog.backend.dto.travelPlan;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TravelPlanRequest {

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotBlank(message = "Travel style is required")
    private String travelStyle;

    @NotBlank(message = "Budget is required")
    private String budget;

    @NotBlank(message = "Interests are required")
    private String interests;

    @NotBlank(message = "Accommodation is required")
    private String accommodation;

    @NotBlank(message = "Transportation is required")
    private String transportation;

}