package com.travelingdog.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.config.JpaAuditingConfigTest;
import com.travelingdog.backend.model.Itinerary;
import com.travelingdog.backend.model.ItineraryActivity;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.status.PlanStatus;

@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
@Import(JpaAuditingConfigTest.class)
public class ItineraryActivityRepositoryTest {

    @Autowired
    private ItineraryActivityRepository itineraryActivityRepository;

    @Autowired
    private ItineraryRepository itineraryRepository;

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private TravelPlan travelPlan;
    private Itinerary itinerary;
    private ItineraryActivity activity;

    @BeforeEach
    public void setUp() {
        // 테스트 사용자 생성
        user = User.builder()
                .nickname("테스트사용자")
                .password("password123")
                .email("test@example.com")
                .preferredTravelStyle("Adventure")
                .build();
        userRepository.save(user);

        // 테스트 여행 계획 생성
        travelPlan = TravelPlan.builder()
                .title("제주도 여행")
                .country("Korea")
                .city("제주시")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .status(PlanStatus.PRIVATE)
                .user(user)
                .build();
        travelPlanRepository.save(travelPlan);

        // 테스트 일정 생성
        itinerary = Itinerary.builder()
                .date("2024-05-15")
                .location("성산일출봉")
                .travelPlan(travelPlan)
                .build();
        itineraryRepository.save(itinerary);

        // 테스트 활동 생성 (연관관계 편의 메소드 사용)
        activity = ItineraryActivity.builder()
                .title("성산일출봉 등반")
                .description("제주도의 상징적인 화산 등반")
                .locationName("성산일출봉")
                .build(); // itinerary는 아직 설정하지 않음
        
        // 양방향 연관관계 설정
        itinerary.addActivity(activity);
        
        // 활동 저장
        itineraryActivityRepository.save(activity);
        
        // 일정 업데이트하여 활동 목록 포함
        itineraryRepository.save(itinerary);
    }

    @Test
    @DisplayName("활동 저장 및 조회 테스트")
    public void testSaveAndFindActivity() {
        // Given
        Long activityId = activity.getId();

        // When
        Optional<ItineraryActivity> foundActivity = itineraryActivityRepository.findById(activityId);

        // Then
        assertThat(foundActivity).isPresent();
        assertThat(foundActivity.get().getTitle()).isEqualTo("성산일출봉 등반");
        assertThat(foundActivity.get().getDescription()).isEqualTo("제주도의 상징적인 화산 등반");
        assertThat(foundActivity.get().getLocationName()).isEqualTo("성산일출봉");
        assertThat(foundActivity.get().getItinerary().getId()).isEqualTo(itinerary.getId());
    }

    @Test
    @DisplayName("활동 업데이트 테스트")
    public void testUpdateActivity() {
        // Given
        Long activityId = activity.getId();
        ItineraryActivity activityToUpdate = itineraryActivityRepository.findById(activityId).orElseThrow();

        // When
        activityToUpdate.setTitle("성산일출봉 트레킹");
        activityToUpdate.setDescription("제주도의 상징적인 화산을 트레킹");
        itineraryActivityRepository.save(activityToUpdate);

        // Then
        ItineraryActivity updatedActivity = itineraryActivityRepository.findById(activityId).orElseThrow();
        assertThat(updatedActivity.getTitle()).isEqualTo("성산일출봉 트레킹");
        assertThat(updatedActivity.getDescription()).isEqualTo("제주도의 상징적인 화산을 트레킹");
    }

    @Test
    @DisplayName("활동 삭제 테스트")
    public void testDeleteActivity() {
        // Given
        Long activityId = activity.getId();

        // When
        itineraryActivityRepository.deleteById(activityId);

        // Then
        assertThat(itineraryActivityRepository.findById(activityId)).isEmpty();
    }

    @Test
    @DisplayName("일정에 여러 활동 추가 테스트")
    public void testAddMultipleActivitiesToItinerary() {
        // Given
        ItineraryActivity secondActivity = ItineraryActivity.builder()
                .title("우도 자전거 투어")
                .description("우도 섬 자전거 투어")
                .locationName("우도")
                .build(); // itinerary는 아직 설정하지 않음

        ItineraryActivity thirdActivity = ItineraryActivity.builder()
                .title("해산물 저녁 식사")
                .description("제주 신선한 해산물 식사")
                .locationName("제주 서귀포 해안")
                .build(); // itinerary는 아직 설정하지 않음

        // 양방향 연관관계 설정
        itinerary.addActivity(secondActivity);
        itinerary.addActivity(thirdActivity);

        // When
        itineraryActivityRepository.save(secondActivity);
        itineraryActivityRepository.save(thirdActivity);
        itineraryRepository.save(itinerary); // 일정도 업데이트

        // Then
        List<ItineraryActivity> allActivities = itineraryActivityRepository.findAll();
        assertThat(allActivities).hasSize(3);
        assertThat(allActivities).extracting("title")
                .containsExactlyInAnyOrder("성산일출봉 등반", "우도 자전거 투어", "해산물 저녁 식사");
    }

    @Test
    @DisplayName("일정 삭제 시 활동도 함께 삭제되는지 테스트")
    public void testDeleteItineraryWithActivities() {
        // Given
        Long itineraryId = itinerary.getId();
        Long activityId = activity.getId();
        
        // 일정과 활동의 연관관계가 제대로 설정되었는지 확인
        Itinerary foundItinerary = itineraryRepository.findById(itineraryId).orElseThrow();
        assertThat(foundItinerary.getActivities()).hasSize(1);
        assertThat(foundItinerary.getActivities().get(0).getId()).isEqualTo(activityId);

        // When
        itineraryRepository.deleteById(itineraryId);
        
        // 변경사항 반영 명시적 요청
        itineraryRepository.flush();

        // Then
        assertThat(itineraryActivityRepository.findById(activityId)).isEmpty();
    }
}