package com.travelingdog.backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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

/**
 * GPT 응답 처리기 단위 테스트
 *
 * 이 테스트 클래스는 GptResponseHandler의 다양한 기능을 단위 테스트합니다. 주요 테스트 대상: 1. GPT API 응답
 * JSON 파싱 기능 2. 다양한 형식의 JSON 응답 처리 능력 3. 오류 상황 처리 (잘못된 JSON, 필수 필드 누락 등) 4. 강화된
 * 프롬프트 생성 기능 5. 대체 응답 제공 기능
 *
 * 이 테스트는 외부 의존성을 모킹하여 GptResponseHandler의 로직만 독립적으로 테스트합니다.
 */
@Tag("unit")
public class GptResponseHandlerTest {

    private FailedGptResponseRepository failedResponseRepository;
    private GptResponseHandler gptResponseHandler;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private LocalDate today = LocalDate.now();

    /**
     * 각 테스트 실행 전 환경 설정
     *
     * 1. FailedGptResponseRepository 모킹 2. GptResponseHandler 인스턴스 생성 3. 리플렉션을
     * 사용하여 private 필드 설정
     *
     * 이 설정을 통해 실제 데이터베이스에 접근하지 않고도 GptResponseHandler의 로직을 테스트할 수 있습니다.
     */
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

    /**
     * 정상적인 JSON 배열 응답 파싱 테스트
     *
     * 이 테스트는 GptResponseHandler가 정상적인 형식의 JSON 배열 응답을 올바르게 파싱하여
     * AIRecommendedLocationDTO 객체 리스트로 변환하는지 검증합니다.
     *
     * 테스트 과정: 1. 유효한 JSON 배열 문자열 생성 2. GptResponseHandler를 사용하여 JSON 파싱 3. 결과
     * 검증: 파싱된 객체 수, 객체 속성 값
     */
    @Test
    @DisplayName("정상적인 JSON 배열 응답을 파싱할 수 있어야 한다")
    void testParseValidJsonArray() {
        // Given
        String validJson = "[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770}]";

        // When
        List<AIRecommendedLocationDTO> result = gptResponseHandler.parseGptResponse(validJson);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gyeongbokgung Palace", result.get(0).getName());
        assertEquals(37.5796, result.get(0).getLatitude());
        assertEquals(126.9770, result.get(0).getLongitude());
    }

    /**
     * 코드 블록으로 감싸진 JSON 응답 파싱 테스트
     *
     * 이 테스트는 GptResponseHandler가 마크다운 코드 블록(```json)으로 감싸진 JSON 응답을 올바르게 파싱할 수
     * 있는지 검증합니다.
     *
     * GPT API는 종종 코드 블록 형식으로 JSON을 반환하므로, 이를 처리할 수 있어야 합니다.
     *
     * 테스트 과정: 1. 코드 블록으로 감싸진 JSON 문자열 생성 2. GptResponseHandler를 사용하여 JSON 파싱 3.
     * 결과 검증: 파싱된 객체 수, 객체 속성 값
     */
    @Test
    @DisplayName("코드 블록으로 감싸진 JSON 응답을 파싱할 수 있어야 한다")
    void testParseJsonInCodeBlock() {
        // Given
        String jsonInCodeBlock = "```json\n[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770}]\n```";

        // When
        List<AIRecommendedLocationDTO> result = gptResponseHandler.parseGptResponse(jsonInCodeBlock);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gyeongbokgung Palace", result.get(0).getName());
    }

    /**
     * 추가 텍스트가 포함된 JSON 응답 파싱 테스트
     *
     * 이 테스트는 GptResponseHandler가 설명 텍스트와 함께 제공되는 JSON 응답을 올바르게 파싱할 수 있는지 검증합니다.
     *
     * GPT API는 종종 JSON 앞뒤로 설명 텍스트를 포함하여 응답하므로, JSON 부분만 정확히 추출하여 파싱할 수 있어야 합니다.
     *
     * 테스트 과정: 1. 추가 텍스트가 포함된 JSON 문자열 생성 2. GptResponseHandler를 사용하여 JSON 파싱 3.
     * 결과 검증: 파싱된 객체 수, 객체 속성 값
     */
    @Test
    @DisplayName("추가 텍스트가 포함된 JSON 응답을 파싱할 수 있어야 한다")
    void testParseJsonWithAdditionalText() {
        // Given
        String jsonWithText = "여기 서울의 추천 장소입니다:\n[{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770}]";

        // When
        List<AIRecommendedLocationDTO> result = gptResponseHandler.parseGptResponse(jsonWithText);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gyeongbokgung Palace", result.get(0).getName());
    }

