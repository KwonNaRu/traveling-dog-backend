package com.travelingdog.backend.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class UserTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void whenSaveUser_thenFindById() {
        User user = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .preferredTravelStyle("Adventure")
                .favoriteDestinations(List.of("Paris", "New York"))
                .build();

        userRepository.save(user);

        User foundUser = userRepository.findById(user.getId()).orElse(null);
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo("testuser");
        assertThat(foundUser.getFavoriteDestinations()).containsExactly("Paris", "New York");
    }

    @Test
    public void whenUpdateUser_thenUpdatedAtChanges() throws InterruptedException {
        User user = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .preferredTravelStyle("Adventure")
                .build();

        userRepository.save(user);
        LocalDateTime initialUpdatedAt = user.getUpdatedAt();

        // 충분한 시간 차이를 주기 위해 대기
        Thread.sleep(100);

        user.setUsername("updatedUser");
        userRepository.saveAndFlush(user);

        User updatedUser = userRepository.findById(user.getId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getUsername()).isEqualTo("updatedUser");
        assertThat(updatedUser.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    @Test
    public void whenCreateUser_thenCreatedAtAndUpdatedAtAreSet() {
        User user = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .build();

        userRepository.save(user);

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isEqualTo(user.getUpdatedAt());
    }
}
