package com.travelingdog.backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.AIChatMessage;
import com.travelingdog.backend.dto.AIChatRequest;
import com.travelingdog.backend.dto.AIChatResponse;
import com.travelingdog.backend.dto.AIChatResponse.Choice;
import com.travelingdog.backend.dto.AIRecommendedLocationDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.model.TravelLocation;

/**
 * GPT 응답 처리 통합 테스트
 *
 * 이 테스트 클래스는 OpenAI GPT API와의 통합을 테스트합니다. 여행 계획 생성 요청에 대한 GPT 응답을 처리하고, 이를 통해
 * 여행 위치 목록을 생성하는 전체 프로세스를 검증합니다.
 *
 * 주요 테스트 대상: 1. GPT 응답 JSON 파싱 기능 2. 파싱된 데이터를 TravelLocation 객체로 변환 3. 경로 최적화
 * 서비스와의 통합
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
public class GPTResponseIntegrationTest {

    @Autowired
    private TravelPlanService tripPlanService;

    @Autowired
    private GptResponseHandler gptResponseHandler;

    @MockBean
    private RestClient restClient;

    @MockBean
    private RouteOptimizationService routeOptimizationService;

    private TravelPlanRequest request;
    private AIChatResponse mockResponse;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private ObjectMapper objectMapper = new ObjectMapper();
    private LocalDate today;

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
        request.setCountry("South Korea");
        request.setCity("Seoul");
        request.setStartDate(today);
        request.setEndDate(endDate);

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
        jsonBuilder.append("[");

        // 첫째 날 장소들
        jsonBuilder.append(
                "{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770,\"availableDate\":\"")
                .append(startDate.format(formatter)).append("\"},");
        jsonBuilder.append(
                "{\"name\":\"Insadong\",\"latitude\":37.5746,\"longitude\":126.9850,\"availableDate\":\"")
                .append(startDate.format(formatter)).append("\"},");

        // 둘째 날 장소들
        LocalDate secondDay = startDate.plusDays(1);
        jsonBuilder
                .append("{\"name\":\"Namsan Tower\",\"latitude\":37.5512,\"longitude\":126.9882,\"availableDate\":\"")
                .append(secondDay.format(formatter)).append("\"},");
        jsonBuilder.append(
                "{\"name\":\"Myeongdong\",\"latitude\":37.5635,\"longitude\":126.9850,\"availableDate\":\"")
                .append(secondDay.format(formatter)).append("\"}");

        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    /**
     * 모의 여행 위치 객체 생성 헬퍼 메소드
     *
     * 이 메소드는 테스트에 사용할 모의 TravelLocation 객체 리스트를 생성합니다. 서울의 주요 관광지 정보와 방문 날짜, 순서
     * 등을 포함한 객체를 생성합니다.
     *
     * @return 모의 TravelLocation 객체 리스트
     */
    private List<TravelLocation> createMockLocations() {
        List<TravelLocation> locations = new ArrayList<>();

        // 첫째 날 장소들
        TravelLocation location1 = new TravelLocation();
        location1.setPlaceName("Gyeongbokgung Palace");
        location1.setCoordinates(126.9770, 37.5796);
        location1.setLocationOrder(0);
        location1.setDescription("경복궁");
        location1.setAvailableDate(today);
        locations.add(location1);

        TravelLocation location2 = new TravelLocation();
        location2.setPlaceName("Insadong");
        location2.setCoordinates(126.9850, 37.5746);
        location2.setLocationOrder(1);
        location2.setDescription("인사동");
        location2.setAvailableDate(today);
        locations.add(location2);

        // 둘째 날 장소들
        LocalDate secondDay = today.plusDays(1);
        TravelLocation location3 = new TravelLocation();
        location3.setPlaceName("Namsan Tower");
        location3.setCoordinates(126.9882, 37.5512);
        location3.setLocationOrder(2);
        location3.setDescription("남산타워");
        location3.setAvailableDate(secondDay);
        locations.add(location3);

        TravelLocation location4 = new TravelLocation();
        location4.setPlaceName("Myeongdong");
        location4.setCoordinates(126.9850, 37.5635);
        location4.setLocationOrder(3);
        location4.setDescription("명동");
        location4.setAvailableDate(secondDay);
        locations.add(location4);

        return locations;
    }

    /**
     * GPT 응답을 여행 계획 및 위치로 변환하는 기능 테스트
     *
     * 이 테스트는 TripPlanService가 GPT 응답을 처리하여 여행 위치 목록을 생성하는 전체 프로세스를 검증합니다.
     *
     * 테스트 과정: 1. 모의 GPT 응답 설정 2. 경로 최적화 서비스 모킹 3. 여행 계획 생성 요청 4. 결과 검증: 위치 수,
     * 위치 이름, 날짜별 그룹화
     *
     * 이 테스트는 GPT 응답 처리와 경로 최적화의 통합을 검증합니다.
     */
    @Test
    @DisplayName("GPT 응답을 여행 계획 및 위치로 변환하는 기능 테스트")
    void testGptResponseToTravelPlanAndLocations() throws JsonProcessingException {
        // 경로 최적화 서비스 모킹
        List<TravelLocation> mockLocations = createMockLocations();
        when(routeOptimizationService.optimizeRouteWithSimulatedAnnealing(any())).thenReturn(mockLocations);

        // When
        List<TravelLocation> result = tripPlanService.generateTripPlan(request);

        // Then
        assertNotNull(result);
        assertEquals(4, result.size());

        // 장소 이름 확인
        assertTrue(result.stream().anyMatch(loc -> loc.getPlaceName().equals("Gyeongbokgung Palace")));
        assertTrue(result.stream().anyMatch(loc -> loc.getPlaceName().equals("Insadong")));
        assertTrue(result.stream().anyMatch(loc -> loc.getPlaceName().equals("Namsan Tower")));
        assertTrue(result.stream().anyMatch(loc -> loc.getPlaceName().equals("Myeongdong")));

        // 날짜별 장소 확인
        LocalDate firstDay = request.getStartDate();
        LocalDate secondDay = firstDay.plusDays(1);

        List<TravelLocation> firstDayLocations = result.stream()
                .filter(loc -> loc.getAvailableDate().equals(firstDay))
                .toList();

        List<TravelLocation> secondDayLocations = result.stream()
                .filter(loc -> loc.getAvailableDate().equals(secondDay))
                .toList();

        assertEquals(2, firstDayLocations.size());
        assertEquals(2, secondDayLocations.size());
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
        String validJson = "[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770,\"availableDate\":\""
                + today.format(formatter) + "\"}]";

        // When
        List<AIRecommendedLocationDTO> result = gptResponseHandler.parseGptResponse(validJson);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gyeongbokgung Palace", result.get(0).getName());
    }
}
