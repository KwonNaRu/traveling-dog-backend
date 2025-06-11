package com.travelingdog.backend.dto.travelPlan;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserSpecifiedAccommodation {

    private String date;
    private String accommodation;

}
