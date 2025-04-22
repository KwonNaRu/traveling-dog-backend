package com.travelingdog.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityCreateRequest;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityUpdateRequest;
import com.travelingdog.backend.model.Itinerary;
import com.travelingdog.backend.model.ItineraryActivity;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.ItineraryActivityRepository;
import com.travelingdog.backend.repository.ItineraryRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.status.PlanStatus;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Transactional
public class ItineraryActivityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Autowired
    private ItineraryRepository itineraryRepository;

    @Autowired
    private ItineraryActivityRepository activityRepository;

    private User testUser;
    private User otherUser;
    private TravelPlan travelPlan;
    private Itinerary itinerary;
    private ItineraryActivity activity;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .nickname("testUser")
                .email("test@example.com")
                .password("password123")
                .build();
        userRepository.save(testUser);

        // 다른 사용자 생성
        otherUser = User.builder()
                .nickname("otherUser")
                .email("other@example.com")
                .password("password123")
                .build();
        userRepository.save(otherUser);

        // 사용자 인증 설정
        Authentication auth = new UsernamePasswordAuthenticationToken(testUser, null,
                List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 여행 계획 생성
        travelPlan = TravelPlan.builder()
                .title("제주도 여행")
                .country("Korea")
                .city("제주시")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .status(PlanStatus.PRIVATE)
                .user(testUser)
                .build();
        travelPlanRepository.save(travelPlan);

        // 일정 생성
        itinerary = Itinerary.builder()
                .date("2024-05-15")
                .location("성산일출봉")
                .travelPlan(travelPlan)
                .build();
        itineraryRepository.save(itinerary);

        // 활동 생성 (연관관계 편의 메소드 사용)
        activity = ItineraryActivity.builder()
                .title("성산일출봉 등반")
                .description("제주도의 상징적인 화산 등반")
                .locationName("성산일출봉")
                .build();

        // 일정에 활동 추가 (양방향 연관관계 설정)
        itinerary.addActivity(activity);
        activityRepository.save(activity);

        // 일정 업데이트
        itineraryRepository.save(itinerary);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        activityRepository.deleteAll();
        itineraryRepository.deleteAll();
        travelPlanRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("활동 상세 조회 통합 테스트")
    void testGetActivity() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/itinerary/activities/{id}", activity.getId())
                .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(activity.getId().intValue())))
                .andExpect(jsonPath("$.title", is("성산일출봉 등반")))
                .andExpect(jsonPath("$.description", is("제주도의 상징적인 화산 등반")))
                .andExpect(jsonPath("$.locationName", is("성산일출봉")))
                .andExpect(jsonPath("$.itineraryId", is(itinerary.getId().intValue())));
    }

    @Test
    @DisplayName("일정별 활동 목록 조회 통합 테스트")
    void testGetActivitiesByItinerary() throws Exception {
        // 첫 번째 활동은 이미 setUp()에서 저장됨

        // 두 번째 활동 생성 및 저장 (연관관계 편의 메소드 사용)
        ItineraryActivity secondActivity = ItineraryActivity.builder()
                .title("우도 자전거 투어")
                .description("우도 섬 자전거 투어")
                .locationName("우도")
                .build();

        // 일정에 활동 추가 (양방향 연관관계 설정)
        itinerary.addActivity(secondActivity);
        activityRepository.save(secondActivity);
        itineraryRepository.save(itinerary);

        // 연관관계 갱신을 위해 Itinerary 다시 조회
        itinerary = itineraryRepository.findById(itinerary.getId()).orElseThrow();

        // 데이터베이스 상태 확인용 로그
        System.out.println("저장된 활동 수: " + activityRepository.count());
        System.out.println("일정에 연결된 활동 수: " + itinerary.getActivities().size());
        itinerary.getActivities().forEach(a -> System.out.println("활동 ID: " + a.getId() + ", 제목: " + a.getTitle()));

        // When & Then
        mockMvc.perform(get("/api/itinerary/activities/itinerary/{id}", itinerary.getId())
                .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("성산일출봉 등반")))
                .andExpect(jsonPath("$[1].title", is("우도 자전거 투어")));
    }

    @Test
    @DisplayName("활동 생성 통합 테스트")
    void testCreateActivity() throws Exception {
        // Given
        ItineraryActivityCreateRequest createRequest = new ItineraryActivityCreateRequest();
        createRequest.setTitle("한라산 등반");
        createRequest.setDescription("제주도의 대표 산 등반");
        createRequest.setLocationName("한라산");
        createRequest.setItineraryId(itinerary.getId());

        // When & Then
        mockMvc.perform(post("/api/itinerary/activities")
                .header("Authorization", "Bearer dummy-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("한라산 등반")))
                .andExpect(jsonPath("$.description", is("제주도의 대표 산 등반")))
                .andExpect(jsonPath("$.locationName", is("한라산")))
                .andExpect(jsonPath("$.itineraryId", is(itinerary.getId().intValue())));

        // 실제로 DB에 저장되었는지 확인
        // 데이터베이스에서 일정을 새로 조회
        itinerary = itineraryRepository.findById(itinerary.getId()).orElseThrow();

        // 데이터베이스 상태 확인용 로그
        System.out.println("활동 생성 후 저장된 활동 수: " + activityRepository.count());
        System.out.println("활동 생성 후 일정에 연결된 활동 수: " + itinerary.getActivities().size());

        List<ItineraryActivity> activities = activityRepository.findAll();
        assertThat(activities).hasSize(2);
        assertThat(activities.get(1).getTitle()).isEqualTo("한라산 등반");
    }

    @Test
    @DisplayName("활동 수정 통합 테스트")
    void testUpdateActivity() throws Exception {
        // Given
        ItineraryActivityUpdateRequest updateRequest = new ItineraryActivityUpdateRequest();
        updateRequest.setTitle("성산일출봉 트레킹");
        updateRequest.setDescription("제주도의 상징적인 화산 트레킹");
        updateRequest.setLocationName("성산일출봉 일대");

        // When & Then
        mockMvc.perform(put("/api/itinerary/activities/{id}", activity.getId())
                .header("Authorization", "Bearer dummy-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(activity.getId().intValue())))
                .andExpect(jsonPath("$.title", is("성산일출봉 트레킹")))
                .andExpect(jsonPath("$.description", is("제주도의 상징적인 화산 트레킹")))
                .andExpect(jsonPath("$.locationName", is("성산일출봉 일대")))
                .andExpect(jsonPath("$.itineraryId", is(itinerary.getId().intValue())));

        // 실제로 DB에 업데이트되었는지 확인
        // 데이터베이스에서 일정과 활동을 새로 조회
        itinerary = itineraryRepository.findById(itinerary.getId()).orElseThrow();

        // 데이터베이스 상태 확인용 로그
        System.out.println("활동 수정 후 일정에 연결된 활동 수: " + itinerary.getActivities().size());
        itinerary.getActivities().forEach(a -> System.out.println("활동 ID: " + a.getId() + ", 제목: " + a.getTitle()));

        ItineraryActivity updatedActivity = activityRepository.findById(activity.getId()).orElseThrow();
        assertThat(updatedActivity.getTitle()).isEqualTo("성산일출봉 트레킹");
        assertThat(updatedActivity.getLocationName()).isEqualTo("성산일출봉 일대");
    }

    @Test
    @DisplayName("활동 삭제 통합 테스트")
    void testDeleteActivity() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/itinerary/activities/{id}", activity.getId())
                .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isNoContent());

        // 실제로 DB에서 삭제되었는지 확인
        assertThat(activityRepository.findById(activity.getId())).isEmpty();
    }

    @Test
    @DisplayName("권한 없는 활동 접근 통합 테스트")
    void testForbiddenAccess() throws Exception {
        // Given
        // 다른 사용자의 여행 계획과 일정, 활동 생성
        TravelPlan otherTravelPlan = TravelPlan.builder()
                .title("다른 사용자의 제주도 여행")
                .country("Korea")
                .city("제주시")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .status(PlanStatus.PRIVATE)
                .user(otherUser)
                .build();
        travelPlanRepository.save(otherTravelPlan);

        Itinerary otherItinerary = Itinerary.builder()
                .date("2024-05-15")
                .location("성산일출봉")
                .travelPlan(otherTravelPlan)
                .build();
        itineraryRepository.save(otherItinerary);

        ItineraryActivity otherActivity = ItineraryActivity.builder()
                .title("다른 사용자의 성산일출봉 등반")
                .description("제주도의 상징적인 화산 등반")
                .locationName("성산일출봉")
                .build();

        // 일정에 활동 추가 (양방향 연관관계 설정)
        otherItinerary.addActivity(otherActivity);
        activityRepository.save(otherActivity);
        itineraryRepository.save(otherItinerary);

        // When & Then - 다른 사용자의 활동 조회 시도
        mockMvc.perform(get("/api/itinerary/activities/{id}", otherActivity.getId())
                .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isForbidden());

        // When & Then - 다른 사용자의 활동 수정 시도
        ItineraryActivityUpdateRequest updateRequest = new ItineraryActivityUpdateRequest();
        updateRequest.setTitle("허가되지 않은 수정");
        updateRequest.setDescription("허가되지 않은 설명");
        updateRequest.setLocationName("허가되지 않은 장소");

        mockMvc.perform(put("/api/itinerary/activities/{id}", otherActivity.getId())
                .header("Authorization", "Bearer dummy-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // When & Then - 다른 사용자의 활동 삭제 시도
        mockMvc.perform(delete("/api/itinerary/activities/{id}", otherActivity.getId())
                .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isForbidden());
    }
}