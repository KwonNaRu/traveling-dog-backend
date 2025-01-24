package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelLocationRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;

@DataJpaTest
public class TravelPlanServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Autowired
    private TravelLocationRepository travelLocationRepository;

    private User user;
    private TravelPlan travelPlan;

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

        travelPlan = TravelPlan.builder()
                .title("Test Travel Plan")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .user(user)
                .build();

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
                .latitude(123.456)
                .longitude(78.901)
                .description("Test Description 1")
                .locationOrder(1)
                .build();
        travelPlan.addTravelLocation(travelLocation1);

        TravelLocation travelLocation2 = TravelLocation.builder()
                .placeName("Test Travel Location 2")
                .latitude(123.456)
                .longitude(78.901)
                .description("Test Description 2")
                .locationOrder(2)
                .build();
        travelPlan.addTravelLocation(travelLocation2);

        travelPlanRepository.save(travelPlan);

        // TODO:
    }

}
