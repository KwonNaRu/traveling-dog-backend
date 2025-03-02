package com.travelingdog.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.config.SecurityConfig;
import com.travelingdog.backend.dto.TravelPlanRequest;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.service.TravelPlanService;

@WebMvcTest(TravelPlanController.class)
@Import({ SecurityConfig.class, TravelPlanControllerTest.MockSecurityConfig.class })
public class TravelPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TravelPlanService tripPlanService;

    private TravelPlanRequest request;
    private List<TravelLocation> mockLocations;
    private GeometryFactory geometryFactory;
    private TravelPlan travelPlan;

    @TestConfiguration
    static class MockSecurityConfig {
        @Bean
        public JwtTokenProvider jwtTokenProvider() {
            return Mockito.mock(JwtTokenProvider.class);
        }

        @Bean
        public UserDetailsService userDetailsService() {
            return Mockito.mock(UserDetailsService.class);
        }
    }

    @BeforeEach
    public void setUp() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        request = new TravelPlanRequest();
        request.setCountry("South Korea");
        request.setCity("Seoul");
        request.setStartDate(LocalDate.now().toString());
        request.setEndDate(LocalDate.now().plusDays(3).toString());

        // 여행 계획 생성
        travelPlan = TravelPlan.builder()
                .title("Test Travel Plan")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .build();

        // 모의 여행 장소 데이터 생성
        TravelLocation location1 = TravelLocation.builder()
                .id(1L)
                .placeName("Gyeongbokgung Palace")
                .coordinates(geometryFactory.createPoint(new Coordinate(126.977, 37.579)))
                .description("Beautiful palace in Seoul")
                .locationOrder(1)
                .availableDate(LocalDate.now())
                .travelPlan(travelPlan)
                .build();

        TravelLocation location2 = TravelLocation.builder()
                .id(2L)
                .placeName("Namsan Tower")
                .coordinates(geometryFactory.createPoint(new Coordinate(126.988, 37.551)))
                .description("Iconic tower with great views")
                .locationOrder(2)
                .availableDate(LocalDate.now().plusDays(1))
                .travelPlan(travelPlan)
                .build();

        mockLocations = Arrays.asList(location1, location2);
    }

    @Test
    public void testGetTripPlan() throws Exception {
        // Given
        when(tripPlanService.generateTripPlan(any(TravelPlanRequest.class))).thenReturn(mockLocations);

        // When & Then
        mockMvc.perform(post("/trip/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].placeName").value("Gyeongbokgung Palace"))
                .andExpect(jsonPath("$[1].placeName").value("Namsan Tower"))
                .andExpect(jsonPath("$[0].longitude").value(126.977))
                .andExpect(jsonPath("$[0].latitude").value(37.579));
    }

    @Test
    public void testGetTripPlan_EmptyResponse() throws Exception {
        // Given
        when(tripPlanService.generateTripPlan(any(TravelPlanRequest.class))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(post("/trip/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testGetTripPlan_InvalidRequest() throws Exception {
        // Given
        TravelPlanRequest invalidRequest = new TravelPlanRequest();
        // 필수 필드 누락

        // When & Then
        mockMvc.perform(post("/trip/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}