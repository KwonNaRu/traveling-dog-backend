package com.travelingdog.backend.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
public class UserTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TravelPlanRepository travelPlanRepository;

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
                assertThat(user.getUpdatedAt()).isCloseTo(user.getCreatedAt(), within(1, ChronoUnit.SECONDS));
        }

        /*
         * favoriteDestinations 수정 테스트
         */
        @Test
        public void whenUpdateFavoriteDestinations_thenFavoriteDestinationsUpdated() {
                List<String> initialDestinations = new ArrayList<>();
                initialDestinations.add("Paris");
                initialDestinations.add("New York");
                User user = User.builder()
                                .username("testuser")
                                .password("password")
                                .email("test@example.com")
                                .preferredTravelStyle("Adventure")
                                .favoriteDestinations(initialDestinations)
                                .build();

                userRepository.save(user);

                List<String> updatedDestinations = new ArrayList<>();
                updatedDestinations.add("Paris");
                updatedDestinations.add("New York");
                updatedDestinations.add("Tokyo");
                user.setFavoriteDestinations(updatedDestinations);
                userRepository.save(user);

                User updatedUser = userRepository.findById(user.getId()).orElse(null);
                assertThat(updatedUser).isNotNull();
                assertThat(updatedUser.getFavoriteDestinations()).containsExactly("Paris", "New York", "Tokyo");
        }

        /*
         * User를 삭제하면 TravelPlan도 삭제되는지 테스트
         */
        @Test
        public void whenDeleteUser_thenTravelPlansDeleted() {
                User user = User.builder()
                                .username("testuser")
                                .password("password")
                                .email("test@example.com")
                                .build();

                TravelPlan travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .build();

                user.addTravelPlan(travelPlan);
                userRepository.save(user);

                TravelPlan savedTravelPlan = userRepository.findById(user.getId()).orElse(null).getTravelPlans().get(0);
                assertThat(savedTravelPlan.getUser()).isEqualTo(user);

                userRepository.delete(user);
                user.removeTravelPlan(travelPlan);

                assertThat(userRepository.findById(user.getId()).orElse(null)).isNull();
                assertThat(travelPlanRepository.findById(travelPlan.getId()).orElse(null)).isNull();
        }

        /*
         * TravelPlan을 수정하면 User도 함께 수정되어야 한다.
         */
        @Test
        public void whenUpdateTravelPlan_thenUserUpdated() {
                User user = User.builder()
                                .username("testuser")
                                .password("password")
                                .email("test@example.com")
                                .build();

                TravelPlan travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .build();

                user.addTravelPlan(travelPlan);
                userRepository.save(user);

                travelPlan.setTitle("Updated Travel Plan");
                travelPlanRepository.save(travelPlan);

                User updatedUser = userRepository.findById(user.getId()).orElse(null);
                assertThat(updatedUser).isNotNull();
                assertThat(updatedUser.getTravelPlans().get(0).getTitle()).isEqualTo("Updated Travel Plan");
        }

}
