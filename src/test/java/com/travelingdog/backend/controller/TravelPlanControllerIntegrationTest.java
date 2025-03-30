package com.travelingdog.backend.controller;

import static org.mockito.ArgumentMatchers.any;
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
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.config.WithMockCustomUser;
import com.travelingdog.backend.dto.travelPlan.AccommodationTypeDTO;
import com.travelingdog.backend.dto.travelPlan.InterestDTO;
import com.travelingdog.backend.dto.travelPlan.ItineraryDTO;
import com.travelingdog.backend.dto.travelPlan.TransportationTypeDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanUpdateRequest;
import com.travelingdog.backend.dto.travelPlan.TravelStyleDTO;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.service.TravelPlanService;
import com.travelingdog.backend.status.PlanStatus;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TravelPlanControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TravelPlanRepository travelPlanRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @MockBean
        private TravelPlanService travelPlanService;

        private User testUser;
        private TravelPlan testTravelPlan;

        @BeforeEach
        public void setUp() {
                // 테스트 사용자 생성 및 저장
                testUser = new User();
                testUser.setEmail("test@example.com");
                testUser.setPassword(passwordEncoder.encode("password"));
                testUser.setNickname("Test User");
                userRepository.save(testUser);

                // 테스트 여행 계획 생성 및 저장
                testTravelPlan = new TravelPlan();
                testTravelPlan.setTitle("Test Travel Plan");
                testTravelPlan.setCountry("South Korea");
                testTravelPlan.setCity("Seoul");
                testTravelPlan.setStartDate(LocalDate.now().plusDays(1));
                testTravelPlan.setEndDate(LocalDate.now().plusDays(5));
                testTravelPlan.setUser(testUser);
                testTravelPlan.setStatus(PlanStatus.PUBLISHED);
                testTravelPlan.setSeason("Spring");
                testTravelPlan.setBudget("1000000");
                testTravelPlan.setTravelStyles(new ArrayList<>());
                testTravelPlan.setInterests(new ArrayList<>());
                testTravelPlan.setAccommodationTypes(new ArrayList<>());
                testTravelPlan.setTransportationTypes(new ArrayList<>());
                testTravelPlan.setItineraries(new ArrayList<>());
                testTravelPlan.setRestaurantRecommendations(new ArrayList<>());
                testTravelPlan.setAccommodationRecommendations(new ArrayList<>());
                testTravelPlan.setLikes(new ArrayList<>());
                testTravelPlan.setViewCount(0);
                travelPlanRepository.save(testTravelPlan);
        }

        @AfterEach
        public void cleanup() {
                // 테스트 데이터 정리
                travelPlanRepository.deleteAll();
                userRepository.deleteAll();
        }

        @Test
        @WithMockCustomUser(email = "test@example.com")
        public void testGenerateTravelPlan() throws Exception {
                // Given
                TravelPlanRequest request = new TravelPlanRequest();
                request.setTitle("New Travel Plan");
                request.setCountry("Japan");
                request.setCity("Tokyo");
                request.setStartDate(LocalDate.now().plusDays(10));
                request.setEndDate(LocalDate.now().plusDays(15));
                request.setSeason("Spring");
                request.setTravelStyle("Adventure");
                request.setBudget("1000000");
                request.setInterests("Shopping, Food, Culture");
                request.setAccommodation("Hotel");
                request.setTransportation("Train, Bus");

                List<ItineraryDTO> itineraries = new ArrayList<>();
                itineraries.add(ItineraryDTO.builder()
                                .id(1L)
                                .location("도쿄 타워")
                                .date(1)
                                .activities(new ArrayList<>())
                                .lunch(null)
                                .dinner(null)
                                .build());
                itineraries.add(ItineraryDTO.builder()
                                .id(2L)
                                .location("시부야")
                                .date(2)
                                .activities(new ArrayList<>())
                                .lunch(null)
                                .dinner(null)
                                .build());
                itineraries.add(ItineraryDTO.builder()
                                .id(3L)
                                .location("하라주쿠")
                                .date(3)
                                .activities(new ArrayList<>())
                                .lunch(null)
                                .dinner(null)
                                .build());

                TravelPlanDTO mockResponse = new TravelPlanDTO();
                mockResponse.setId(999L);
                mockResponse.setTitle("New Travel Plan");
                mockResponse.setCountry("Japan");
                mockResponse.setCity("Tokyo");
                mockResponse.setStartDate(LocalDate.now().plusDays(10));
                mockResponse.setEndDate(LocalDate.now().plusDays(15));
                mockResponse.setItineraries(itineraries);
                mockResponse.setSeason("Spring");
                mockResponse.setTravelStyles(new ArrayList<>());
                mockResponse.setBudget("1000000");
                mockResponse.setInterests(new ArrayList<>());
                mockResponse.setAccommodation(new ArrayList<>());
                mockResponse.setTransportation(new ArrayList<>());

                when(travelPlanService.createTravelPlan(any(TravelPlanRequest.class), any())).thenReturn(mockResponse);

                // When & Then
                mockMvc.perform(post("/api/travel/plan")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("New Travel Plan"))
                                .andExpect(jsonPath("$.country").value("Japan"))
                                .andExpect(jsonPath("$.city").value("Tokyo"))
                                .andExpect(jsonPath("$.itineraries.length()").value(3))
                                .andExpect(jsonPath("$.itineraries[0].location").value("도쿄 타워"))
                                .andExpect(jsonPath("$.itineraries[1].location").value("시부야"))
                                .andExpect(jsonPath("$.itineraries[2].location").value("하라주쿠"));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com")
        public void testGetTravelPlanList() throws Exception {
                // Given
                List<ItineraryDTO> itineraries = new ArrayList<>();
                itineraries.add(ItineraryDTO.builder()
                                .id(1L)
                                .location("남산 타워")
                                .date(1)
                                .activities(new ArrayList<>())
                                .lunch(null)
                                .dinner(null)
                                .build());
                itineraries.add(ItineraryDTO.builder()
                                .id(2L)
                                .location("홍대입구")
                                .date(2)
                                .activities(new ArrayList<>())
                                .lunch(null)
                                .dinner(null)
                                .build());

                List<TravelPlanDTO> mockResponse = new ArrayList<>();
                TravelPlanDTO dto = new TravelPlanDTO();
                dto.setId(testTravelPlan.getId());
                dto.setTitle(testTravelPlan.getTitle());
                dto.setCountry(testTravelPlan.getCountry());
                dto.setCity(testTravelPlan.getCity());
                dto.setStartDate(testTravelPlan.getStartDate());
                dto.setEndDate(testTravelPlan.getEndDate());
                dto.setItineraries(itineraries);
                dto.setSeason(testTravelPlan.getSeason());
                dto.setTravelStyles(testTravelPlan.getTravelStyles().stream()
                                .map(TravelStyleDTO::fromEntity)
                                .collect(Collectors.toList()));
                dto.setBudget(testTravelPlan.getBudget());
                dto.setInterests(testTravelPlan.getInterests().stream()
                                .map(InterestDTO::fromEntity)
                                .collect(Collectors.toList()));
                dto.setAccommodation(testTravelPlan.getAccommodationTypes().stream()
                                .map(AccommodationTypeDTO::fromEntity)
                                .collect(Collectors.toList()));
                dto.setTransportation(testTravelPlan.getTransportationTypes().stream()
                                .map(TransportationTypeDTO::fromEntity)
                                .collect(Collectors.toList()));
                mockResponse.add(dto);

                when(travelPlanService.getTravelPlanList(any())).thenReturn(mockResponse);

                // When & Then
                mockMvc.perform(get("/api/travel/plans"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(testTravelPlan.getId()))
                                .andExpect(jsonPath("$[0].title").value(testTravelPlan.getTitle()))
                                .andExpect(jsonPath("$[0].itineraries.length()").value(2))
                                .andExpect(jsonPath("$[0].itineraries[0].location").value("남산 타워"))
                                .andExpect(jsonPath("$[0].itineraries[1].location").value("홍대입구"));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com")
        public void testGetTravelPlanDetail() throws Exception {
                // Given
                List<ItineraryDTO> itineraries = new ArrayList<>();
                itineraries.add(ItineraryDTO.builder()
                                .id(1L)
                                .location("강남")
                                .date(1)
                                .activities(new ArrayList<>())
                                .lunch(null)
                                .dinner(null)
                                .build());
                itineraries.add(ItineraryDTO.builder()
                                .id(2L)
                                .location("잠실")
                                .date(2)
                                .activities(new ArrayList<>())
                                .lunch(null)
                                .dinner(null)
                                .build());
                itineraries.add(ItineraryDTO.builder()
                                .id(3L)
                                .location("송파")
                                .date(3)
                                .activities(new ArrayList<>())
                                .lunch(null)
                                .dinner(null)
                                .build());

                TravelPlanDTO mockResponse = new TravelPlanDTO();
                mockResponse.setId(testTravelPlan.getId());
                mockResponse.setTitle(testTravelPlan.getTitle());
                mockResponse.setCountry(testTravelPlan.getCountry());
                mockResponse.setCity(testTravelPlan.getCity());
                mockResponse.setStartDate(testTravelPlan.getStartDate());
                mockResponse.setEndDate(testTravelPlan.getEndDate());
                mockResponse.setItineraries(itineraries);
                mockResponse.setSeason(testTravelPlan.getSeason());
                mockResponse.setTravelStyles(testTravelPlan.getTravelStyles().stream()
                                .map(TravelStyleDTO::fromEntity)
                                .collect(Collectors.toList()));
                mockResponse.setBudget(testTravelPlan.getBudget());
                mockResponse.setInterests(testTravelPlan.getInterests().stream()
                                .map(InterestDTO::fromEntity)
                                .collect(Collectors.toList()));
                mockResponse.setAccommodation(testTravelPlan.getAccommodationTypes().stream()
                                .map(AccommodationTypeDTO::fromEntity)
                                .collect(Collectors.toList()));
                mockResponse.setTransportation(testTravelPlan.getTransportationTypes().stream()
                                .map(TransportationTypeDTO::fromEntity)
                                .collect(Collectors.toList()));

                when(travelPlanService.getTravelPlanDetail(any(Long.class), any())).thenReturn(mockResponse);

                // When & Then
                mockMvc.perform(get("/api/travel/plan/{id}", testTravelPlan.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(testTravelPlan.getId()))
                                .andExpect(jsonPath("$.title").value(testTravelPlan.getTitle()))
                                .andExpect(jsonPath("$.itineraries.length()").value(3))
                                .andExpect(jsonPath("$.itineraries[0].location").value("강남"))
                                .andExpect(jsonPath("$.itineraries[1].location").value("잠실"))
                                .andExpect(jsonPath("$.itineraries[2].location").value("송파"));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com")
        public void testUpdateTravelPlan() throws Exception {
                // Given
                TravelPlanUpdateRequest updateRequest = new TravelPlanUpdateRequest();
                updateRequest.setTitle("Updated Travel Plan");
                updateRequest.setStartDate(LocalDate.now().plusDays(2));
                updateRequest.setEndDate(LocalDate.now().plusDays(6));

                TravelPlanDTO mockResponse = new TravelPlanDTO();
                mockResponse.setId(testTravelPlan.getId());
                mockResponse.setTitle("Updated Travel Plan");
                mockResponse.setCountry(testTravelPlan.getCountry());
                mockResponse.setCity(testTravelPlan.getCity());
                mockResponse.setStartDate(LocalDate.now().plusDays(2));
                mockResponse.setEndDate(LocalDate.now().plusDays(6));
                mockResponse.setSeason(testTravelPlan.getSeason());
                mockResponse.setTravelStyles(testTravelPlan.getTravelStyles().stream()
                                .map(TravelStyleDTO::fromEntity)
                                .collect(Collectors.toList()));
                mockResponse.setBudget(testTravelPlan.getBudget());
                mockResponse.setInterests(testTravelPlan.getInterests().stream()
                                .map(InterestDTO::fromEntity)
                                .collect(Collectors.toList()));
                mockResponse.setAccommodation(testTravelPlan.getAccommodationTypes().stream()
                                .map(AccommodationTypeDTO::fromEntity)
                                .collect(Collectors.toList()));
                mockResponse.setTransportation(testTravelPlan.getTransportationTypes().stream()
                                .map(TransportationTypeDTO::fromEntity)
                                .collect(Collectors.toList()));

                when(travelPlanService.updateTravelPlan(any(Long.class), any(TravelPlanUpdateRequest.class), any()))
                                .thenReturn(mockResponse);

                // When & Then
                mockMvc.perform(put("/api/travel/plan/{id}", testTravelPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Updated Travel Plan"));
        }

        @Test
        @WithMockCustomUser(email = "test@example.com")
        public void testDeleteTravelPlan() throws Exception {
                // When & Then
                mockMvc.perform(delete("/api/travel/plan/{id}", testTravelPlan.getId()))
                                .andExpect(status().isNoContent());
        }
}