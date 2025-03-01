package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelingdog.backend.dto.AIRecommendedLocationDTO;
import com.travelingdog.backend.exception.ExternalApiException;
import com.travelingdog.backend.repository.FailedGptResponseRepository;

@Tag("unit")
public class GptResponseHandlerTest {

    private FailedGptResponseRepository failedResponseRepository;
    private GptResponseHandler gptResponseHandler;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        // 모의 객체 생성
        failedResponseRepository = Mockito.mock(FailedGptResponseRepository.class);

        // 테스트 대상 객체 생성
        gptResponseHandler = new GptResponseHandler();

        // 리플렉션을 사용하여 private 필드 설정
        ReflectionTestUtils.setField(gptResponseHandler, "failedResponseRepository", failedResponseRepository);
        ReflectionTestUtils.setField(gptResponseHandler, "objectMapper", new ObjectMapper());
    }

    @Test
    @DisplayName("정상적인 JSON 배열 응답을 파싱할 수 있어야 한다")
    void testParseValidJsonArray() {
        // Given
        String validJson = "[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770,\"availableDate\":\""
                + today.format(formatter) + "\"}]";

        // When
        List<AIRecommendedLocationDTO> result = gptResponseHandler.parseGptResponse(validJson);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gyeongbokgung Palace", result.get(0).getName());
        assertEquals(37.5796, result.get(0).getLatitude());
        assertEquals(126.9770, result.get(0).getLongitude());
        assertEquals(today.format(formatter), result.get(0).getAvailableDate());
    }

    @Test
    @DisplayName("코드 블록으로 감싸진 JSON 응답을 파싱할 수 있어야 한다")
    void testParseJsonInCodeBlock() {
        // Given
        String jsonInCodeBlock = "```json\n[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770,\"availableDate\":\""
                + today.format(formatter) + "\"}]\n```";

        // When
        List<AIRecommendedLocationDTO> result = gptResponseHandler.parseGptResponse(jsonInCodeBlock);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gyeongbokgung Palace", result.get(0).getName());
    }

    @Test
    @DisplayName("추가 텍스트가 포함된 JSON 응답을 파싱할 수 있어야 한다")
    void testParseJsonWithAdditionalText() {
        // Given
        String jsonWithText = "여기 서울의 추천 장소입니다:\n[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770,\"availableDate\":\""
                + today.format(formatter) + "\"}]";

        // When
        List<AIRecommendedLocationDTO> result = gptResponseHandler.parseGptResponse(jsonWithText);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gyeongbokgung Palace", result.get(0).getName());
    }

    @Test
    @DisplayName("필수 필드가 누락된 JSON 응답을 처리할 수 있어야 한다")
    void testHandleMissingFields() {
        // Given
        String invalidJson = "[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770}]"; // availableDate
                                                                                                                  // 누락

        // When & Then
        Exception exception = assertThrows(ExternalApiException.class, () -> {
            gptResponseHandler.parseGptResponse(invalidJson);
        });

        assertTrue(exception.getMessage().contains("필수 필드가 누락"));
    }

    @Test
    @DisplayName("잘못된 형식의 JSON 응답을 처리할 수 있어야 한다")
    void testHandleInvalidJson() {
        // Given
        String invalidJson = "This is not a valid JSON response";

        // When & Then
        Exception exception = assertThrows(ExternalApiException.class, () -> {
            gptResponseHandler.parseGptResponse(invalidJson);
        });

        assertTrue(exception.getMessage().contains("JSON 파싱 실패"));
    }

    @Test
    @DisplayName("빈 JSON 배열 응답을 처리할 수 있어야 한다")
    void testHandleEmptyJsonArray() {
        // Given
        String emptyJson = "[]";

        // When & Then
        Exception exception = assertThrows(ExternalApiException.class, () -> {
            gptResponseHandler.parseGptResponse(emptyJson);
        });

        assertTrue(exception.getMessage().contains("빈 응답"));
    }

    @Test
    @DisplayName("강화된 프롬프트를 생성할 수 있어야 한다")
    void testCreateEnhancedPrompt() {
        // Given
        String country = "South Korea";
        String city = "Seoul";
        String startDate = today.format(formatter);
        String endDate = today.plusDays(3).format(formatter);

        // When
        String enhancedPrompt = gptResponseHandler.createEnhancedPrompt(country, city, startDate, endDate);

        // Then
        assertNotNull(enhancedPrompt);
        assertTrue(enhancedPrompt.contains("\"name\": \"장소명(문자열)\""));
        assertTrue(enhancedPrompt.contains("\"latitude\": 위도(숫자)"));
        assertTrue(enhancedPrompt.contains("\"longitude\": 경도(숫자)"));
        assertTrue(enhancedPrompt.contains("\"availableDate\": \"yyyy-MM-dd 형식의 날짜(문자열)\""));
        assertTrue(enhancedPrompt.contains(country));
        assertTrue(enhancedPrompt.contains(city));
        assertTrue(enhancedPrompt.contains(startDate));
        assertTrue(enhancedPrompt.contains(endDate));
    }

    @Test
    @DisplayName("대체 응답을 제공할 수 있어야 한다")
    void testProvideFallbackResponse() {
        // Given
        String country = "South Korea";
        String city = "Seoul";
        String startDate = today.format(formatter);
        String endDate = today.plusDays(3).format(formatter);

        // When
        List<AIRecommendedLocationDTO> fallbackResponse = gptResponseHandler.getFallbackResponse(country, city,
                startDate, endDate);

        // Then
        assertNotNull(fallbackResponse);
        assertFalse(fallbackResponse.isEmpty());
        // 대체 응답의 각 항목이 필수 필드를 모두 가지고 있는지 확인
        for (AIRecommendedLocationDTO location : fallbackResponse) {
            assertNotNull(location.getName());
            assertNotEquals(0.0, location.getLatitude());
            assertNotEquals(0.0, location.getLongitude());
            assertNotNull(location.getAvailableDate());
            // 날짜 형식 검증 추가
            try {
                LocalDate.parse(location.getAvailableDate(), formatter);
            } catch (Exception e) {
                fail("날짜 형식이 올바르지 않습니다: " + location.getAvailableDate());
            }
        }
    }
}