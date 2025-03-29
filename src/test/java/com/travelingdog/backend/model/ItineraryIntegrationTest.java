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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.config.JpaAuditingConfigTest;
import com.travelingdog.backend.repository.ItineraryRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.status.PlanStatus;

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
                                .country("한국")
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
                                .day(1)
                                .location("성산일출봉")
                                .travelPlan(travelPlan)
                                .build();

                // when
                Itinerary savedItinerary = itineraryRepository.save(itinerary);
                List<Itinerary> itineraries = itineraryRepository
                                .findAllByTravelPlanIdOrderByDayAsc(travelPlan.getId());

                // then
                assertNotNull(savedItinerary);
                assertNotNull(itineraries);
                assertEquals(1, itineraries.size());
                assertEquals(itinerary.getLocation(), itineraries.get(0).getLocation());
                assertEquals(itinerary.getDay(), itineraries.get(0).getDay());
                assertEquals(travelPlan.getId(), itinerary.getTravelPlan().getId());
        }

        /**
         * 여행 계획이 삭제되면 여행 계획의 일정 리스트도 삭제된다.
         */
        @Test
        public void testDeleteTravelPlan() {
                // given
                Itinerary itinerary = Itinerary.builder()
                                .day(1)
                                .location("성산일출봉")
                                .travelPlan(travelPlan)
                                .build();
                itineraryRepository.save(itinerary);

                // when
                List<Itinerary> beforeDelete = itineraryRepository
                                .findAllByTravelPlanIdOrderByDayAsc(travelPlan.getId());
                travelPlanRepository.delete(travelPlan);
                List<Itinerary> afterDelete = itineraryRepository
                                .findAllByTravelPlanIdOrderByDayAsc(travelPlan.getId());

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
                                .day(1)
                                .location("성산일출봉")
                                .travelPlan(travelPlan)
                                .build();

                Itinerary itinerary2 = Itinerary.builder()
                                .day(2)
                                .location("만장굴")
                                .travelPlan(travelPlan)
                                .build();

                // when
                itineraryRepository.saveAll(List.of(itinerary1, itinerary2));
                List<Itinerary> itineraries = itineraryRepository
                                .findAllByTravelPlanIdOrderByDayAsc(travelPlan.getId());

                // then
                assertEquals(2, itineraries.size());
                assertTrue(itineraries.stream()
                                .allMatch(itinerary -> itinerary.getTravelPlan().getId().equals(travelPlan.getId())));
                assertEquals(1, itineraries.get(0).getDay());
                assertEquals(2, itineraries.get(1).getDay());
        }

        /**
         * 일정에 활동을 추가할 수 있다.
         */
        @Test
        public void testAddActivitiesToItinerary() {
                // given
                Itinerary itinerary = Itinerary.builder()
                                .day(1)
                                .location("성산일출봉")
                                .travelPlan(travelPlan)
                                .build();
                itineraryRepository.save(itinerary);

                ItineraryActivity activity1 = ItineraryActivity.builder()
                                .name("성산일출봉 등반")
                                .description("제주도의 상징적인 화산 등반")
                                // .coordinates(geometryFactory.createPoint(new Coordinate(126.939, 33.458)))
                                .activityOrder(0)
                                .itinerary(itinerary)
                                .build();

                ItineraryActivity activity2 = ItineraryActivity.builder()
                                .name("우도 자전거 투어")
                                .description("우도 섬 자전거 투어")
                                // .coordinates(geometryFactory.createPoint(new Coordinate(126.953, 33.506)))
                                .activityOrder(1)
                                .itinerary(itinerary)
                                .build();

                // when
                itinerary.getActivities().add(activity1);
                itinerary.getActivities().add(activity2);
                Itinerary savedItinerary = itineraryRepository.save(itinerary);

                // then
                assertNotNull(savedItinerary);
                assertEquals(2, savedItinerary.getActivities().size());
                assertEquals("성산일출봉 등반", savedItinerary.getActivities().get(0).getName());
                assertEquals("우도 자전거 투어", savedItinerary.getActivities().get(1).getName());
                assertEquals(0, savedItinerary.getActivities().get(0).getActivityOrder());
                assertEquals(1, savedItinerary.getActivities().get(1).getActivityOrder());
        }

        /**
         * 일정에 식사 장소(점심, 저녁)를 설정할 수 있다.
         */
        @Test
        public void testAddMealLocationsToItinerary() {
                // given
                Itinerary itinerary = Itinerary.builder()
                                .day(1)
                                .location("성산일출봉")
                                .travelPlan(travelPlan)
                                .build();
                itineraryRepository.save(itinerary);

                ItineraryLocation lunch = ItineraryLocation.builder()
                                .name("제주 흑돼지 맛집")
                                .description("제주 전통 흑돼지 구이 맛집")
                                // .coordinates(geometryFactory.createPoint(new Coordinate(126.531, 33.499)))
                                .itinerary(itinerary)
                                .build();

                ItineraryLocation dinner = ItineraryLocation.builder()
                                .name("해녀의 집")
                                .description("신선한 해산물 요리")
                                // .coordinates(geometryFactory.createPoint(new Coordinate(126.559, 33.248)))
                                .itinerary(itinerary)
                                .build();

                // when
                itinerary.setLunch(lunch);
                itinerary.setDinner(dinner);
                Itinerary savedItinerary = itineraryRepository.save(itinerary);

                // then
                assertNotNull(savedItinerary);
                assertNotNull(savedItinerary.getLunch());
                assertNotNull(savedItinerary.getDinner());
                assertEquals("제주 흑돼지 맛집", savedItinerary.getLunch().getName());
                assertEquals("해녀의 집", savedItinerary.getDinner().getName());
                // assertEquals(126.531, savedItinerary.getLunch().getCoordinates().getX());
                // assertEquals(33.499, savedItinerary.getLunch().getCoordinates().getY());
                // assertEquals(126.559, savedItinerary.getDinner().getCoordinates().getX());
                // assertEquals(33.248, savedItinerary.getDinner().getCoordinates().getY());
        }
}
