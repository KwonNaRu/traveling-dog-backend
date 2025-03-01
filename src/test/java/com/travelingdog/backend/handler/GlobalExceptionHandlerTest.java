package com.travelingdog.backend.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelingdog.backend.exception.DuplicateEmailException;
import com.travelingdog.backend.exception.ExternalApiException;
import com.travelingdog.backend.exception.InvalidRequestException;
import com.travelingdog.backend.exception.ResourceNotFoundException;

@Tag("unit")
public class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private TestController testController;

    @BeforeEach
    void setUp() {
        testController = new TestController();
        mockMvc = MockMvcBuilders.standaloneSetup(testController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("DuplicateEmailException 처리 테스트")
    void handleDuplicateEmailException() throws Exception {
        mockMvc.perform(get("/test/duplicate-email")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("이메일 중복 오류"))
                .andExpect(jsonPath("$.errors.email").value("이미 가입된 이메일입니다."));
    }

    @Test
    @DisplayName("ResourceNotFoundException 처리 테스트")
    void handleResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/resource-not-found")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.errors.resource").value("요청한 여행 계획을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("InvalidRequestException 처리 테스트")
    void handleInvalidRequestException() throws Exception {
        mockMvc.perform(get("/test/invalid-request")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.errors.request").value("여행 날짜가 유효하지 않습니다."));
    }

    @Test
    @DisplayName("ExternalApiException 처리 테스트")
    void handleExternalApiException() throws Exception {
        mockMvc.perform(get("/test/external-api-error")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_API_ERROR"))
                .andExpect(jsonPath("$.message").value("외부 API 오류"))
                .andExpect(jsonPath("$.errors.api").value("OpenAI API 호출 중 오류가 발생했습니다."));
    }

    // 테스트용 컨트롤러
    @RestController
    @RequestMapping("/test")
    private static class TestController {

        @GetMapping("/duplicate-email")
        public void throwDuplicateEmailException() {
            throw new DuplicateEmailException();
        }

        @GetMapping("/resource-not-found")
        public void throwResourceNotFoundException() {
            throw new ResourceNotFoundException("여행 계획", "요청한 여행 계획을 찾을 수 없습니다.");
        }

        @GetMapping("/invalid-request")
        public void throwInvalidRequestException() {
            throw new InvalidRequestException("여행 날짜가 유효하지 않습니다.");
        }

        @GetMapping("/external-api-error")
        public void throwExternalApiException() {
            throw new ExternalApiException("OpenAI API 호출 중 오류가 발생했습니다.");
        }
    }
}