package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.AIRecommendedItineraryDTO;
import com.travelingdog.backend.dto.AIRecommendedTravelPlanDTO;
import com.travelingdog.backend.dto.gpt.AIChatMessage;
import com.travelingdog.backend.dto.gpt.AIChatRequest;
import com.travelingdog.backend.dto.gpt.AIChatResponse;
import com.travelingdog.backend.dto.gpt.AIChatResponse.Choice;
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
        private AIChatResponse mockResponse;
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
                // 테스트 요청 데이터 설정
                today = LocalDate.now();
                LocalDate endDate = today.plusDays(3);

                request = new TravelPlanRequest();
                request.setTitle("제주도 3박 4일 여행");
                request.setCountry("한국");
                request.setCity("제주시");
                request.setStartDate(today);
                request.setEndDate(endDate);
                request.setSeason("여름");
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
                mockResponse = new AIChatResponse();
                AIChatResponse.Choice choice = new AIChatResponse.Choice();
                AIChatMessage message = new AIChatMessage();

                // 여러 장소를 포함하는 JSON 응답 생성
                String jsonResponse = createMockGptResponse(today);
                message.setContent(jsonResponse);

                choice.setMessage(message);
                List<Choice> choices = new ArrayList<>();
                choices.add(choice);
                mockResponse.setChoices(choices);

                // RestClient 모킹 설정
                RequestBodySpec requestBodySpec = Mockito.mock(RequestBodySpec.class);
                RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
                ResponseSpec responseSpec = Mockito.mock(ResponseSpec.class);

                when(restClient.post()).thenReturn(requestBodyUriSpec);
                when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
                when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
                when(requestBodySpec.body(any(AIChatRequest.class))).thenReturn(requestBodySpec);
                when(requestBodySpec.retrieve()).thenReturn(responseSpec);
                when(responseSpec.body(AIChatResponse.class)).thenReturn(mockResponse);
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
                jsonBuilder.append("\"season\":\"여름\",");
                jsonBuilder.append("\"travel_style\":[\"해변\",\"자연 풍경 감상\"],");
                jsonBuilder.append("\"budget\":\"100만원\",");
                jsonBuilder.append("\"destination\":\"제주시\",");
                jsonBuilder.append("\"interests\":[\"맛집\",\"자연\"],");
                jsonBuilder.append("\"accommodation\":[\"호텔\"],");
                jsonBuilder.append("\"transportation\":[\"렌터카\"],");
                jsonBuilder.append("\"itinerary\":[");

                // 첫째 날 일정
                jsonBuilder.append("{\"day\":1,\"location\":\"성산일출봉\",");
                jsonBuilder.append("\"activities\":[");
                jsonBuilder.append(
                                "{\"name\":\"성산일출봉 등반\",\"latitude\":33.458,\"longitude\":126.939,\"description\":\"제주도의 상징적인 화산 등반\"},");
                jsonBuilder.append(
                                "{\"name\":\"우도 자전거 투어\",\"latitude\":33.506,\"longitude\":126.953,\"description\":\"우도 섬 자전거 투어\"}");
                jsonBuilder.append("],");
                jsonBuilder.append(
                                "\"lunch\":{\"name\":\"제주 흑돼지 맛집\",\"latitude\":33.499,\"longitude\":126.531,\"description\":\"제주 전통 흑돼지 구이 맛집\"},");
                jsonBuilder.append(
                                "\"dinner\":{\"name\":\"해녀의 집\",\"latitude\":33.248,\"longitude\":126.559,\"description\":\"신선한 해산물 요리\"}");
                jsonBuilder.append("},");

                // 둘째 날 일정
                jsonBuilder.append("{\"day\":2,\"location\":\"만장굴\",");
                jsonBuilder.append("\"activities\":[");
                jsonBuilder.append(
                                "{\"name\":\"만장굴 탐험\",\"latitude\":33.470,\"longitude\":126.786,\"description\":\"제주도의 대표적인 용암동굴 탐험\"}");
                jsonBuilder.append("],");
                jsonBuilder.append(
                                "\"lunch\":{\"name\":\"제주 전복 요리\",\"latitude\":33.450,\"longitude\":126.790,\"description\":\"신선한 전복 요리 맛집\"},");
                jsonBuilder.append(
                                "\"dinner\":{\"name\":\"제주 한라산 고기\",\"latitude\":33.460,\"longitude\":126.780,\"description\":\"한라산 고기 맛집\"}");
                jsonBuilder.append("}");

                jsonBuilder.append("]}");
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
                assertEquals(1, firstDay.getDay());
                assertEquals("성산일출봉", firstDay.getLocation());
                assertEquals(2, firstDay.getActivities().size());
                assertEquals("성산일출봉 등반", firstDay.getActivities().get(0).getName());
                assertEquals("우도 자전거 투어", firstDay.getActivities().get(1).getName());
                assertEquals("제주 흑돼지 맛집", firstDay.getLunch().getName());
                assertEquals("해녀의 집", firstDay.getDinner().getName());

                // 둘째 날 일정 검증
                AIRecommendedItineraryDTO secondDay = result.getItinerary().get(1);
                assertEquals(2, secondDay.getDay());
                assertEquals("만장굴", secondDay.getLocation());
                assertEquals(1, secondDay.getActivities().size());
                assertEquals("만장굴 탐험", secondDay.getActivities().get(0).getName());
                assertEquals("제주 전복 요리", secondDay.getLunch().getName());
                assertEquals("제주 한라산 고기", secondDay.getDinner().getName());
        }

        @Test
        @DisplayName("여행 계획 생성 통합 테스트")
        void testCreateTravelPlanWithItineraries() {
                // Given
                String validJson = createMockGptResponse(today);
                AIRecommendedTravelPlanDTO travelPlanDTO = gptResponseHandler.parseGptResponse(validJson);

                // When
                TravelPlanDTO createdPlanDTO = tripPlanService.createTravelPlan(request, user);
                TravelPlan travelPlan = travelPlanRepository.findById(createdPlanDTO.getId()).orElse(null);

                // Then
                assertNotNull(travelPlan);
                assertEquals("제주도 3박 4일 여행", travelPlan.getTitle());
                assertEquals("한국", travelPlan.getCountry());
                assertEquals("제주시", travelPlan.getCity());
                assertEquals(today, travelPlan.getStartDate());
                assertEquals(today.plusDays(3), travelPlan.getEndDate());

                // 일정 검증
                List<Itinerary> itineraries = itineraryRepository
                                .findAllByTravelPlanIdOrderByDayAsc(travelPlan.getId());
                assertEquals(2, itineraries.size());

                // 첫째 날 일정 검증
                Itinerary firstDay = itineraries.get(0);
                assertEquals(1, firstDay.getDay());
                assertEquals("성산일출봉", firstDay.getLocation());
                assertEquals(2, firstDay.getActivities().size());
                assertEquals("성산일출봉 등반", firstDay.getActivities().get(0).getName());
                assertEquals("우도 자전거 투어", firstDay.getActivities().get(1).getName());
                assertEquals("제주 흑돼지 맛집", firstDay.getLunch().getName());
                assertEquals("해녀의 집", firstDay.getDinner().getName());

                // 둘째 날 일정 검증
                Itinerary secondDay = itineraries.get(1);
                assertEquals(2, secondDay.getDay());
                assertEquals("만장굴", secondDay.getLocation());
                assertEquals(1, secondDay.getActivities().size());
                assertEquals("만장굴 탐험", secondDay.getActivities().get(0).getName());
                assertEquals("제주 전복 요리", secondDay.getLunch().getName());
                assertEquals("제주 한라산 고기", secondDay.getDinner().getName());
        }
}
