package com.travelingdog.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.todayActivity.SaveActivityRequestDTO;
import com.travelingdog.backend.dto.todayActivity.SavedActivityResponseDTO;
import com.travelingdog.backend.dto.todayActivity.TodayActivityRequestDTO;
import com.travelingdog.backend.dto.todayActivity.TodayActivityResponseDTO;
import com.travelingdog.backend.service.TodayActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import com.travelingdog.backend.config.WithMockCustomUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TodayActivityController.class)
class TodayActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodayActivityService todayActivityService;

    @Autowired
    private ObjectMapper objectMapper;

    private TodayActivityRequestDTO validRequest;
    private TodayActivityResponseDTO mockResponse;

    @BeforeEach
    void setUp() {
        // 유효한 요청 생성
        validRequest = new TodayActivityRequestDTO();
        validRequest.setLocation("서울시 강남구");
        validRequest.setRestaurantCount(3);
        validRequest.setCultureCount(2);
        validRequest.setShoppingCount(1);
        validRequest.setNatureCount(0);

        // 모의 응답 생성
        mockResponse = new TodayActivityResponseDTO();
        mockResponse.setLocation("서울시 강남구");
        mockResponse.setCreatedAt(LocalDateTime.now());

        // 모의 추천 데이터 생성
        List<TodayActivityResponseDTO.ActivityRecommendation> restaurants = new ArrayList<>();
        restaurants.add(new TodayActivityResponseDTO.ActivityRecommendation(
                "강남 맛집", "일식"));
        mockResponse.setRestaurants(restaurants);
        mockResponse.setCultureSpots(new ArrayList<>());
        mockResponse.setShoppingSpots(new ArrayList<>());
        mockResponse.setNatureSpots(new ArrayList<>());
    }

    @Test
    @WithMockUser
    void recommendTodayActivity_Success() throws Exception {
        // Given
        when(todayActivityService.generateTodayActivity(any(TodayActivityRequestDTO.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/today-activity/recommend")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("서울시 강남구"))
                .andExpect(jsonPath("$.restaurants").isArray())
                .andExpect(jsonPath("$.restaurants[0].name").value("강남 맛집"));
    }

    @Test
    @WithMockUser
    void recommendTodayActivity_InvalidRequest() throws Exception {
        // Given - 잘못된 요청 (location이 null)
        TodayActivityRequestDTO invalidRequest = new TodayActivityRequestDTO();
        invalidRequest.setLocation(null);
        invalidRequest.setRestaurantCount(3);
        invalidRequest.setCultureCount(2);
        invalidRequest.setShoppingCount(1);
        invalidRequest.setNatureCount(0);

        // When & Then
        mockMvc.perform(post("/api/today-activity/recommend")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void recommendTodayActivity_InvalidCount() throws Exception {
        // Given - 잘못된 개수 (음수)
        TodayActivityRequestDTO invalidRequest = new TodayActivityRequestDTO();
        invalidRequest.setLocation("서울시 강남구");
        invalidRequest.setRestaurantCount(-1);
        invalidRequest.setCultureCount(2);
        invalidRequest.setShoppingCount(1);
        invalidRequest.setNatureCount(0);

        // When & Then
        mockMvc.perform(post("/api/today-activity/recommend")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void healthCheck_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/today-activity/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Today Activity Service is running"));
    }

    @Test
    void recommendTodayActivity_Unauthorized() throws Exception {
        // When & Then - 인증 없이 요청
        mockMvc.perform(post("/api/today-activity/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockCustomUser
    void saveActivity_Success() throws Exception {
        // Given
        SaveActivityRequestDTO saveRequest = new SaveActivityRequestDTO();
        saveRequest.setLocationName("강남 맛집");
        saveRequest.setCategory("한식");
        saveRequest.setSavedLocation("서울시 강남구");

        SavedActivityResponseDTO mockSavedResponse = new SavedActivityResponseDTO();
        mockSavedResponse.setId(1L);
        mockSavedResponse.setLocationName("강남 맛집");
        mockSavedResponse.setCategory("맛집");
        mockSavedResponse.setSavedLocation("서울시 강남구");
        mockSavedResponse.setCreatedAt(LocalDateTime.now());

        when(todayActivityService.saveActivity(any(SaveActivityRequestDTO.class), any()))
                .thenReturn(mockSavedResponse);

        // When & Then
        mockMvc.perform(post("/api/today-activity/save")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("강남 맛집"))
                .andExpect(jsonPath("$.category").value("맛집"));
    }

    @Test
    @WithMockCustomUser
    void saveActivity_InvalidRequest() throws Exception {
        // Given - 이름이 없는 잘못된 요청
        SaveActivityRequestDTO invalidRequest = new SaveActivityRequestDTO();
        invalidRequest.setLocationName(null);
        invalidRequest.setCategory("맛집");

        // When & Then
        mockMvc.perform(post("/api/today-activity/save")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockCustomUser
    void getSavedActivities_Success() throws Exception {
        // Given
        List<SavedActivityResponseDTO> mockSavedList = new ArrayList<>();
        SavedActivityResponseDTO saved1 = new SavedActivityResponseDTO();
        saved1.setId(1L);
        saved1.setLocationName("강남 맛집");
        saved1.setCategory("맛집");
        mockSavedList.add(saved1);

        when(todayActivityService.getSavedActivities(any()))
                .thenReturn(mockSavedList);

        // When & Then
        mockMvc.perform(get("/api/today-activity/saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("강남 맛집"));
    }

    @Test
    @WithMockCustomUser
    void getSavedActivitiesByCategory_Success() throws Exception {
        // Given
        List<SavedActivityResponseDTO> mockSavedList = new ArrayList<>();
        SavedActivityResponseDTO saved1 = new SavedActivityResponseDTO();
        saved1.setId(1L);
        saved1.setLocationName("강남 맛집");
        saved1.setCategory("맛집");
        mockSavedList.add(saved1);

        when(todayActivityService.getSavedActivitiesByCategory(any(), eq("맛집")))
                .thenReturn(mockSavedList);

        // When & Then
        mockMvc.perform(get("/api/today-activity/saved/category/맛집"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].category").value("맛집"));
    }

    @Test
    @WithMockCustomUser
    void deleteSavedActivity_Success() throws Exception {
        // Given
        doNothing().when(todayActivityService).deleteSavedActivity(eq(1L), any());

        // When & Then
        mockMvc.perform(delete("/api/today-activity/saved/1")
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockCustomUser
    void getSavedActivityCount_Success() throws Exception {
        // Given
        when(todayActivityService.getSavedActivityCount(any()))
                .thenReturn(5L);

        // When & Then
        mockMvc.perform(get("/api/today-activity/saved/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }
}