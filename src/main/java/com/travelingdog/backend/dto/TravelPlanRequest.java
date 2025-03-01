package com.travelingdog.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class TravelPlanRequest {
    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Start date is required")
    private String startDate; // yyyy-MM-dd 형식

    @NotBlank(message = "End date is required")
    private String endDate; // yyyy-MM-dd 형식

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}