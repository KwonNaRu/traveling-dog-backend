package com.travelingdog.backend.dto;

import java.util.List;

import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String email;
    private String nickname;
    private String preferredTravelStyle;
    private List<String> favoriteDestinations;
    private List<TravelPlanDTO> travelPlans;

    public static UserProfileDTO fromEntity(User user, List<TravelPlanDTO> travelPlans) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .preferredTravelStyle(user.getPreferredTravelStyle())
                .favoriteDestinations(user.getFavoriteDestinations())
                .travelPlans(travelPlans)
                .build();
    }
}