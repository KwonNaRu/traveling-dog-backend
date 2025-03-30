package com.travelingdog.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@Tag("unit")
public class ItineraryUnitTest {

        private TravelPlan travelPlan;
        private Itinerary itinerary;
        private Validator validator;
        private GeometryFactory geometryFactory;

        @BeforeEach
        public void setup() {
                Locale.setDefault(Locale.ENGLISH);

                ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                validator = factory.getValidator();

                // TravelPlan 생성 (필수 필드만 가장 기본적으로 초기화)
                travelPlan = TravelPlan.builder()
                                .title("강아지와 함께하는 제주도 여행")
                                .startDate(LocalDate.now().plusDays(1))
                                .endDate(LocalDate.now().plusDays(5))
                                .user(User.builder()
                                                .nickname("dogLover")
                                                .password("password123")
                                                .email("dogLover@example.com")
                                                .build())
                                .build();

                // GeometryFactory 초기화
                geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

                // Itinerary 초기화
                itinerary = Itinerary.builder()
                                .date(1)
                                .location("제주시")
                                .travelPlan(travelPlan)
                                .build();
        }

        /**
         * ✅ Itinerary 엔티티가 정상적으로 생성되는지 검증합니다.
         */
        @Test
        public void testItineraryCreation() {
                assertThat(itinerary).isNotNull();
                assertThat(itinerary.getDate()).isEqualTo(1);
                assertThat(itinerary.getLocation()).isEqualTo("제주시");
                assertThat(itinerary.getTravelPlan()).isEqualTo(travelPlan);
                assertThat(itinerary.getActivities()).isEmpty();
                assertThat(itinerary.getLunch()).isNull();
                assertThat(itinerary.getDinner()).isNull();
        }

        /**
         * ✅ Itinerary 엔티티가 정상적으로 수정되는지 검증합니다.
         */
        @Test
        public void testItineraryUpdate() {
                itinerary.setDate(2);
                itinerary.setLocation("서귀포시");
                itinerary.setTravelPlan(travelPlan);

                assertThat(itinerary.getDate()).isEqualTo(2);
                assertThat(itinerary.getLocation()).isEqualTo("서귀포시");
        }

        /**
         * ✅ Itinerary 엔티티에 점심/저녁 장소가 정상적으로 추가되는지 검증합니다.
         */
        @Test
        public void testItineraryMealLocations() {
                // 점심 장소 추가
                ItineraryLunch lunch = ItineraryLunch.builder()
                                .name("제주 흑돼지 맛집")
                                .description("제주 전통 흑돼지 구이 맛집")
                                // .coordinates(geometryFactory.createPoint(new Coordinate(126.531, 33.499)))
                                .itinerary(itinerary)
                                .build();
                itinerary.setLunch(lunch);

                // 저녁 장소 추가
                ItineraryDinner dinner = ItineraryDinner.builder()
                                .name("해녀의 집")
                                .description("신선한 해산물 요리")
                                // .coordinates(geometryFactory.createPoint(new Coordinate(126.559, 33.248)))
                                .itinerary(itinerary)
                                .build();
                itinerary.setDinner(dinner);

                assertThat(itinerary.getLunch()).isEqualTo(lunch);
                assertThat(itinerary.getDinner()).isEqualTo(dinner);
        }

        /**
         * ✅ Itinerary 엔티티에 활동이 정상적으로 추가되는지 검증합니다.
         */
        @Test
        public void testItineraryActivities() {
                ItineraryActivity activity = ItineraryActivity.builder()
                                .name("성산일출봉")
                                .description("제주도의 상징적인 화산")
                                // .coordinates(geometryFactory.createPoint(new Coordinate(126.939, 33.458)))
                                .activityOrder(0)
                                .itinerary(itinerary)
                                .build();
                itinerary.getActivities().add(activity);

                assertThat(itinerary.getActivities()).hasSize(1);
                assertThat(itinerary.getActivities().get(0)).isEqualTo(activity);
        }

        /**
         * ✅ day 필드가 null이면 검증 실패해야 합니다.
         */
        @Test
        public void testDateIsNull() {
                Itinerary invalidItinerary = Itinerary.builder()
                                .location("제주시")
                                .travelPlan(travelPlan)
                                .build();

                Set<ConstraintViolation<Itinerary>> violations = validator.validate(invalidItinerary);
                assertThat(violations)
                                .extracting(ConstraintViolation::getMessage)
                                .contains("must not be null");
        }

        /**
         * ✅ location 필드가 null이면 검증 실패해야 합니다.
         */
        @Test
        public void testLocationIsNull() {
                Itinerary invalidItinerary = Itinerary.builder()
                                .date(1)
                                .travelPlan(travelPlan)
                                .build();

                Set<ConstraintViolation<Itinerary>> violations = validator.validate(invalidItinerary);
                assertThat(violations)
                                .extracting(ConstraintViolation::getMessage)
                                .contains("must not be null");
        }

        @Test
        @Tag("unit")
        public void testItineraryCreation_success() {
                Itinerary validItinerary = Itinerary.builder()
                                .date(1)
                                .location("제주시")
                                .travelPlan(travelPlan)
                                .build();

                Set<ConstraintViolation<Itinerary>> violations = validator.validate(validItinerary);
                assertThat(violations).isEmpty();
        }
}
