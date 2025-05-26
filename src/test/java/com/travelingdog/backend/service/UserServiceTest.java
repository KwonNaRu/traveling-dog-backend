package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelingdog.backend.dto.UserProfileDTO;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Mock
    private TravelPlanRepository travelPlanRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("Test User")
                .preferredTravelStyle("Adventure")
                .favoriteDestinations(Arrays.asList("Seoul", "Tokyo", "Paris"))
                .build();
    }

    @Test
    @DisplayName("유효한 사용자 정보로 프로필 조회 시 프로필 정보를 반환한다")
    void getUserProfile_ValidUser_ReturnsProfile() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        // When
        UserProfileDTO profile = userService.getUserProfile(testUser);

        // Then
        assertNotNull(profile);
        assertEquals(1L, profile.getId());
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("Test User", profile.getNickname());
        assertEquals("Adventure", profile.getPreferredTravelStyle());
        assertEquals(Arrays.asList("Seoul", "Tokyo", "Paris"), profile.getFavoriteDestinations());
    }

    @Test
    @DisplayName("null 사용자로 프로필 조회 시 예외가 발생한다")
    void getUserProfile_NullUser_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserProfile(null);
        });
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 프로필 조회 시 예외가 발생한다")
    void getUserProfile_NonExistentUser_ThrowsException() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserProfile(testUser);
        });
    }
}