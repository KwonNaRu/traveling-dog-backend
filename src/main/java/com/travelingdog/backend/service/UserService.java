package com.travelingdog.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travelingdog.backend.dto.UserProfileDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(User user) {

        // 최신 사용자 정보 조회 (선택적)
        User refreshedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<TravelPlan> travelPlans = travelPlanRepository.findAllByUser(user);
        List<TravelPlanDTO> travelPlanDTOs = travelPlans.stream()
                .map(TravelPlanDTO::fromEntity)
                .collect(Collectors.toList());

        return UserProfileDTO.fromEntity(refreshedUser, travelPlanDTOs);
    }
}