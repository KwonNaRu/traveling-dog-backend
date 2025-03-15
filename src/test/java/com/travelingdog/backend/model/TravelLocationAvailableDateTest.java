package com.travelingdog.backend.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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
import com.travelingdog.backend.status.PlanStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
@Import(JpaAuditingConfigTest.class)
public class TravelLocationAvailableDateTest {

        @Autowired
        private TravelLocationRepository travelLocationRepository;

        @Autowired
        private TravelPlanRepository travelPlanRepository;

        @Autowired
        private UserRepository userRepository;

        private User user;
        private TravelPlan travelPlan;
        private Validator validator;

        @BeforeEach
        public void setUp() {
                ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                validator = factory.getValidator();

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
                                .country("Japan")
                                .city("Osaka")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(7))
                                .user(user)
                                .status(PlanStatus.PUBLISHED)
                                .build();
                travelPlanRepository.save(travelPlan);
        }

        @Test
        public void testAvailableDatePersistence() {
                // Given
                LocalDate testDate = LocalDate.now().plusDays(2);
                TravelLocation travelLocation = TravelLocation.builder()
                                .placeName("Test Travel Location")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Test Description")
                                .locationOrder(1)
                                .availableDate(testDate)
                                .travelPlan(travelPlan)
                                .build();

                // When
                travelLocationRepository.save(travelLocation);
                TravelLocation savedLocation = travelLocationRepository.findById(travelLocation.getId()).orElse(null);

                // Then
                assertNotNull(savedLocation);
                assertNotNull(savedLocation.getAvailableDate());
                assertEquals(testDate, savedLocation.getAvailableDate());
        }

        @Test
        public void testAvailableDateValidation() {
                // Given
                TravelLocation invalidLocation = TravelLocation.builder()
                                .placeName("Test Travel Location")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Test Description")
                                .locationOrder(1)
                                .travelPlan(travelPlan)
                                // availableDate 필드 누락
                                .build();

                // When
                Set<ConstraintViolation<TravelLocation>> violations = validator.validate(invalidLocation);

                // Then
                assertThat(violations).isNotEmpty();
                // 메시지는 로케일에 따라 다를 수 있으므로 메시지 내용 대신 필드 이름으로 검증
                assertThat(violations.stream()
                                .anyMatch(v -> v.getPropertyPath().toString().equals("availableDate")))
                                .isTrue();
        }

        @Test
        public void testAvailableDateUpdate() {
                // Given
                LocalDate initialDate = LocalDate.now();
                TravelLocation travelLocation = TravelLocation.builder()
                                .placeName("Test Travel Location")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Test Description")
                                .locationOrder(1)
                                .availableDate(initialDate)
                                .travelPlan(travelPlan)
                                .build();

                travelLocationRepository.save(travelLocation);

                // When
                LocalDate newDate = LocalDate.now().plusDays(3);
                TravelLocation savedLocation = travelLocationRepository.findById(travelLocation.getId()).orElse(null);
                assertNotNull(savedLocation);
                savedLocation.setAvailableDate(newDate);
                travelLocationRepository.save(savedLocation);

                // Then
                TravelLocation updatedLocation = travelLocationRepository.findById(travelLocation.getId()).orElse(null);
                assertNotNull(updatedLocation);
                assertEquals(newDate, updatedLocation.getAvailableDate());
        }

        @Test
        public void testFindLocationsByAvailableDate() {
                // Given
                LocalDate today = LocalDate.now();
                LocalDate tomorrow = today.plusDays(1);

                TravelLocation location1 = TravelLocation.builder()
                                .placeName("Location Today")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Today's location")
                                .locationOrder(1)
                                .availableDate(today)
                                .travelPlan(travelPlan)
                                .build();

                TravelLocation location2 = TravelLocation.builder()
                                .placeName("Location Tomorrow")
                                .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                                                .createPoint(new Coordinate(123.456, 78.901)))
                                .description("Tomorrow's location")
                                .locationOrder(2)
                                .availableDate(tomorrow)
                                .travelPlan(travelPlan)
                                .build();

                travelLocationRepository.save(location1);
                travelLocationRepository.save(location2);

                // When & Then
                List<TravelLocation> locations = travelLocationRepository.findByTravelPlanId(travelPlan.getId());
                assertThat(locations).hasSize(2);

                // 날짜별로 필터링
                List<TravelLocation> todayLocations = locations.stream()
                                .filter(loc -> loc.getAvailableDate().equals(today))
                                .toList();

                List<TravelLocation> tomorrowLocations = locations.stream()
                                .filter(loc -> loc.getAvailableDate().equals(tomorrow))
                                .toList();

                assertThat(todayLocations).hasSize(1);
                assertThat(todayLocations.get(0).getPlaceName()).isEqualTo("Location Today");

                assertThat(tomorrowLocations).hasSize(1);
                assertThat(tomorrowLocations.get(0).getPlaceName()).isEqualTo("Location Tomorrow");
        }
}