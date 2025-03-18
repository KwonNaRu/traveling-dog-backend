package com.travelingdog.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@Tag("unit")
public class TravelLocationUnitTest {

        private TravelPlan travelPlan;
        private TravelLocation travelLocation;
        private Validator validator;
        private GeometryFactory geometryFactory;
        private LocalDate testDate;

        @BeforeEach
        public void setup() {
                Locale.setDefault(Locale.ENGLISH);

                ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                validator = factory.getValidator();

                // TravelPlan 생성 (필수 필드만 가장 기본적으로 초기화)
                travelPlan = TravelPlan.builder()
                                .title("여행 계획")
                                .startDate(LocalDate.now().plusDays(1))
                                .endDate(LocalDate.now().plusDays(5))
                                .user(User.builder()
                                                .nickname("testUser")
                                                .password("password123")
                                                .email("test@example.com")
                                                .build())
                                .build();

                // TravelLocation 초기화
                geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
                testDate = LocalDate.now();
                travelLocation = TravelLocation.builder()
                                .placeName("Test Location")
                                .coordinates(geometryFactory.createPoint(new Coordinate(126.977, 37.579)))
                                .description("Test Description")
                                .locationOrder(1)
                                .travelPlan(travelPlan)
                                .build();
        }

        /**
         * ✅ TravelLocation 엔티티가 정상적으로 생성되는지 검증합니다.
         */
        @Test
        public void testTravelLocationCreation() {
                assertThat(travelLocation).isNotNull();
                assertThat(travelLocation.getPlaceName()).isEqualTo("Test Location");
                assertThat(travelLocation.getDescription()).isEqualTo("Test Description");
                assertThat(travelLocation.getCoordinates())
                                .isEqualTo(geometryFactory.createPoint(new Coordinate(126.977, 37.579)));
                assertThat(travelLocation.getLocationOrder()).isEqualTo(1);
                assertThat(travelLocation.getTravelPlan()).isEqualTo(travelPlan);
        }

        /**
         * ✅ TravelLocation 엔티티가 정상적으로 수정되는지 검증합니다.
         */
        @Test
        public void testTravelLocationUpdate() {
                travelLocation.setPlaceName("업데이트된 여행 위치");
                travelLocation.setCoordinates(155.978, 42.5665);

                assertThat(travelLocation.getPlaceName()).isEqualTo("업데이트된 여행 위치");
                assertThat(travelLocation.getCoordinates())
                                .isEqualTo(geometryFactory.createPoint(new Coordinate(155.978, 42.5665)));
        }

        /**
         * ✅ TravelLocation 엔티티가 정상적으로 삭제되는지 검증합니다.
         */
        @Test
        public void testTravelLocationDeletion() {
                travelPlan.removeTravelLocation(travelLocation);
                assertThat(travelPlan.getTravelLocations()).doesNotContain(travelLocation);
        }

        /*** 🔹 추가된 필수 필드 검증 테스트 🔹 ***/

        /**
         * ✅ placeName 필드가 null이면 검증 실패해야 합니다.
         */
        @Test
        public void testPlaceNameIsNull() {
                TravelLocation invalidTravelLocation = TravelLocation.builder()
                                .description("여행 위치 설명")
                                .coordinates(geometryFactory.createPoint(new Coordinate(126.978, 37.5665)))
                                .locationOrder(1)
                                .travelPlan(travelPlan)
                                .build();

                Set<ConstraintViolation<TravelLocation>> violations = validator.validate(invalidTravelLocation);
                assertThat(violations)
                                .extracting(ConstraintViolation::getMessage)
                                .contains("must not be null");
        }

        /**
         * ✅ coordinates 필드가 null이면 검증 실패해야 합니다.
         */
        @Test
        public void testCoordinatesIsNull() {
                TravelLocation invalidTravelLocation = TravelLocation.builder()
                                .placeName("여행 위치")
                                .description("여행 위치 설명")
                                .locationOrder(1)
                                .travelPlan(travelPlan)
                                .build();

                var violations = validator.validate(invalidTravelLocation);
                assertThat(violations).extracting(ConstraintViolation::getMessage)
                                .contains("must not be null");
        }

        /**
         * ✅ locationOrder 필드 미입력 시 0으로 설정되어야 합니다.
         */
        @Test
        public void testLocationOrderIsNull() {
                TravelLocation validTravelLocation = TravelLocation.builder()
                                .placeName("여행 위치")
                                .description("여행 위치 설명")
                                .coordinates(geometryFactory.createPoint(new Coordinate(126.978, 37.5665)))
                                .travelPlan(travelPlan)
                                .build();

                assertThat(validTravelLocation.getLocationOrder()).isEqualTo(0);
        }

        /**
         * ✅ travelPlan 필드가 null이면 검증 실패해야 합니다.
         */
        @Test
        public void testTravelPlanIsNull() {
                TravelLocation invalidTravelLocation = TravelLocation.builder()
                                .placeName("여행 위치")
                                .description("여행 위치 설명")
                                .coordinates(geometryFactory.createPoint(new Coordinate(126.978, 37.5665)))
                                .locationOrder(1)
                                .build();

                var violations = validator.validate(invalidTravelLocation);
                assertThat(violations).extracting(ConstraintViolation::getMessage)
                                .contains("must not be null");
        }

        /**
         * ✅ locationOrder 필드가 0보다 작으면 검증 실패해야 합니다.
         */
        @Test
        public void testLocationOrderIsNegative() {
                TravelLocation invalidTravelLocation = TravelLocation.builder()
                                .placeName("여행 위치")
                                .description("여행 위치 설명")
                                .coordinates(geometryFactory.createPoint(new Coordinate(126.978, 37.5665)))
                                .locationOrder(-1)
                                .travelPlan(travelPlan)
                                .build();

                var violations = validator.validate(invalidTravelLocation);
                assertThat(violations).extracting(ConstraintViolation::getMessage)
                                .contains("Order must be positive number");
        }

        @Test
        @Tag("unit")
        public void testTravelLocationCreation_success() {
                TravelLocation travelLocation = TravelLocation.builder()
                                .placeName("여행 위치")
                                .description("여행 위치 설명")
                                .coordinates(geometryFactory.createPoint(new Coordinate(126.978, 37.5665)))
                                .locationOrder(1)
                                .travelPlan(travelPlan)
                                .build();

                Set<ConstraintViolation<TravelLocation>> violations = validator.validate(travelLocation);
                assertThat(violations).isEmpty();
        }

        @Test
        public void testEqualsAndHashCode() {
                // Given
                TravelLocation location1 = TravelLocation.builder()
                                .id(1L)
                                .placeName("Test Location")
                                .coordinates(geometryFactory.createPoint(new Coordinate(126.977, 37.579)))
                                .description("Test Description")
                                .locationOrder(1)
                                .build();

                TravelLocation location2 = TravelLocation.builder()
                                .id(1L)
                                .placeName("Test Location")
                                .coordinates(geometryFactory.createPoint(new Coordinate(126.977, 37.579)))
                                .description("Test Description")
                                .locationOrder(1)
                                .build();

                // Then
                assertThat(location1).isEqualTo(location2);
                assertThat(location1.hashCode()).isEqualTo(location2.hashCode());
        }

        @Test
        public void testToString() {
                // When
                String toString = travelLocation.toString();

                // Then
                assertThat(toString).isNotNull();
        }
}
