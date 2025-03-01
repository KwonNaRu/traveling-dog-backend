package com.travelingdog.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@Tag("unit")
public class UserUnitTest {

    private User user;
    private static Validator validator;

    /**
     * 각 테스트 실행 전에 기본 User 객체를 생성합니다.
     * - username: testUser
     * - password: securePassword123
     * - email: test@example.com
     * - preferredTravelStyle: Adventure
     */
    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ENGLISH); // 기본 로케일 영어 설정
        user = User.builder()
                .nickname("testUser")
                .password("securePassword123")
                .email("test@example.com")
                .preferredTravelStyle("Adventure")
                .build();

        // Validator 인스턴스 생성 (필수 필드 검증을 위한 설정)
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    /**
     * ✅ User 엔티티가 정상적으로 생성되는지 검증합니다.
     * - 필드 값이 기대한 값과 동일한지 확인합니다.
     * - favoriteDestinations와 travelPlans 리스트는 초기 상태에서 비어 있어야 합니다.
     */
    @Test
    void whenUserIsCreated_thenFieldsAreSetCorrectly() {
        assertThat(user).isNotNull();
        assertThat(user.getNickname()).isEqualTo("testUser");
        assertThat(user.getPassword()).isEqualTo("securePassword123");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPreferredTravelStyle()).isEqualTo("Adventure");
        assertThat(user.getFavoriteDestinations()).isEmpty();
        assertThat(user.getTravelPlans()).isEmpty();
    }

    /**
     * ✅ User 엔티티가 정상적으로 수정되는지 검증합니다.
     * - 필드 값이 기대한 값과 동일한지 확인합니다.
     */
    @Test
    void whenUserIsUpdated_thenFieldsAreUpdatedCorrectly() {
        user.setNickname("updatedUser");
        user.setPassword("updatedPassword123");

        assertThat(user.getNickname()).isEqualTo("updatedUser");
        assertThat(user.getPassword()).isEqualTo("updatedPassword123");
    }

    /**
     * ✅ TravelPlan을 User에 추가하면, 올바르게 연관이 설정되는지 검증합니다.
     * - addTravelPlan()을 호출하면 travelPlans 리스트에 추가되어야 합니다.
     * - travelPlan의 user 필드가 현재 user를 가리켜야 합니다.
     */
    @Test
    void whenAddingTravelPlan_thenTravelPlanIsLinkedToUser() {
        TravelPlan travelPlan = TravelPlan.builder()
                .build();

        user.addTravelPlan(travelPlan);

        assertThat(user.getTravelPlans()).containsExactly(travelPlan);
        assertThat(travelPlan.getUser()).isEqualTo(user);
    }

    /**
     * ✅ TravelPlan을 User에서 제거하면, 올바르게 삭제되는지 검증합니다.
     * - removeTravelPlan()을 호출하면 travelPlans 리스트에서 제거되어야 합니다.
     * - travelPlan의 user 필드는 null로 설정되어야 합니다.
     */
    @Test
    void whenRemovingTravelPlan_thenTravelPlanIsDetachedFromUser() {
        TravelPlan travelPlan = TravelPlan.builder()
                .build();

        user.addTravelPlan(travelPlan);
        user.removeTravelPlan(travelPlan);

        assertThat(user.getTravelPlans()).isEmpty();
        assertThat(travelPlan.getUser()).isNull();
    }

    /**
     * ✅ favoriteDestinations 리스트가 정상적으로 추가되는지 검증합니다.
     * - 리스트에 항목을 추가하면, 해당 항목이 포함되어야 합니다.
     */
    @Test
    void whenAddingFavoriteDestination_thenListContainsNewDestination() {
        user.getFavoriteDestinations().add("Hawaii");

        assertThat(user.getFavoriteDestinations()).containsExactly("Hawaii");
    }

    /*** 🔹 추가된 필수 필드 검증 테스트 🔹 ***/

    /**
     * ✅ username 필드가 null이면 검증 실패해야 합니다.
     * - `@NotNull` 검증이 동작하여 "must not be null" 메시지를 반환해야 합니다.
     */
    @Test
    void whenUsernameIsNull_thenValidationFails() {
        User invalidUser = User.builder()
                .password("securePassword123")
                .email("test@example.com")
                .build();

        var violations = validator.validate(invalidUser);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("must not be null");
    }

    /**
     * ✅ password 필드가 null이면 검증 실패해야 합니다.
     * - `@NotNull` 검증이 동작하여 "must not be null" 메시지를 반환해야 합니다.
     */
    @Test
    void whenPasswordIsNull_thenValidationFails() {
        User invalidUser = User.builder()
                .nickname("testUser")
                .email("test@example.com")
                .build();

        var violations = validator.validate(invalidUser);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("must not be null");
    }

    /**
     * ✅ email 필드가 null이면 검증 실패해야 합니다.
     * - `@NotNull` 검증이 동작하여 "must not be null" 메시지를 반환해야 합니다.
     */
    @Test
    void whenEmailIsNull_thenValidationFails() {
        User invalidUser = User.builder()
                .nickname("testUser")
                .password("securePassword123")
                .build();

        var violations = validator.validate(invalidUser);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("must not be null");
    }

    /**
     * ✅ 잘못된 이메일 형식을 입력하면 검증 실패해야 합니다.
     * - `@Email` 애너테이션이 동작하여 "must be a well-formed email address" 메시지를 반환해야 합니다.
     */
    @Test
    void whenEmailIsInvalid_thenValidationFails() {
        User invalidUser = User.builder()
                .nickname("testUser")
                .password("securePassword123")
                .email("invalid-email")
                .build();

        var violations = validator.validate(invalidUser);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("must be a well-formed email address");
    }

    @Test
    @Tag("unit")
    public void testUserCreation_success() {
        User user = User.builder()
                .nickname("testUser")
                .password("securePassword123")
                .email("test@example.com")
                .preferredTravelStyle("Adventure")
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations).isEmpty();
    }

    @Test
    @Tag("unit")
    public void testUserEmailValidation_failure() {
        User user = User.builder()
                .nickname("testUser")
                .password("securePassword123")
                .email("invalid-email")
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("must be a well-formed email address");
    }
}
