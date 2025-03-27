package com.travelingdog.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.travelingdog.backend.status.PlanStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@EnableJpaAuditing
@Tag("unit")
public class TravelPlanUnitTest {

        private User user;
        private TravelPlan travelPlan;
        private Validator validator;

        @BeforeEach
        void setUp() {
                Locale.setDefault(Locale.ENGLISH); // 기본 로케일 영어 설정

                user = User.builder()
                                .nickname("testUser")
                                .password("password123")
                                .email("test@example.com")
                                .build();

                travelPlan = TravelPlan.builder()
                                .title("여행 계획")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(10))
                                .build();

                user.addTravelPlan(travelPlan);

                // Validator 인스턴스 생성 (필수 필드 검증을 위한 설정)
                try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
                        validator = factory.getValidator();
                }

        }

        /**
         * ✅ 여행 계획 생성 테스트
         * - 여행 계획이 정상적으로 생성되는지 검증합니다.
         */
        @Test
        public void testCreateTravelPlan() {
                // 여행 계획 생성 테스트 코드 작성
                assertThat(travelPlan).isNotNull();
                assertThat(travelPlan.getTitle()).isEqualTo("여행 계획");
                assertThat(travelPlan.getUser()).isEqualTo(user);
                assertThat(travelPlan.getItineraries()).isEmpty();
                assertThat(travelPlan.getLikes()).isEmpty();
                assertThat(travelPlan.getViewCount()).isEqualTo(0);
                assertThat(travelPlan.getStatus()).isEqualTo(PlanStatus.DRAFT);
        }

        /**
         * ✅ 여행 계획 수정 테스트
         * - 여행 계획이 정상적으로 수정되는지 검증합니다.
         */
        @Test
        public void testUpdateTravelPlan() {
                Itinerary itinerary = Itinerary.builder()
                                .day(1)
                                .location("테스트 장소")
                                .travelPlan(travelPlan)
                                .build();

                travelPlan.setTitle("업데이트된 여행 계획");
                travelPlan.setStartDate(LocalDate.now().plusDays(1));
                travelPlan.setEndDate(LocalDate.now().plusDays(11));
                travelPlan.setStatus(PlanStatus.PUBLISHED);

                travelPlan.addItinerary(itinerary);

                assertThat(travelPlan.getTitle()).isEqualTo("업데이트된 여행 계획");
                assertThat(travelPlan.getUser()).isEqualTo(user);
                assertThat(travelPlan.getItineraries()).hasSize(1);
                assertThat(travelPlan.getLikes()).isEmpty();
                assertThat(travelPlan.getViewCount()).isEqualTo(0);
                assertThat(travelPlan.getStatus()).isEqualTo(PlanStatus.PUBLISHED);
        }

        /**
         * ✅ 여행 계획 삭제 테스트
         * - 여행 계획이 정상적으로 삭제되는지 검증합니다.
         */
        @Test
        public void testDeleteTravelPlan() {
                travelPlan.softDelete();

                assertThat(travelPlan.getStatus()).isEqualTo(PlanStatus.DELETED);
                assertThat(travelPlan.getDeletedAt()).isNotNull();
        }

        /**
         * ✅ 여행 계획 조회 테스트
         * - 여행 계획이 정상적으로 조회되는지 검증합니다.
         */
        @Test
        public void testViewTravelPlan() {
                travelPlan.incrementViewCount();

                assertThat(travelPlan.getViewCount()).isEqualTo(1);
        }

        /**
         * ✅ 여행 계획 좋아요 테스트
         * - 여행 계획이 정상적으로 좋아요를 누르는지 검증합니다.
         */
        @Test
        public void testLikeTravelPlan() {
                PlanLike planLike = PlanLike.builder()
                                .build();

                travelPlan.addLike(planLike);

                assertThat(travelPlan.getLikes()).hasSize(1);
        }

        /**
         * ✅ 여행 계획 좋아요 취소 테스트
         * - 여행 계획이 정상적으로 좋아요를 취소하는지 검증합니다.
         */
        @Test
        public void testUnlikeTravelPlan() {
                PlanLike planLike = PlanLike.builder()
                                .build();

                travelPlan.addLike(planLike);

                travelPlan.removeLike(planLike);

                assertThat(travelPlan.getLikes()).isEmpty();
        }

        /*** 🔹 추가된 필수 필드 검증 테스트 🔹 ***/

        /**
         * ✅ title 필드가 null이면 검증 실패해야 합니다.
         * - `@NotNull` 검증이 동작하여 "must not be null" 메시지를 반환해야 합니다.
         */
        @Test
        public void testTitleIsEmpty() {
                TravelPlan invalidTravelPlan = TravelPlan.builder()
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(10))
                                .build();

                var violations = validator.validate(invalidTravelPlan);
                assertThat(violations).extracting(ConstraintViolation::getMessage)
                                .contains("must not be null");
        }

        /**
         * ✅ startDate 필드가 null이면 검증 실패해야 합니다.
         * - `@NotNull` 검증이 동작하여 "must not be null" 메시지를 반환해야 합니다.
         */
        @Test
        public void testStartDateIsEmpty() {
                TravelPlan invalidTravelPlan = TravelPlan.builder()
                                .title("여행 계획")
                                .endDate(LocalDate.now().plusDays(10))
                                .build();

                var violations = validator.validate(invalidTravelPlan);
                assertThat(violations).extracting(ConstraintViolation::getMessage)
                                .contains("must not be null");
        }

        /**
         * ✅ endDate 필드가 null이면 검증 실패해야 합니다.
         * - `@NotNull` 검증이 동작하여 "must not be null" 메시지를 반환해야 합니다.
         */
        @Test
        public void testEndDateIsEmpty() {
                TravelPlan invalidTravelPlan = TravelPlan.builder()
                                .title("여행 계획")
                                .startDate(LocalDate.now())
                                .build();

                var violations = validator.validate(invalidTravelPlan);
                assertThat(violations).extracting(ConstraintViolation::getMessage)
                                .contains("must not be null");
        }

        /**
         * ✅ startDate가 endDate보다 이전인 경우 검증 실패해야 합니다.
         * - `@Future` 검증이 동작하여 "must be a future date" 메시지를 반환해야 합니다.
         */
        @Test
        public void testStartDateBeforeEndDate() {
                TravelPlan invalidTravelPlan = TravelPlan.builder()
                                .title("여행 계획")
                                .startDate(LocalDate.now().plusDays(1))
                                .endDate(LocalDate.now())
                                .build();

                var violations = validator.validate(invalidTravelPlan);
                assertThat(violations).extracting(ConstraintViolation::getMessage)
                                .contains("End date must be in the future");
        }

        /**
         * ✅ startDate가 현재 날짜보다 이전인 경우 검증 실패해야 합니다.
         * - `@FutureOrPresent` 검증이 동작하여 "must be in the present or future" 메시지를 반환해야
         * 합니다.
         */
        @Test
        public void testStartDateBeforeCurrentDate() {
                TravelPlan invalidTravelPlan = TravelPlan.builder()
                                .title("여행 계획")
                                .startDate(LocalDate.now().minusDays(1))
                                .endDate(LocalDate.now().plusDays(10))
                                .build();

                var violations = validator.validate(invalidTravelPlan);
                assertThat(violations).extracting(ConstraintViolation::getMessage)
                                .contains("Start date must be in the present or future");
        }

        /**
         * ✅ endDate가 미래가 아닐 경우 검증 실패해야 합니다.
         * - `@Future` 검증이 동작하여 "must be a future date" 메시지를 반환해야 합니다.
         */
        @Test
        public void testEndDateNotFuture() {
                TravelPlan invalidTravelPlan = TravelPlan.builder()
                                .title("여행 계획")
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now())
                                .build();

                var violations = validator.validate(invalidTravelPlan);
                assertThat(violations).extracting(ConstraintViolation::getMessage)
                                .contains("End date must be in the future");

        }

        /**
         * ✅ user 필드가 null이면 검증을 통과할 수 없음.
         * - `user_id` 컬럼은 nullable=false로 설정되어 있습니다.
         */
        @Test
        public void testUserIsNull() {
                TravelPlan invalidTravelPlan = TravelPlan.builder()
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusDays(10))
                                .build();

                var violations = validator.validate(invalidTravelPlan);
                assertThat(violations).extracting(ConstraintViolation::getMessage)
                                .contains("must not be null");
        }
}
