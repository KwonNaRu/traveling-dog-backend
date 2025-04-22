package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelingdog.backend.dto.itinerary.ItineraryActivityCreateRequest;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityResponseDTO;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityUpdateRequest;
import com.travelingdog.backend.exception.ForbiddenResourceAccessException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.model.Itinerary;
import com.travelingdog.backend.model.ItineraryActivity;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.ItineraryActivityRepository;
import com.travelingdog.backend.repository.ItineraryRepository;
import com.travelingdog.backend.status.PlanStatus;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class ItineraryActivityServiceTest {

    @Mock
    private ItineraryActivityRepository activityRepository;

    @Mock
    private ItineraryRepository itineraryRepository;

    @InjectMocks
    private ItineraryActivityService activityService;

    private User user;
    private User otherUser;
    private TravelPlan travelPlan;
    private Itinerary itinerary;
    private ItineraryActivity activity;
    private ItineraryActivityCreateRequest createRequest;
    private ItineraryActivityUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 설정
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        // 여행 계획 설정
        travelPlan = TravelPlan.builder()
                .id(1L)
                .title("제주도 여행")
                .country("Korea")
                .city("제주시")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .status(PlanStatus.PRIVATE)
                .user(user)
                .build();

        // 일정 설정
        itinerary = Itinerary.builder()
                .id(1L)
                .date("2024-05-15")
                .location("성산일출봉")
                .travelPlan(travelPlan)
                .activities(new ArrayList<>())
                .build();

        // 활동 설정
        activity = ItineraryActivity.builder()
                .id(1L)
                .title("성산일출봉 등반")
                .description("제주도의 상징적인 화산 등반")
                .locationName("성산일출봉")
                .itinerary(itinerary)
                .build();

        itinerary.getActivities().add(activity);

        // 생성 요청 설정
        createRequest = new ItineraryActivityCreateRequest();
        createRequest.setTitle("우도 자전거 투어");
        createRequest.setDescription("우도 섬 자전거 투어");
        createRequest.setLocationName("우도");
        createRequest.setItineraryId(1L);

        // 수정 요청 설정
        updateRequest = new ItineraryActivityUpdateRequest();
        updateRequest.setTitle("성산일출봉 트레킹");
        updateRequest.setDescription("제주도의 상징적인 화산 트레킹");
        updateRequest.setLocationName("성산일출봉");
    }

    @Test
    @DisplayName("특정 활동 조회 테스트")
    void testGetActivity() {
        // Given
        when(activityRepository.findById(anyLong())).thenReturn(Optional.of(activity));

        // When
        ItineraryActivityResponseDTO response = activityService.getActivity(1L, user);

        // Then
        assertEquals(activity.getId(), response.getId());
        assertEquals(activity.getTitle(), response.getTitle());
        assertEquals(activity.getDescription(), response.getDescription());
        assertEquals(activity.getLocationName(), response.getLocationName());
        assertEquals(activity.getItinerary().getId(), response.getItineraryId());

        verify(activityRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 활동 조회 시 예외 발생 테스트")
    void testGetActivityNotFound() {
        // Given
        when(activityRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            activityService.getActivity(1L, user);
        });

        verify(activityRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("권한 없는 사용자의 활동 조회 시 예외 발생 테스트")
    void testGetActivityForbidden() {
        // Given
        when(activityRepository.findById(anyLong())).thenReturn(Optional.of(activity));

        // When & Then
        assertThrows(ForbiddenResourceAccessException.class, () -> {
            activityService.getActivity(1L, otherUser);
        });

        verify(activityRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("일정별 활동 목록 조회 테스트")
    void testGetActivitiesByItineraryId() {
        // Given
        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));

        // When
        List<ItineraryActivityResponseDTO> activities = activityService.getActivitiesByItineraryId(1L, user);

        // Then
        assertEquals(1, activities.size());
        assertEquals(activity.getId(), activities.get(0).getId());
        assertEquals(activity.getTitle(), activities.get(0).getTitle());

        verify(itineraryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("활동 생성 테스트")
    void testCreateActivity() {
        // Given
        ItineraryActivity newActivity = ItineraryActivity.builder()
                .id(2L)
                .title(createRequest.getTitle())
                .description(createRequest.getDescription())
                .locationName(createRequest.getLocationName())
                .itinerary(itinerary)
                .build();

        when(itineraryRepository.findById(anyLong())).thenReturn(Optional.of(itinerary));
        when(activityRepository.save(any(ItineraryActivity.class))).thenReturn(newActivity);

        // When
        ItineraryActivityResponseDTO response = activityService.createActivity(createRequest, user);

        // Then
        assertEquals(newActivity.getId(), response.getId());
        assertEquals(newActivity.getTitle(), response.getTitle());
        assertEquals(newActivity.getDescription(), response.getDescription());
        assertEquals(newActivity.getLocationName(), response.getLocationName());
        assertEquals(newActivity.getItinerary().getId(), response.getItineraryId());

        verify(itineraryRepository, times(1)).findById(1L);
        verify(activityRepository, times(1)).save(any(ItineraryActivity.class));
    }

    @Test
    @DisplayName("활동 수정 테스트")
    void testUpdateActivity() {
        // Given
        ItineraryActivity updatedActivity = ItineraryActivity.builder()
                .id(1L)
                .title(updateRequest.getTitle())
                .description(updateRequest.getDescription())
                .locationName(updateRequest.getLocationName())
                .itinerary(itinerary)
                .build();

        when(activityRepository.findById(anyLong())).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(ItineraryActivity.class))).thenReturn(updatedActivity);

        // When
        ItineraryActivityResponseDTO response = activityService.updateActivity(1L, updateRequest, user);

        // Then
        assertEquals(updatedActivity.getId(), response.getId());
        assertEquals(updatedActivity.getTitle(), response.getTitle());
        assertEquals(updatedActivity.getDescription(), response.getDescription());
        assertEquals(updatedActivity.getLocationName(), response.getLocationName());

        verify(activityRepository, times(1)).findById(1L);
        verify(activityRepository, times(1)).save(any(ItineraryActivity.class));
    }

    @Test
    @DisplayName("활동 삭제 테스트")
    void testDeleteActivity() {
        // Given
        when(activityRepository.findById(anyLong())).thenReturn(Optional.of(activity));
        doNothing().when(activityRepository).delete(any(ItineraryActivity.class));

        // When
        activityService.deleteActivity(1L, user);

        // Then
        verify(activityRepository, times(1)).findById(1L);
        verify(activityRepository, times(1)).delete(activity);
    }
}