package com.travelingdog.backend.dto.travelPlan;

import java.time.LocalDate;
import java.util.List;

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

    private String travelStyle;

    private String interests;

    private String accommodation;

    private String transportation;

    private List<UserSpecifiedAccommodation> userSpecifiedAccommodations;

}