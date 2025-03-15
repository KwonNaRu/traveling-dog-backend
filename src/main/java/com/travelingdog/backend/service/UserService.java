package com.travelingdog.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travelingdog.backend.dto.UserProfileDTO;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(User user) {
        if (user == null) {
            throw new IllegalArgumentException("사용자 정보가 없습니다.");
        }

        // 최신 사용자 정보 조회 (선택적)
        User refreshedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return UserProfileDTO.fromEntity(refreshedUser);
    }
}