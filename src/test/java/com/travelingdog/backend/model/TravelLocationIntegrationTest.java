package com.travelingdog.backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.config.JpaAuditingConfigTest;
import com.travelingdog.backend.repository.TravelLocationRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
@Import(JpaAuditingConfigTest.class)
public class TravelLocationIntegrationTest {

        @Autowired
        private TravelLocationRepository travelLocationRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TravelPlanRepository travelPlanRepository;

        private User user;
        private TravelPlan travelPlan;

        @BeforeEach
        public void setUp() {
                user = User.builder()
                                .nickname("testuser")
                                .password("password")
                                .email("test@example.com")
                                .preferredTravelStyle("Adventure")
                                .favoriteDestinations(List.of("Paris", "New York"))
                                .build();
                userRepository.save(user);

                travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .user(user)
                                .build();
                travelPlanRepository.save(travelPlan);
        }

        /*
         * 여행 계획에 여행 위치가 추가되면 여행 계획의 여행 위치 리스트에 추가된다.
         */
        @Test
        public void testAddTravelLocationToTravelPlan() {
                TravelLocation travelLocation = TravelLocation.builder()
                                .placeName("Test Travel Location")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Test Description")
                                .locationOrder(1)
                                .build();

                travelPlan.addTravelLocation(travelLocation);
                travelPlanRepository.save(travelPlan);

                TravelLocation addedTravelLocation = travelPlanRepository.findById(travelPlan.getId()).orElse(null)
                                .getTravelLocations().get(0);

                assertNotNull(addedTravelLocation);
                assertEquals(addedTravelLocation.getTravelPlan(), travelPlan);
                assertEquals(travelPlan.getTravelLocations().get(0), addedTravelLocation);
        }

        /*
         * 여행 계획이 삭제되면 여행 계획의 여행 위치 리스트도 삭제된다.
         */
        @Test
        public void testDeleteTravelPlan() {
                TravelLocation travelLocation = TravelLocation.builder()
                                .placeName("Test Travel Location")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Test Description")
                                .locationOrder(1)
                                .build();

                travelPlan.addTravelLocation(travelLocation);
                travelPlanRepository.save(travelPlan);

                TravelLocation addedTravelLocation = travelPlanRepository.findById(travelPlan.getId()).orElse(null)
                                .getTravelLocations().get(0);
                assertEquals(1, travelPlan.getTravelLocations().size());
                assertEquals(addedTravelLocation.getTravelPlan(), travelPlan);

                travelPlanRepository.delete(travelPlan);

                assertNull(travelPlanRepository.findById(travelPlan.getId()).orElse(null));
                assertEquals(0, travelLocationRepository.findByTravelPlanId(travelPlan.getId()).size());
        }

        /*
         * 모든 여행 위치는 하나의 여행 계획에 속해야 하며, 독립적으로 존재할 수 없다.
         */
        @Test
        public void testTravelLocationBelongsToTravelPlan() {
                TravelLocation travelLocation = TravelLocation.builder()
                                .placeName("Test Travel Location")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Test Description")
                                .locationOrder(1)
                                .build();

                travelPlan.addTravelLocation(travelLocation);
                travelPlanRepository.save(travelPlan);

                assertEquals(travelPlan.getId(), travelLocation.getTravelPlan().getId());
        }

        // TODO:
        /*
         * 사용자의 선호도를 기반으로 여행 위치를 추천한다.
         */

}
