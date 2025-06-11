package com.travelingdog.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.config.JpaAuditingConfigTest;
import com.travelingdog.backend.model.FailedGptResponse;

@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
@Import(JpaAuditingConfigTest.class)
public class FailedGptResponseRepositoryTest {

    @Autowired
    private FailedGptResponseRepository failedGptResponseRepository;

    @Test
    @DisplayName("실패한 GPT 응답 저장 및 조회 테스트")
    public void testSaveAndFindFailedGptResponse() {
        // Given
        String prompt = "제주도 3일 여행 계획 작성해줘";
        String response = "{\"error\": \"내용 파싱 실패\"}";
        String errorMessage = "잘못된 JSON 형식";
        LocalDateTime timestamp = LocalDateTime.now();

        FailedGptResponse failedResponse = FailedGptResponse.builder()
                .prompt(prompt)
                .response(response)
                .errorMessage(errorMessage)
                .timestamp(timestamp)
                .build();

        // When
        FailedGptResponse savedResponse = failedGptResponseRepository.save(failedResponse);

        // Then
        assertThat(savedResponse).isNotNull();
        assertThat(savedResponse.getId()).isNotNull();

        Optional<FailedGptResponse> foundResponse = failedGptResponseRepository.findById(savedResponse.getId());
        assertThat(foundResponse).isPresent();
        assertThat(foundResponse.get().getPrompt()).isEqualTo(prompt);
        assertThat(foundResponse.get().getResponse()).isEqualTo(response);
        assertThat(foundResponse.get().getErrorMessage()).isEqualTo(errorMessage);
        assertThat(foundResponse.get().getTimestamp()).isEqualToIgnoringNanos(timestamp);
    }

    @Test
    @DisplayName("실패한 GPT 응답 업데이트 테스트")
    public void testUpdateFailedGptResponse() {
        // Given
        FailedGptResponse failedResponse = FailedGptResponse.builder()
                .prompt("원래 프롬프트")
                .response("원래 응답")
                .errorMessage("원래 에러 메시지")
                .timestamp(LocalDateTime.now())
                .build();

        FailedGptResponse savedResponse = failedGptResponseRepository.save(failedResponse);

        // When
        savedResponse.setErrorMessage("업데이트된 에러 메시지");
        failedGptResponseRepository.save(savedResponse);

        // Then
        FailedGptResponse updatedResponse = failedGptResponseRepository.findById(savedResponse.getId()).orElseThrow();
        assertThat(updatedResponse.getErrorMessage()).isEqualTo("업데이트된 에러 메시지");
    }

    @Test
    @DisplayName("실패한 GPT 응답 삭제 테스트")
    public void testDeleteFailedGptResponse() {
        // Given
        FailedGptResponse failedResponse = FailedGptResponse.builder()
                .prompt("삭제할 프롬프트")
                .response("삭제할 응답")
                .errorMessage("삭제할 에러 메시지")
                .timestamp(LocalDateTime.now())
                .build();

        FailedGptResponse savedResponse = failedGptResponseRepository.save(failedResponse);
        Long responseId = savedResponse.getId();

        // When
        failedGptResponseRepository.deleteById(responseId);

        // Then
        assertThat(failedGptResponseRepository.findById(responseId)).isEmpty();
    }

    @Test
    @DisplayName("여러 실패한 GPT 응답 저장 및 모두 찾기 테스트")
    public void testSaveMultipleResponsesAndFindAll() {
        // Given
        failedGptResponseRepository.deleteAll(); // 기존 데이터 삭제

        FailedGptResponse response1 = FailedGptResponse.builder()
                .prompt("프롬프트1")
                .response("응답1")
                .errorMessage("에러1")
                .timestamp(LocalDateTime.now().minusDays(2))
                .build();

        FailedGptResponse response2 = FailedGptResponse.builder()
                .prompt("프롬프트2")
                .response("응답2")
                .errorMessage("에러2")
                .timestamp(LocalDateTime.now().minusDays(1))
                .build();

        FailedGptResponse response3 = FailedGptResponse.builder()
                .prompt("프롬프트3")
                .response("응답3")
                .errorMessage("에러3")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        failedGptResponseRepository.saveAll(List.of(response1, response2, response3));

        // Then
        List<FailedGptResponse> allResponses = failedGptResponseRepository.findAll();
        assertThat(allResponses).hasSize(3);
        assertThat(allResponses).extracting("prompt")
                .containsExactlyInAnyOrder("프롬프트1", "프롬프트2", "프롬프트3");
    }
}