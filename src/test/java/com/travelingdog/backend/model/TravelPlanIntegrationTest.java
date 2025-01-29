package com.travelingdog.backend.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.repository.TravelLocationRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
public class TravelPlanIntegrationTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TravelPlanRepository travelPlanRepository;

        @Autowired
        private TravelLocationRepository travelLocationRepository;

        private User user;

        @BeforeEach
        public void setUp() {
                user = User.builder()
                                .username("testuser")
                                .password("password")
                                .email("test@example.com")
                                .preferredTravelStyle("Adventure")
                                .favoriteDestinations(List.of("Paris", "New York"))
                                .build();

                userRepository.save(user);
        }

        /*
         * 유저가 여행 계획을 생성하면 여행 계획이 생성되고 유저의 여행 계획 리스트에 추가된다.
         */
        @Test
        public void whenSaveTravelPlan_thenFindById() {
                TravelPlan travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .user(user)
                                .build();

                travelPlanRepository.save(travelPlan);

                TravelPlan foundTravelPlan = travelPlanRepository.findById(travelPlan.getId()).orElse(null);
                assertThat(foundTravelPlan).isNotNull();
                assertThat(foundTravelPlan.getTitle()).isEqualTo("Test Travel Plan");
        }

        /*
         * 여행 계획을 수정하면 여행 계획의 수정 시간이 업데이트된다.
         */
        @Test
        public void whenUpdateTravelPlan_thenUpdatedAtChanges() throws InterruptedException {
                TravelPlan travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .user(user)
                                .build();

                travelPlanRepository.save(travelPlan);
                LocalDateTime initialUpdatedAt = travelPlan.getUpdatedAt();

                // 충분한 시간 차이를 주기 위해 대기
                Thread.sleep(100);

                travelPlan.setTitle("Updated Travel Plan");
                travelPlanRepository.saveAndFlush(travelPlan);

                TravelPlan updatedTravelPlan = travelPlanRepository.findById(travelPlan.getId()).orElse(null);
                assertThat(updatedTravelPlan).isNotNull();
                assertThat(updatedTravelPlan.getTitle()).isEqualTo("Updated Travel Plan");
                assertThat(updatedTravelPlan.getUpdatedAt()).isAfter(initialUpdatedAt);
        }

        /*
         * 유저가 여행 계획을 삭제하면 여행 계획이 삭제되고 유저의 여행 계획 리스트에서 제거된다.
         */
        @Test
        public void whenDeleteTravelPlan_thenTravelPlanDeletedAndUserTravelPlansListUpdated() {

                TravelPlan travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .user(user)
                                .build();

                travelPlanRepository.save(travelPlan);

                travelPlanRepository.delete(travelPlan);

                User updatedUser = userRepository.findById(user.getId()).orElse(null);
                assertThat(updatedUser).isNotNull();
                assertThat(updatedUser.getTravelPlans()).isEmpty();
        }

        /*
         * 여행 위치가 삭제되면 여행 계획의 여행 위치 리스트에서 제거된다.
         */
        @Test
        public void testDeleteTravelLocation() {
                TravelLocation travelLocation = TravelLocation.builder()
                                .placeName("Test Travel Location")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Test Description")
                                .locationOrder(1)
                                .build();

                TravelPlan travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .user(user)
                                .build();

                travelPlan.addTravelLocation(travelLocation);
                travelPlanRepository.save(travelPlan);

                TravelLocation addedTravelLocation = travelPlanRepository.findById(travelPlan.getId()).orElse(null)
                                .getTravelLocations().get(0);
                assertEquals(1, travelPlan.getTravelLocations().size());
                assertEquals(addedTravelLocation.getTravelPlan(), travelPlan);

                travelLocationRepository.delete(addedTravelLocation);
                travelPlan.removeTravelLocation(addedTravelLocation);
                travelPlanRepository.save(travelPlan);

                assertNull(travelLocationRepository.findById(addedTravelLocation.getId()).orElse(null));
                assertEquals(0, travelPlanRepository.findById(travelPlan.getId()).orElse(null).getTravelLocations()
                                .size());
        }

        /*
         * 여행 위치가 업데이트되면 여행 계획의 여행 위치 리스트도 업데이트된다.
         */
        @Test
        public void testUpdateTravelLocation() {
                TravelLocation travelLocation = TravelLocation.builder()
                                .placeName("Test Travel Location")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Test Description")
                                .locationOrder(1)
                                .build();

                TravelPlan travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .user(user)
                                .build();

                travelPlan.addTravelLocation(travelLocation);
                travelPlanRepository.save(travelPlan);

                TravelLocation updatedTravelLocation = travelPlanRepository.findById(travelPlan.getId()).orElse(null)
                                .getTravelLocations().get(0);
                updatedTravelLocation.setPlaceName("Updated Travel Location");
                travelLocationRepository.save(updatedTravelLocation);

                assertEquals("Updated Travel Location", travelPlanRepository.findById(travelPlan.getId()).orElse(null)
                                .getTravelLocations().get(0).getPlaceName());

        }
}
