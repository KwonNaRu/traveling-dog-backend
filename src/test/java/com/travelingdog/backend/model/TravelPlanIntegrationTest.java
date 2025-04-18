package com.travelingdog.backend.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.travelingdog.backend.config.JpaAuditingConfigTest;
import com.travelingdog.backend.repository.ItineraryRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.status.PlanStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
@Import(JpaAuditingConfigTest.class)
public class TravelPlanIntegrationTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TravelPlanRepository travelPlanRepository;

        @Autowired
        private ItineraryRepository itineraryRepository;

        @PersistenceContext
        private EntityManager entityManager;

        private User user;
        private GeometryFactory geometryFactory;

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

                geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        }

        /*
         * 유저가 여행 계획을 생성하면 여행 계획이 생성되고 유저의 여행 계획 리스트에 추가된다.
         */
        @Test
        public void whenSaveTravelPlan_thenFindById() {
                TravelPlan travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .country("Korea")
                                .city("Seoul")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .status(PlanStatus.PRIVATE)
                                .build();

                travelPlanRepository.save(travelPlan);
                user.addTravelPlan(travelPlan);

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
                                .country("Korea")
                                .city("Seoul")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .status(PlanStatus.PRIVATE)
                                .build();

                user.addTravelPlan(travelPlan);

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
                                .country("Korea")
                                .city("Seoul")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .status(PlanStatus.PRIVATE)
                                .build();

                travelPlanRepository.save(travelPlan);

                travelPlanRepository.delete(travelPlan);

                User updatedUser = userRepository.findById(user.getId()).orElse(null);
                assertThat(updatedUser).isNotNull();
                assertThat(updatedUser.getTravelPlans()).isEmpty();
        }

        /*
         * 일정이 삭제되면 여행 계획의 일정 리스트에서 제거된다.
         */
        @Test
        public void testDeleteItinerary() {
                // 여행 계획 생성
                TravelPlan travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .country("Korea")
                                .city("Seoul")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .status(PlanStatus.PRIVATE)
                                .build();

                travelPlanRepository.save(travelPlan);

                ArrayList<ItineraryActivity> activities = new ArrayList<>();
                ItineraryActivity activity = ItineraryActivity.builder()
                                .title("Test Activity")
                                .description("Test Description")
                                .locationName("Test Location Name")
                                .build();
                activities.add(activity);

                // 일정 생성
                Itinerary itinerary = Itinerary.builder()
                                .date("2024-05-10")
                                .location("Test Location")
                                .travelPlan(travelPlan)
                                .build();

                itineraryRepository.save(itinerary);

                // 일정이 여행 계획에 추가되었는지 확인
                List<Itinerary> itineraries = itineraryRepository
                                .findAllByTravelPlanIdOrderByDateAsc(travelPlan.getId());
                assertEquals(1, itineraries.size());
                assertEquals(itinerary.getTravelPlan(), travelPlan);

                // 일정 삭제
                itineraryRepository.delete(itinerary);

                // 일정이 삭제되었는지 확인
                assertNull(itineraryRepository.findById(itinerary.getId()).orElse(null));
                assertEquals(0, itineraryRepository.findAllByTravelPlanIdOrderByDateAsc(travelPlan.getId())
                                .size());
        }

        /*
         * 일정이 업데이트되면 여행 계획의 일정 리스트도 업데이트된다.
         */
        @Test
        @Transactional
        public void testUpdateItinerary() {
                // 여행 계획 생성
                TravelPlan travelPlan = TravelPlan.builder()
                                .title("Test Travel Plan")
                                .country("Korea")
                                .city("Seoul")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .status(PlanStatus.PRIVATE)
                                .build();

                travelPlanRepository.save(travelPlan);

                // 일정 생성
                Itinerary itinerary = Itinerary.builder()
                                .date("2024-05-10")
                                .location("Test Location")
                                .travelPlan(travelPlan)
                                .build();

                ItineraryActivity activity = ItineraryActivity.builder()
                                .title("Test Activity")
                                .description("Test Description")
                                .locationName("Test Location Name")
                                .itinerary(itinerary)
                                .build();

                // List.of()는 불변 컬렉션을 생성하므로 ArrayList로 대체
                List<ItineraryActivity> activities = new ArrayList<>();
                activities.add(activity);
                itinerary.setActivities(activities);

                itineraryRepository.save(itinerary);

                // 영속성 컨텍스트 초기화
                entityManager.flush();
                entityManager.clear();

                // 일정 업데이트
                List<Itinerary> itineraries = itineraryRepository
                                .findAllByTravelPlanIdOrderByDateAsc(travelPlan.getId());
                Itinerary updatedItinerary = itineraries.get(0);
                updatedItinerary.setLocation("Updated Location");
                itineraryRepository.save(updatedItinerary);

                // 업데이트된 일정 확인
                assertEquals("Updated Location",
                                itineraryRepository.findAllByTravelPlanIdOrderByDateAsc(travelPlan.getId())
                                                .get(0).getLocation());
        }
}
