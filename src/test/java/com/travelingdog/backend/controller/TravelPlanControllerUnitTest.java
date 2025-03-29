package com.travelingdog.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.config.SecurityConfig;
import com.travelingdog.backend.config.WithMockCustomUser;
import com.travelingdog.backend.dto.ItineraryDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanUpdateRequest;
import com.travelingdog.backend.exception.ForbiddenResourceAccessException;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.ItineraryRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.service.AuthService;
import com.travelingdog.backend.service.SessionService;
import com.travelingdog.backend.service.TravelPlanService;
import com.travelingdog.backend.status.PlanStatus;

@WebMvcTest(TravelPlanController.class)
@Import({ SecurityConfig.class, TravelPlanControllerUnitTest.MockConfig.class })
public class TravelPlanControllerUnitTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private TravelPlanService travelPlanService;

        @MockBean
        private TravelPlanRepository travelPlanRepository;

        @MockBean
        private ItineraryRepository itineraryRepository;

        @MockBean
        private UserDetailsService userDetailsService;

        private User testUser;
        private TravelPlanRequest request;
        private TravelPlanDTO travelPlanDTO;

        @BeforeEach
        public void setUp() {
                // 테스트 사용자 설정
                testUser = new User();
                testUser.setId(1L);
                testUser.setEmail("test@example.com");
                testUser.setPassword("password");

                // UserRepository 모킹
                when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

                // 요청 객체 설정
                request = new TravelPlanRequest();
                request.setTitle("Test Travel Plan");
                request.setCountry("South Korea");
                request.setCity("Seoul");
                request.setStartDate(LocalDate.now().plusDays(1));
                request.setEndDate(LocalDate.now().plusDays(5));
                request.setSeason("Spring");
                request.setTravelStyle("Cultural and historical exploration");
                request.setBudget("1000000");
                request.setInterests("Cultural heritage, historical sites");
                request.setAccommodation("Hotel");
                request.setTransportation("Public transportation");

                // 응답 DTO 설정
                travelPlanDTO = new TravelPlanDTO();
                travelPlanDTO.setId(1L);
                travelPlanDTO.setTitle("Test Travel Plan");
                travelPlanDTO.setCountry("South Korea");
                travelPlanDTO.setCity("Seoul");
                travelPlanDTO.setStatus(PlanStatus.PUBLISHED);
                travelPlanDTO.setStartDate(LocalDate.now().plusDays(1));
                travelPlanDTO.setEndDate(LocalDate.now().plusDays(5));
                travelPlanDTO.setSeason("Spring");
                travelPlanDTO.setTravelStyle("Cultural and historical exploration");
                travelPlanDTO.setBudget("1000000");
                travelPlanDTO.setInterests("Cultural heritage, historical sites");
                travelPlanDTO.setAccommodation("Hotel");
                travelPlanDTO.setTransportation("Public transportation");

                // 일정 데이터 설정
                List<ItineraryDTO> itineraries = new ArrayList<>();
                itineraries.add(ItineraryDTO.builder()
                                .id(1L)
                                .location("남산 타워")
                                .date(1)
                                .build());
                itineraries.add(ItineraryDTO.builder()
                                .id(2L)
                                .location("홍대입구")
                                .date(2)
                                .build());
                travelPlanDTO.setItineraries(itineraries);
        }

        @AfterEach
        public void cleanup() {
                // 테스트 후 정리
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        public void testCreateTravelPlan() throws Exception {
                // Given
                when(travelPlanService.createTravelPlan(any(TravelPlanRequest.class), any(User.class)))
                                .thenReturn(travelPlanDTO);

                // When & Then
                mockMvc.perform(post("/api/travel/plan")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.title").value(request.getTitle()))
                                .andExpect(jsonPath("$.itineraries.length()").value(2))
                                .andExpect(jsonPath("$.itineraries[0].location").value("남산 타워"))
                                .andExpect(jsonPath("$.itineraries[1].location").value("홍대입구"));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        public void testGetTravelPlanList() throws Exception {
                // Given
                List<TravelPlanDTO> travelPlans = new ArrayList<>();
                travelPlans.add(travelPlanDTO);
                when(travelPlanService.getTravelPlanList(any(User.class)))
                                .thenReturn(travelPlans);

                // When & Then
                mockMvc.perform(get("/api/travel/plans"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].itineraries.length()").value(2))
                                .andExpect(jsonPath("$[0].itineraries[0].location").value("남산 타워"));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        public void testUpdateTravelPlan() throws Exception {
                // Given
                TravelPlanUpdateRequest updateRequest = new TravelPlanUpdateRequest();
                updateRequest.setTitle("Updated Travel Plan");
                updateRequest.setStartDate(LocalDate.now().plusDays(2));
                updateRequest.setEndDate(LocalDate.now().plusDays(6));

                TravelPlanDTO updatedTravelPlanDTO = new TravelPlanDTO();
                updatedTravelPlanDTO.setId(1L);
                updatedTravelPlanDTO.setTitle("Updated Travel Plan");
                updatedTravelPlanDTO.setStartDate(LocalDate.now().plusDays(2));
                updatedTravelPlanDTO.setEndDate(LocalDate.now().plusDays(6));

                // 일정 데이터 설정
                List<ItineraryDTO> itineraries = new ArrayList<>();
                itineraries.add(ItineraryDTO.builder()
                                .id(1L)
                                .location("강남")
                                .date(1)
                                .build());
                itineraries.add(ItineraryDTO.builder()
                                .id(2L)
                                .location("잠실")
                                .date(2)
                                .build());
                updatedTravelPlanDTO.setItineraries(itineraries);

                when(travelPlanService.updateTravelPlan(anyLong(), any(TravelPlanUpdateRequest.class), any(User.class)))
                                .thenReturn(updatedTravelPlanDTO);

                // When & Then
                mockMvc.perform(put("/api/travel/plan/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.title").value(updateRequest.getTitle()))
                                .andExpect(jsonPath("$.itineraries.length()").value(2))
                                .andExpect(jsonPath("$.itineraries[0].location").value("강남"))
                                .andExpect(jsonPath("$.itineraries[1].location").value("잠실"));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        public void testDeleteTravelPlan() throws Exception {
                // Given
                doNothing().when(travelPlanService).deleteTravelPlan(anyLong(), any(User.class));

                // When & Then
                mockMvc.perform(delete("/api/travel/plan/{id}", 1L))
                                .andExpect(status().isNoContent());
        }

        /**
         * 여행 계획 생성 - 빈 응답 처리 테스트
         */
        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        public void testCreateTravelPlan_EmptyResponse() throws Exception {
                // Given
                when(travelPlanService.createTravelPlan(any(TravelPlanRequest.class), any(User.class)))
                                .thenReturn(null);

                // When & Then
                mockMvc.perform(post("/api/travel/plan")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());
        }

        /**
         * 여행 계획 생성 - 유효하지 않은 요청 처리 테스트
         */
        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        public void testCreateTravelPlan_InvalidRequest() throws Exception {
                // Given
                TravelPlanRequest invalidRequest = new TravelPlanRequest();
                // 필수 필드를 비워둠

                // When & Then
                mockMvc.perform(post("/api/travel/plan")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        /**
         * 여행 계획 업데이트 - 다른 사용자의 여행 계획 업데이트 실패 테스트
         */
        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        public void testFailedUpdateTravelPlan_FromAnotherUser() throws Exception {
                // Given
                TravelPlanUpdateRequest updateRequest = new TravelPlanUpdateRequest();
                updateRequest.setTitle("Updated Travel Plan");
                updateRequest.setStartDate(LocalDate.now().plusDays(5));
                updateRequest.setEndDate(LocalDate.now().plusDays(8));

                when(travelPlanService.updateTravelPlan(any(Long.class), any(TravelPlanUpdateRequest.class),
                                any(User.class)))
                                .thenThrow(
                                                new ForbiddenResourceAccessException(
                                                                "You don't have permission to update this travel plan"));

                // When & Then
                mockMvc.perform(put("/api/travel/plan/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isForbidden());
        }

        /**
         * 여행 계획 삭제 - 다른 사용자의 여행 계획 삭제 실패 테스트
         */
        @Test
        @WithMockCustomUser(email = "test@example.com", roles = "USER")
        public void testFailedDeleteTravelPlan_FromAnotherUser() throws Exception {
                // Given
                doThrow(new ForbiddenResourceAccessException("삭제할 수 없는 여행 계획입니다."))
                                .when(travelPlanService).deleteTravelPlan(any(Long.class), any(User.class));

                // When & Then
                mockMvc.perform(delete("/api/travel/plan/{id}", 1L))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("삭제할 수 없는 여행 계획입니다."));
        }

        @TestConfiguration
        static class MockConfig {
                @Bean
                public AuthService authService() {
                        return Mockito.mock(AuthService.class);
                }

                @Bean
                public JwtTokenProvider jwtTokenProvider() {
                        return Mockito.mock(JwtTokenProvider.class);
                }

                @Bean
                public UserDetailsService userDetailsService() {
                        return Mockito.mock(UserDetailsService.class);
                }

                @Bean
                public UserRepository userRepository() {
                        return Mockito.mock(UserRepository.class);
                }

                @Bean
                public SessionService sessionService() {
                        return Mockito.mock(SessionService.class);
                }
        }

}