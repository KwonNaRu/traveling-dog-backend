package com.travelingdog.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.config.SecurityConfig;
import com.travelingdog.backend.config.WithMockCustomUser;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityCreateRequest;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityResponseDTO;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityUpdateRequest;
import com.travelingdog.backend.exception.ForbiddenResourceAccessException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.jwt.JwtAuthenticationEntryPoint;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.service.AuthService;
import com.travelingdog.backend.service.ItineraryActivityService;

@WebMvcTest(ItineraryActivityController.class)
@Import({ SecurityConfig.class, JwtAuthenticationEntryPoint.class })
public class ItineraryActivityControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ItineraryActivityService activityService;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private UserDetailsService userDetailsService;

        @MockBean
        private AuthService authService;

        @MockBean
        private JwtTokenProvider jwtTokenProvider;

        private User testUser;
        private ItineraryActivityResponseDTO responseDTO;
        private ItineraryActivityCreateRequest createRequest;
        private ItineraryActivityUpdateRequest updateRequest;

        @BeforeEach
        void setUp() {
                // 테스트 사용자 설정
                testUser = new User();
                testUser.setId(1L);
                testUser.setEmail("test@example.com");

                // 응답 DTO 설정
                responseDTO = ItineraryActivityResponseDTO.builder()
                                .id(1L)
                                .title("성산일출봉 등반")
                                .description("제주도의 상징적인 화산 등반")
                                .locationName("성산일출봉")
                                .itineraryId(1L)
                                .build();

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
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        @DisplayName("활동 상세 조회 테스트")
        void testGetActivity() throws Exception {
                // Given
                when(activityService.getActivity(anyLong(), any(User.class))).thenReturn(responseDTO);

                // When & Then
                mockMvc.perform(get("/api/itinerary/activities/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(1)))
                                .andExpect(jsonPath("$.title", is("성산일출봉 등반")))
                                .andExpect(jsonPath("$.description", is("제주도의 상징적인 화산 등반")))
                                .andExpect(jsonPath("$.locationName", is("성산일출봉")))
                                .andExpect(jsonPath("$.itineraryId", is(1)));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        @DisplayName("존재하지 않는 활동 조회 시 404 반환 테스트")
        void testGetActivityNotFound() throws Exception {
                // Given
                when(activityService.getActivity(anyLong(), any(User.class)))
                                .thenThrow(new ResourceNotFoundException("활동을 찾을 수 없습니다. ID: 1"));

                // When & Then
                mockMvc.perform(get("/api/itinerary/activities/1"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        @DisplayName("권한 없는 활동 조회 시 403 반환 테스트")
        void testGetActivityForbidden() throws Exception {
                // Given
                when(activityService.getActivity(anyLong(), any(User.class)))
                                .thenThrow(new ForbiddenResourceAccessException("해당 일정에 대한 접근 권한이 없습니다."));

                // When & Then
                mockMvc.perform(get("/api/itinerary/activities/1"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        @DisplayName("일정별 활동 목록 조회 테스트")
        void testGetActivitiesByItinerary() throws Exception {
                // Given
                ItineraryActivityResponseDTO responseDTO2 = ItineraryActivityResponseDTO.builder()
                                .id(2L)
                                .title("우도 자전거 투어")
                                .description("우도 섬 자전거 투어")
                                .locationName("우도")
                                .itineraryId(1L)
                                .build();

                List<ItineraryActivityResponseDTO> activities = Arrays.asList(responseDTO, responseDTO2);
                when(activityService.getActivitiesByItineraryId(anyLong(), any(User.class))).thenReturn(activities);

                // When & Then
                mockMvc.perform(get("/api/itinerary/activities/itinerary/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].id", is(1)))
                                .andExpect(jsonPath("$[0].title", is("성산일출봉 등반")))
                                .andExpect(jsonPath("$[1].id", is(2)))
                                .andExpect(jsonPath("$[1].title", is("우도 자전거 투어")));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        @DisplayName("활동 생성 테스트")
        void testCreateActivity() throws Exception {
                // Given
                ItineraryActivityResponseDTO createdDTO = ItineraryActivityResponseDTO.builder()
                                .id(2L)
                                .title(createRequest.getTitle())
                                .description(createRequest.getDescription())
                                .locationName(createRequest.getLocationName())
                                .itineraryId(1L)
                                .build();

                when(activityService.createActivity(any(ItineraryActivityCreateRequest.class), any(User.class)))
                                .thenReturn(createdDTO);

                // When & Then
                mockMvc.perform(post("/api/itinerary/activities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id", is(2)))
                                .andExpect(jsonPath("$.title", is("우도 자전거 투어")))
                                .andExpect(jsonPath("$.description", is("우도 섬 자전거 투어")))
                                .andExpect(jsonPath("$.locationName", is("우도")))
                                .andExpect(jsonPath("$.itineraryId", is(1)));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        @DisplayName("활동 생성 시 필수 필드 누락 400 반환 테스트")
        void testCreateActivityBadRequest() throws Exception {
                // Given
                ItineraryActivityCreateRequest invalidRequest = new ItineraryActivityCreateRequest();
                invalidRequest.setDescription("우도 섬 자전거 투어");
                // title과 locationName 필드 누락
                invalidRequest.setItineraryId(1L);

                // When & Then
                mockMvc.perform(post("/api/itinerary/activities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        @DisplayName("활동 수정 테스트")
        void testUpdateActivity() throws Exception {
                // Given
                ItineraryActivityResponseDTO updatedDTO = ItineraryActivityResponseDTO.builder()
                                .id(1L)
                                .title(updateRequest.getTitle())
                                .description(updateRequest.getDescription())
                                .locationName(updateRequest.getLocationName())
                                .itineraryId(1L)
                                .build();

                when(activityService.updateActivity(eq(1L), any(ItineraryActivityUpdateRequest.class), any(User.class)))
                                .thenReturn(updatedDTO);

                // When & Then
                mockMvc.perform(put("/api/itinerary/activities/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(1)))
                                .andExpect(jsonPath("$.title", is("성산일출봉 트레킹")))
                                .andExpect(jsonPath("$.description", is("제주도의 상징적인 화산 트레킹")))
                                .andExpect(jsonPath("$.locationName", is("성산일출봉")))
                                .andExpect(jsonPath("$.itineraryId", is(1)));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        @DisplayName("활동 삭제 테스트")
        void testDeleteActivity() throws Exception {
                // Given
                doNothing().when(activityService).deleteActivity(anyLong(), any(User.class));

                // When & Then
                mockMvc.perform(delete("/api/itinerary/activities/1"))
                                .andExpect(status().isNoContent());
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        @DisplayName("존재하지 않는 활동 삭제 시 404 반환 테스트")
        void testDeleteActivityNotFound() throws Exception {
                // Given
                doThrow(new ResourceNotFoundException("활동을 찾을 수 없습니다. ID: 1"))
                                .when(activityService).deleteActivity(anyLong(), any(User.class));

                // When & Then
                mockMvc.perform(delete("/api/itinerary/activities/1"))
                                .andExpect(status().isNotFound());
        }
}