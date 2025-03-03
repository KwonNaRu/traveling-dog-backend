package com.travelingdog.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.config.SecurityConfig;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanUpdateRequest;
import com.travelingdog.backend.jwt.JwtTokenProvider;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.service.TravelPlanService;

@WebMvcTest(TravelPlanController.class)
@Import({ SecurityConfig.class, TravelPlanControllerTest.MockSecurityConfig.class })
public class TravelPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelPlanRepository travelPlanRepository;

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
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(3));

        // 테스트용 사용자 생성
        User user = User.builder()
                .nickname("testUser")
                .password("password")
                .email("test@example.com")
                .build();

        userRepository.save(user);

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

        // 여행 계획 생성
        travelPlan = TravelPlan.builder()
                .title("Test Travel Plan")
                .country("South Korea")
                .city("Seoul")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .user(user)
                .travelLocations(mockLocations)
                .build();
        travelPlanRepository.save(travelPlan);
    }

    @Test
    public void testGenerateTripPlan() throws Exception {
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
    public void testGenerateTripPlan_EmptyResponse() throws Exception {
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
    public void testGenerateTripPlan_InvalidRequest() throws Exception {
        // Given
        TravelPlanRequest invalidRequest = new TravelPlanRequest();
        // 필수 필드 누락

        // When & Then
        mockMvc.perform(post("/trip/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetTravelPlanList_FromUser() {
        // Given
        when(tripPlanService.getTravelPlanList(any(Long.class))).thenReturn(List.of(travelPlan));

        // When & Then
        mockMvc.perform(get("/trip/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(travelPlan.getId()))
                .andExpect(jsonPath("$[0].title").value(travelPlan.getTitle()))
                .andExpect(jsonPath("$[0].startDate").value(travelPlan.getStartDate().toString()))
                .andExpect(jsonPath("$[0].endDate").value(travelPlan.getEndDate().toString()))
                .andExpect(jsonPath("$[0].user.id").value(travelPlan.getUser().getId()))
                .andExpect(jsonPath("$[0].country").value(travelPlan.getCountry()))
                .andExpect(jsonPath("$[0].city").value(travelPlan.getCity()));
    }

    @Test
    public void testGetTravelPlanDetail_FromUser() {
        // Given
        when(tripPlanService.getTravelPlanDetail(any(Long.class))).thenReturn(travelPlan);

        // When & Then
        mockMvc.perform(get("/trip/plan/{id}", travelPlan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(travelPlan.getId()))
                .andExpect(jsonPath("$.travelLocations").isArray())
                .andExpect(jsonPath("$.travelLocations.length()").value(2))
                .andExpect(jsonPath("$.travelLocations[0].id").value(travelPlan.getTravelLocations().get(0).getId()))
                .andExpect(jsonPath("$.travelLocations[0].placeName")
                        .value(travelPlan.getTravelLocations().get(0).getPlaceName()))
                .andExpect(jsonPath("$.travelLocations[0].longitude")
                        .value(travelPlan.getTravelLocations().get(0).getCoordinates().getX()))
                .andExpect(jsonPath("$.travelLocations[0].latitude")
                        .value(travelPlan.getTravelLocations().get(0).getCoordinates().getY()));
    }

    /*
     * 여행 계획 수정에서 국가와 도시는 변경 불가능
     */
    @Test
    public void testFailedUpdateTravelPlan_FromUser() throws Exception {
        // Given
        TravelPlanRequest updateRequest = new TravelPlanRequest();
        updateRequest.setCountry("Japan");
        updateRequest.setCity("Tokyo");

        when(tripPlanService.updateTravelPlanDetail(any(Long.class), any(TravelPlanRequest.class)))
                .thenThrow(new ImmutableTravelPlanException());

        // When & Then
        mockMvc.perform(put("/trip/plan/{id}", travelPlan.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("국가와 도시는 변경 불가능합니다."));
    }

    /*
     * 여행 계획 수정
     * 해당 유저만 수정 가능
     */
    @Test
    public void testFailedUpdateTravelPlan_FromAnotherUser() throws Exception {
        // Given
        when(tripPlanService.updateTravelPlanDetail(any(Long.class), any(TravelPlanUpdateRequest.class)))
                .thenThrow(new ForbiddenResourceAccessException());

        // When & Then
        mockMvc.perform(put("/trip/plan/{id}", travelPlan.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateTravelPlanDetail_FromUser() {
        // Given
        TravelPlanUpdateRequest updateRequest = new TravelPlanUpdateRequest();
        updateRequest.setStartDate(LocalDate.now().plusDays(5));
        updateRequest.setEndDate(LocalDate.now().plusDays(8));
        updateRequest.setTitle("Updated Travel Plan");

        when(tripPlanService.updateTravelPlanDetail(any(Long.class), any(TravelPlanRequest.class)))
                .thenReturn(updateRequest);

        // When & Then
        mockMvc.perform(put("/trip/plan/{id}", travelPlan.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(travelPlan.getId()))
                .andExpect(jsonPath("$.title").value(updateRequest.getTitle()))
                .andExpect(jsonPath("$.startDate").value(updateRequest.getStartDate().toString()))
                .andExpect(jsonPath("$.endDate").value(updateRequest.getEndDate().toString()))
                .andExpect(jsonPath("$.user.id").value(travelPlan.getUser().getId()))
                .andExpect(jsonPath("$.country").value(travelPlan.getCountry()))
                .andExpect(jsonPath("$.city").value(travelPlan.getCity()));
    }

    /*
     * 여행 계획 삭제
     * 해당 유저만 삭제 가능
     */
    @Test
    public void testDeleteTravelPlan_FromUser() {
        // Given
        when(tripPlanService.deleteTravelPlan(any(Long.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/trip/plan/{id}", travelPlan.getId()))
                .andExpect(status().isOk());
    }

    /*
     * 인증 정보가 맞지 않은 유저는 삭제 불가능
     */
    @Test
    public void testFailedDeleteTravelPlan_FromAnotherUser() throws Exception {
        // Given
        when(tripPlanService.deleteTravelPlan(any(Long.class))).thenThrow(new ForbiddenResourceAccessException());

        // When & Then
        mockMvc.perform(delete("/trip/plan/{id}", travelPlan.getId()))
                .andExpect(status().isForbidden());
    }

}