    /**
     * 잘못된 형식의 JSON 응답 처리 테스트
     *
     * 이 테스트는 GptResponseHandler가 JSON 형식이 아닌 응답을 처리할 때 적절한 예외를 발생시키는지 검증합니다.
     *
     * 테스트 과정: 1. 잘못된 형식의 문자열 생성 2. GptResponseHandler를 사용하여 JSON 파싱 시도 3. 결과
     * 검증: ExternalApiException 예외 발생 및 메시지 확인
     */
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

    /**
     * 빈 JSON 배열 응답 처리 테스트
     *
     * 이 테스트는 GptResponseHandler가 빈 JSON 배열 응답을 처리할 때 적절한 예외를 발생시키는지 검증합니다.
     *
     * 테스트 과정: 1. 빈 JSON 배열 문자열 생성 2. GptResponseHandler를 사용하여 JSON 파싱 시도 3. 결과
     * 검증: ExternalApiException 예외 발생 및 메시지 확인
     */
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

    /**
     * 강화된 프롬프트 생성 테스트
     *
     * 이 테스트는 GptResponseHandler가 여행 계획 요청 정보를 기반으로 강화된 프롬프트를 올바르게 생성하는지 검증합니다.
     *
     * 강화된 프롬프트는 GPT API에 전송되어 더 정확한 응답을 유도하는 데 사용됩니다.
     *
     * 테스트 과정: 1. 여행 계획 요청 정보 설정 (국가, 도시, 시작일, 종료일) 2. GptResponseHandler를 사용하여
     * 강화된 프롬프트 생성 3. 결과 검증: 프롬프트에 필요한 정보와 형식이 포함되어 있는지 확인
     */
    @Test
    @DisplayName("강화된 프롬프트를 생성할 수 있어야 한다")
    void testCreateEnhancedPrompt() {
        // Given
        String country = "South Korea";
        String city = "Seoul";
        LocalDate startDate = today;
        LocalDate endDate = today.plusDays(3);

        // When
        String enhancedPrompt = gptResponseHandler.createEnhancedPrompt(country, city, startDate, endDate);

        // Then
        assertNotNull(enhancedPrompt);
        assertTrue(enhancedPrompt.contains("\"name\": \"장소명(문자열)\""));
        assertTrue(enhancedPrompt.contains("\"latitude\": 위도(숫자, 구글맵 기준)"));
        assertTrue(enhancedPrompt.contains("\"longitude\": 경도(숫자, 구글맵 기준)"));
        assertTrue(enhancedPrompt.contains(country));
        assertTrue(enhancedPrompt.contains(city));
        assertTrue(enhancedPrompt.contains(startDate.format(formatter)));
        assertTrue(enhancedPrompt.contains(endDate.format(formatter)));
    }

    /**
     * 대체 응답 제공 테스트
     *
     * 이 테스트는 GptResponseHandler가 GPT API 호출 실패 시 대체 응답을 올바르게 제공하는지 검증합니다.
     *
     * 대체 응답은 GPT API 호출이 실패하더라도 사용자에게 기본적인 여행 계획을 제공하기 위해 사용됩니다.
     *
     * 테스트 과정: 1. 여행 계획 요청 정보 설정 (국가, 도시, 시작일, 종료일) 2. GptResponseHandler를 사용하여
     * 대체 응답 생성 3. 결과 검증: 대체 응답의 유효성 확인 (비어있지 않음, 필수 필드 포함 등)
     */
    @Test
    @DisplayName("대체 응답을 제공할 수 있어야 한다")
    void testProvideFallbackResponse() {
        // Given
        String country = "South Korea";
        String city = "Seoul";
        LocalDate startDate = today;
        LocalDate endDate = today.plusDays(3);

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
        }
    }
}
