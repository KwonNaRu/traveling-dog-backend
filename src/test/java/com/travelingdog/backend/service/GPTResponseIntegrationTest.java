package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.AIRecommendedItineraryDTO;
import com.travelingdog.backend.dto.AIRecommendedTravelPlanDTO;
import com.travelingdog.backend.dto.gemini.GeminiCandidate;
import com.travelingdog.backend.dto.gemini.GeminiContent;
import com.travelingdog.backend.dto.gemini.GeminiPart;
import com.travelingdog.backend.dto.gemini.GeminiRequest;
import com.travelingdog.backend.dto.gemini.GeminiResponse;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.model.Itinerary;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.ItineraryRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;

/**
 * GPT 응답 처리 통합 테스트
 *
 * 이 테스트 클래스는 OpenAI GPT API와의 통합을 테스트합니다. 여행 계획 생성 요청에 대한 GPT 응답을 처리하고, 이를 통해
 * 여행 일정을 생성하는 전체 프로세스를 검증합니다.
 *
 * 주요 테스트 대상: 1. GPT 응답 JSON 파싱 기능 2. 파싱된 데이터를 Itinerary 객체로 변환 3. 여행 계획과의 통합
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@Transactional
public class GPTResponseIntegrationTest {

        @Autowired
        private TravelPlanService tripPlanService;

        @Autowired
        private GptResponseHandler gptResponseHandler;

        @Autowired
        private TravelPlanRepository travelPlanRepository;

        @Autowired
        private ItineraryRepository itineraryRepository;

        @Autowired
        private UserRepository userRepository;

        @MockBean
        private RestClient restClient;

        private TravelPlanRequest request;
        private GeminiResponse mockResponse;
        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private ObjectMapper objectMapper = new ObjectMapper();
        private LocalDate today;
        private User user;

        /**
         * 각 테스트 실행 전 환경 설정
         *
         * 1. 테스트용 여행 계획 요청 데이터 생성 2. 모의 GPT 응답 데이터 설정 3. RestClient 모킹 설정: OpenAI
         * API 호출을 시뮬레이션
         *
         * 이 설정을 통해 실제 OpenAI API를 호출하지 않고도 GPT 응답 처리 로직을 테스트할 수 있습니다.
         */
        @BeforeEach
        void setUp() {
                // 데이터베이스 초기화
                userRepository.deleteAll();
                travelPlanRepository.deleteAll();
                itineraryRepository.deleteAll();

                // 테스트 요청 데이터 설정
                today = LocalDate.now();
                LocalDate endDate = today.plusDays(3);

                request = new TravelPlanRequest();
                request.setCity("제주시");
                request.setStartDate(today);
                request.setEndDate(endDate);
                request.setTravelStyle("해변, 자연 풍경 감상");
                request.setBudget("100만원");
                request.setInterests("맛집, 자연");
                request.setAccommodation("호텔");
                request.setTransportation("렌터카");

                // 테스트용 사용자 생성
                user = User.builder()
                                .nickname("dogLover")
                                .password("password123")
                                .email("dogLover@example.com")
                                .build();
                userRepository.save(user);

                // 모의 GPT 응답 데이터 설정
                mockResponse = new GeminiResponse();
                GeminiCandidate candidate = new GeminiCandidate();
                GeminiContent content = new GeminiContent();
                List<GeminiPart> parts = new ArrayList<>();

                // 여러 장소를 포함하는 JSON 응답 생성
                String jsonResponse = createMockGptResponse(today);
                parts.add(GeminiPart.builder()
                                .text(jsonResponse)
                                .build());
                content.setParts(parts);
                candidate.setContent(content);

                List<GeminiCandidate> candidates = new ArrayList<>();
                candidates.add(candidate);
                mockResponse.setCandidates(candidates);

                // RestClient 모킹 설정
                RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
                RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
                ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);

                when(restClient.post()).thenReturn(requestBodyUriSpec);
                when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
                when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
                when(requestBodySpec.body(any(GeminiRequest.class))).thenReturn(requestBodySpec);
                when(requestBodySpec.retrieve()).thenReturn(responseSpec);
                when(responseSpec.body(GeminiResponse.class)).thenReturn(mockResponse);
        }

        /**
         * 모의 GPT 응답 JSON 생성 헬퍼 메소드
         *
         * 이 메소드는 테스트에 사용할 모의 GPT 응답 JSON을 생성합니다. 서울의 주요 관광지 정보와 방문 날짜를 포함한 JSON 배열을
         * 생성합니다.
         *
         * @param startDate 여행 시작 날짜
         * @return GPT 응답 형식의 JSON 문자열
         */
        private String createMockGptResponse(LocalDate startDate) {
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{");
                jsonBuilder.append("\"trip_name\":\"제주도 3박 4일 여행\",");
                jsonBuilder.append("\"start_date\":\"").append(startDate.format(formatter)).append("\",");
                jsonBuilder.append("\"end_date\":\"").append(startDate.plusDays(3).format(formatter)).append("\",");
                jsonBuilder.append("\"travel_style\":[\"해변\",\"자연 풍경 감상\"],");
                jsonBuilder.append("\"budget\":\"100만원\",");
                jsonBuilder.append("\"country\":\"한국\",");
                jsonBuilder.append("\"destination\":\"제주시\",");
                jsonBuilder.append("\"interests\":[\"맛집\",\"자연\"],");
                jsonBuilder.append("\"accommodation\":[\"호텔\"],");
                jsonBuilder.append("\"transportation\":[\"렌터카\"],");
                jsonBuilder.append("\"itinerary\":[");

                // 첫째 날 일정
                jsonBuilder.append("{\"date\":1,\"location\":\"성산일출봉\",");
                jsonBuilder.append("\"activities\":[");
                jsonBuilder.append(
                                "{\"title\":\"성산일출봉 등반\",\"location_name\":\"Test Location Name\",\"description\":\"제주도의 상징적인 화산 등반\"},");
                jsonBuilder.append(
                                "{\"title\":\"우도 자전거 투어\",\"location_name\":\"Test Location Name\",\"description\":\"우도 섬 자전거 투어\"}");
                jsonBuilder.append("]");
                jsonBuilder.append("},");

                // 둘째 날 일정
                jsonBuilder.append("{\"date\":2,\"location\":\"만장굴\",");
                jsonBuilder.append("\"activities\":[");
                jsonBuilder.append(
                                "{\"title\":\"만장굴 탐험\",\"location_name\":\"Test Location Name\",\"description\":\"제주도의 대표적인 용암동굴 탐험\"}");
                jsonBuilder.append("]");
                jsonBuilder.append("}");
                jsonBuilder.append("],");

                jsonBuilder.append("\"restaurant_recommendations\":[");
                jsonBuilder.append(
                                "{\"location_name\":\"제주 흑돼지 맛집\",\"description\":\"제주 전통 흑돼지 구이 맛집\"}");
                jsonBuilder.append("],");
                jsonBuilder.append("\"accommodation_recommendations\":[");
                jsonBuilder.append(
                                "{\"location_name\":\"제주 호텔\",\"description\":\"제주 호텔\"}");
                jsonBuilder.append("],");
                jsonBuilder.append("\"transportation_tips\":\"제주도 내 렌터카 대여 추천\"");
                jsonBuilder.append("}");
                return jsonBuilder.toString();
        }

        /**
         * GPT 응답 JSON 파싱 기능 테스트
         *
         * 이 테스트는 GptResponseHandler가 유효한 JSON 형식의 GPT 응답을 올바르게 파싱하는지 검증합니다.
         *
         * 테스트 과정: 1. 유효한 JSON 형식의 GPT 응답 생성 2. GptResponseHandler를 사용하여 JSON 파싱 3.
         * 결과 검증: 파싱된 객체 수, 객체 속성 값
         *
         * 이 테스트는 GPT 응답 파싱의 정확성을 검증합니다.
         */
        @Test
        @DisplayName("GPT 응답 JSON 파싱 기능 테스트")
        void testGptResponseHandlerParseValidJson() {
                // 인증 설정
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Given
                String validJson = createMockGptResponse(today);

                // When
                AIRecommendedTravelPlanDTO result = gptResponseHandler.parseGptResponse(validJson);

                // Then
                assertNotNull(result);
                assertEquals("제주도 3박 4일 여행", result.getTripName());
                assertEquals(2, result.getItinerary().size());

                // 첫째 날 일정 검증
                AIRecommendedItineraryDTO firstDay = result.getItinerary().get(0);
                assertEquals(1, firstDay.getDate());
                assertEquals("성산일출봉", firstDay.getLocation());
                assertEquals(2, firstDay.getActivities().size());
                assertEquals("성산일출봉 등반", firstDay.getActivities().get(0).getTitle());
                assertEquals("우도 자전거 투어", firstDay.getActivities().get(1).getTitle());

                // 둘째 날 일정 검증
                AIRecommendedItineraryDTO secondDay = result.getItinerary().get(1);
                assertEquals(2, secondDay.getDate());
                assertEquals("만장굴", secondDay.getLocation());
                assertEquals(1, secondDay.getActivities().size());
                assertEquals("만장굴 탐험", secondDay.getActivities().get(0).getTitle());

                // 테스트 종료 후 SecurityContext 정리
                SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("여행 계획 생성 통합 테스트")
        void testCreateTravelPlanWithItineraries() {
                // 인증 설정
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // When
                TravelPlanDTO createdPlanDTO = tripPlanService.createTravelPlan(request, user);
                TravelPlan travelPlan = travelPlanRepository.findById(createdPlanDTO.getId()).orElse(null);

                // Then
                assertNotNull(travelPlan);
                assertEquals("제주도 3박 4일 여행", travelPlan.getTitle());
                assertEquals("제주시", travelPlan.getCity());
                assertEquals(today, travelPlan.getStartDate());
                assertEquals(today.plusDays(3), travelPlan.getEndDate());

                // 일정 검증
                List<Itinerary> itineraries = itineraryRepository
                                .findAllByTravelPlanIdOrderByDateAsc(travelPlan.getId());
                assertEquals(2, itineraries.size());

                // 첫째 날 일정 검증
                Itinerary firstDay = itineraries.get(0);
                assertEquals(1, firstDay.getDate());
                assertEquals("성산일출봉", firstDay.getLocation());
                assertEquals(2, firstDay.getActivities().size());
                assertEquals("성산일출봉 등반", firstDay.getActivities().get(0).getTitle());
                assertEquals("우도 자전거 투어", firstDay.getActivities().get(1).getTitle());

                // 둘째 날 일정 검증
                Itinerary secondDay = itineraries.get(1);
                assertEquals(2, secondDay.getDate());
                assertEquals("만장굴", secondDay.getLocation());
                assertEquals(1, secondDay.getActivities().size());
                assertEquals("만장굴 탐험", secondDay.getActivities().get(0).getTitle());

                // 테스트 종료 후 SecurityContext 정리
                SecurityContextHolder.clearContext();
        }

        @AfterEach
        public void cleanup() {
                // 테스트 후 SecurityContext 정리
                SecurityContextHolder.clearContext();

                // 데이터 정리 (외래 키 제약 조건을 고려한 삭제 순서)
                itineraryRepository.deleteAll();
                travelPlanRepository.deleteAll();
                userRepository.deleteAll();
        }
}
