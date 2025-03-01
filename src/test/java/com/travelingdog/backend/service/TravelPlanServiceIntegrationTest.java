package com.travelingdog.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.config.JpaAuditingConfigTest;
import com.travelingdog.backend.config.TestConfig;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
@Import({ JpaAuditingConfigTest.class, TestConfig.class })
public class TravelPlanServiceIntegrationTest {

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private TravelPlanRepository travelPlanRepository;

        private User user;
        private TravelPlan travelPlan;

        @BeforeEach
        public void setUp() {
                user = User.builder()
                                .nickname("testUser")
                                .password("password")
                                .email("test@example.com")
                                .build();
                travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .user(user)
                                .build();
                user.addTravelPlan(travelPlan);
                userRepository.save(user);
                travelPlanRepository.save(travelPlan);
        }

        /*
         * 여행 계획의 여행 위치 리스트에서 여행 위치의 순서를 변경할 수 있다.
         * 다만, 하나의 여행 계획에서 같은 순서를 가지는 여행 위치는 존재할 수 없다.
         * 그렇기 때문에, 여행위치의 순서를 바꿨을 때 같은 순서를 가지는 여행 위치가 존재하면 이전 순서를 가지는 여행 위치의 순서를 1
         * 증가시킨다.
         */
        @Test
        public void testChangeTravelLocationOrder() {
                TravelLocation travelLocation1 = TravelLocation.builder()
                                .placeName("Test Travel Location 1")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Test Description 1")
                                .locationOrder(1)
                                .availableDate(LocalDate.now())
                                .build();
                travelPlan.addTravelLocation(travelLocation1);

                TravelLocation travelLocation2 = TravelLocation.builder()
                                .placeName("Test Travel Location 2")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Test Description 2")
                                .locationOrder(2)
                                .availableDate(LocalDate.now())
                                .build();
                travelPlan.addTravelLocation(travelLocation2);

                travelPlanRepository.save(travelPlan);
        }

        @Test
        public void testUpdateTravelPlan() {
                travelPlan.setTitle("Updated Travel Plan");
                travelPlanRepository.save(travelPlan);

                TravelPlan updatedPlan = travelPlanRepository.findById(travelPlan.getId()).orElse(null);
                assertThat(updatedPlan.getTitle()).isEqualTo("Updated Travel Plan");
        }

}
