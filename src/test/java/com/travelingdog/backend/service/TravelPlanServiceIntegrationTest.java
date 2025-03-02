package com.travelingdog.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.travelingdog.backend.config.JpaAuditingConfigTest;
import com.travelingdog.backend.config.TestConfig;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelLocationRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;

/**
 * TravelPlanService 통합 테스트
 * 
 * 이 테스트 클래스는 TravelPlanService의 기능을 실제 데이터베이스와 연동하여 통합 테스트합니다.
 * 주요 테스트 대상:
 * 1. 여행 계획 업데이트 기능
 * 2. 여행 장소 순서 변경 기능
 * 
 * 이 테스트는 실제 데이터베이스를 사용하여 TravelPlanService의 전체 기능을
 * 다른 컴포넌트들과의 상호작용을 포함하여 테스트합니다.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Tag("integration")
@Import({ JpaAuditingConfigTest.class, TestConfig.class })
public class TravelPlanServiceIntegrationTest {

        @Autowired
        private TravelPlanService travelPlanService;

        @Autowired
        private TravelPlanRepository travelPlanRepository;

        @Autowired
        private TravelLocationRepository travelLocationRepository;

        @Autowired
        private UserRepository userRepository;

        private User user;
        private TravelPlan travelPlan;
        private List<TravelLocation> travelLocations;

        /**
         * 각 테스트 실행 전 환경 설정
         * 
         * 1. 테스트용 사용자 생성 및 저장
         * 2. 테스트용 여행 계획 생성 및 저장
         * 3. 테스트용 여행 장소 생성 및 저장
         * 
         * 이 설정을 통해 각 테스트가 독립적인 환경에서 실행될 수 있도록 합니다.
         * 
         * @Transactional 어노테이션으로 인해 각 테스트 후 데이터베이스 변경사항은 롤백됩니다.
         */
        @BeforeEach
        void setUp() {
                // 테스트용 사용자 생성
                user = User.builder()
                                .nickname("testUser")
                                .password("password")
                                .email("test@example.com")
                                .build();
                userRepository.save(user);

                // 테스트용 여행 계획 생성
                travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .user(user)
                                .build();
                travelPlanRepository.save(travelPlan);

                // 테스트용 여행 장소 생성
                travelLocations = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                        TravelLocation location = TravelLocation.builder()
                                        .placeName("Location " + (i + 1))
                                        .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                        .createPoint(new Coordinate(37.5 + i * 0.1, 127.0 + i * 0.1)))
                                        .locationOrder(i + 1)
                                        .travelPlan(travelPlan)
                                        .build();
                        travelLocationRepository.save(location);
                        travelLocations.add(location);
                }
        }

        /**
         * 여행 장소 순서 변경 테스트
         * 
         * 이 테스트는 TravelPlanService가 여행 장소의 순서를 올바르게 변경하는지 검증합니다.
         * 
         * 테스트 과정:
         * 1. 여행 장소 순서 변경 요청 생성 (장소 ID와 새 순서)
         * 2. TravelPlanService를 사용하여 순서 변경
         * 3. 결과 검증: 변경된 순서가 데이터베이스에 올바르게 반영되었는지 확인
         * 
         * 이 테스트는 또한 중복된 순서가 있을 경우 DuplicateOrderException이 발생하는지도 검증합니다.
         */
        // @Test
        // @DisplayName("여행 장소 순서 변경 테스트")
        // void testChangeTravelLocationOrder() {
        // // 장소 순서 변경 로직:
        // // 1. 장소 ID와 새 순서를 입력받음
        // // 2. 해당 장소의 순서를 변경
        // // 3. 같은 여행 계획 내에 동일한 순서를 가진 장소가 있으면 안 됨

        // // Given
        // Long locationId = travelLocations.get(0).getId();
        // int newOrder = 3;

        // // When
        // travelPlanService.changeTravelLocationOrder(locationId, newOrder);

        // // Then
        // TravelLocation updatedLocation =
        // travelLocationRepository.findById(locationId).orElseThrow();
        // assertEquals(newOrder, updatedLocation.getLocationOrder());

        // // 중복된 순서가 있을 경우 예외 발생 테스트
        // Long anotherLocationId = travelLocations.get(1).getId();
        // assertThrows(DuplicateOrderException.class, () -> {
        // travelPlanService.changeTravelLocationOrder(anotherLocationId, newOrder);
        // });
        // }

        /**
         * 여행 계획 업데이트 테스트
         * 
         * 이 테스트는 TravelPlanService가 여행 계획의 제목을 올바르게 업데이트하는지 검증합니다.
         * 
         * 테스트 과정:
         * 1. 업데이트할 여행 계획 DTO 생성 (새 제목 포함)
         * 2. TravelPlanService를 사용하여 여행 계획 업데이트
         * 3. 결과 검증: 변경된 제목이 데이터베이스에 올바르게 반영되었는지 확인
         */
        // @Test
        // @DisplayName("여행 계획 업데이트 테스트")
        // void testUpdateTravelPlan() {
        // // Given
        // String newTitle = "Updated Travel Plan";
        // TravelPlanRequest updateRequest = new TravelPlanRequest();
        // updateRequest.setId(travelPlan.getId());
        // updateRequest.setTitle(newTitle);
        // updateRequest.setTravelLocations(new ArrayList<>());

        // // When
        // TravelPlan updatedPlan = travelPlanService.updateTravelPlan(updateRequest);

        // // Then
        // assertEquals(newTitle, updatedPlan.getTitle());
        // TravelPlan savedPlan =
        // travelPlanRepository.findById(travelPlan.getId()).orElseThrow();
        // assertEquals(newTitle, savedPlan.getTitle());
        // }
}
