package com.travelingdog.backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
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
public class ItineraryIntegrationTest {

        @Autowired
        private ItineraryRepository itineraryRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TravelPlanRepository travelPlanRepository;

        @PersistenceContext
        private EntityManager entityManager;

        private User user;
        private TravelPlan travelPlan;
        private Itinerary itinerary;
        private GeometryFactory geometryFactory;

        @BeforeEach
        public void setUp() {
                geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

                user = User.builder()
                                .nickname("dogLover")
                                .password("password123")
                                .email("dogLover@example.com")
                                .preferredTravelStyle("Adventure")
                                .favoriteDestinations(List.of("제주도", "강원도"))
                                .build();
                userRepository.save(user);

                travelPlan = TravelPlan.builder()
                                .title("강아지와 함께하는 제주도 여행")
                                .country("Korea")
                                .city("제주시")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(5))
                                .status(PlanStatus.PRIVATE)
                                .user(user)
                                .build();
                travelPlanRepository.save(travelPlan);
        }

        /**
         * 여행 계획에 일정이 추가되면 여행 계획의 일정 리스트에 추가된다.
         */
        @Test
        public void testAddItineraryToTravelPlan() {
                // given
                Itinerary itinerary = Itinerary.builder()
                                .date("2024-05-10")
                                .location("성산일출봉")
                                .travelPlan(travelPlan)
                                .build();

                // when
                Itinerary savedItinerary = itineraryRepository.save(itinerary);
                List<Itinerary> itineraries = itineraryRepository
                                .findAllByTravelPlanIdOrderByDateAsc(travelPlan.getId());

                // then
                assertNotNull(savedItinerary);
                assertNotNull(itineraries);
                assertEquals(1, itineraries.size());
                assertEquals(itinerary.getLocation(), itineraries.get(0).getLocation());
                assertEquals(itinerary.getDate(), itineraries.get(0).getDate());
                assertEquals(travelPlan.getId(), itinerary.getTravelPlan().getId());
        }

        /**
         * 여행 계획이 삭제되면 여행 계획의 일정 리스트도 삭제된다.
         */
        @Test
        @Transactional
        public void testDeleteTravelPlan() {
                // given
                Itinerary itinerary = Itinerary.builder()
                                .date("2024-05-10")
                                .location("성산일출봉")
                                .travelPlan(travelPlan)
                                .build();
                itineraryRepository.save(itinerary);

                // 영속성 컨텍스트 초기화
                entityManager.flush();
                entityManager.clear();

                // when
                // TravelPlan 다시 로드
                TravelPlan reloadedTravelPlan = travelPlanRepository.findById(travelPlan.getId()).orElse(null);
                List<Itinerary> beforeDelete = itineraryRepository
                                .findAllByTravelPlanIdOrderByDateAsc(reloadedTravelPlan.getId());

                // TravelPlan에서 Itinerary 연관관계 제거
                reloadedTravelPlan.getItineraries().clear();
                travelPlanRepository.save(reloadedTravelPlan);
                entityManager.flush();

                // TravelPlan 삭제
                travelPlanRepository.delete(reloadedTravelPlan);
                entityManager.flush();

                List<Itinerary> afterDelete = itineraryRepository
                                .findAllByTravelPlanIdOrderByDateAsc(travelPlan.getId());

                // then
                assertEquals(1, beforeDelete.size());
                assertEquals(itinerary.getLocation(), beforeDelete.get(0).getLocation());
                assertNull(travelPlanRepository.findById(travelPlan.getId()).orElse(null));
                assertTrue(afterDelete.isEmpty());
        }

        /**
         * 모든 일정은 하나의 여행 계획에 속해야 하며, 독립적으로 존재할 수 없다.
         */
        @Test
        public void testItineraryBelongsToTravelPlan() {
                // given
                Itinerary itinerary1 = Itinerary.builder()
                                .date("2024-05-10")
                                .location("성산일출봉")
                                .travelPlan(travelPlan)
                                .build();

                Itinerary itinerary2 = Itinerary.builder()
                                .date("2024-05-11")
                                .location("만장굴")
                                .travelPlan(travelPlan)
                                .build();

                // when
                itineraryRepository.saveAll(List.of(itinerary1, itinerary2));
                List<Itinerary> itineraries = itineraryRepository
                                .findAllByTravelPlanIdOrderByDateAsc(travelPlan.getId());

                // then
                assertEquals(2, itineraries.size());
                assertTrue(itineraries.stream()
                                .allMatch(itinerary -> itinerary.getTravelPlan().getId().equals(travelPlan.getId())));
                assertEquals("2024-05-10", itineraries.get(0).getDate());
                assertEquals("2024-05-11", itineraries.get(1).getDate());
        }

        /**
         * 일정에 활동을 추가할 수 있다.
         */
        @Test
        public void testAddActivitiesToItinerary() {
                // given
                Itinerary itinerary = Itinerary.builder()
                                .date("2024-05-10")
                                .location("성산일출봉")
                                .travelPlan(travelPlan)
                                .build();
                itineraryRepository.save(itinerary);

                ItineraryActivity activity1 = ItineraryActivity.builder()
                                .title("성산일출봉 등반")
                                .description("제주도의 상징적인 화산 등반")
                                .locationName("성산일출봉")
                                .itinerary(itinerary)
                                .build();

                ItineraryActivity activity2 = ItineraryActivity.builder()
                                .title("우도 자전거 투어")
                                .description("우도 섬 자전거 투어")
                                .locationName("우도")
                                .itinerary(itinerary)
                                .build();

                // when
                itinerary.getActivities().add(activity1);
                itinerary.getActivities().add(activity2);
                Itinerary savedItinerary = itineraryRepository.save(itinerary);

                // then
                assertNotNull(savedItinerary);
                assertEquals(2, savedItinerary.getActivities().size());
                assertEquals("성산일출봉 등반", savedItinerary.getActivities().get(0).getTitle());
                assertEquals("우도 자전거 투어", savedItinerary.getActivities().get(1).getTitle());
        }
}
