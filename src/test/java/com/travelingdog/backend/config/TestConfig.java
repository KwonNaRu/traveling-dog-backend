package com.travelingdog.backend.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 테스트 환경을 위한 설정 클래스
 * 테스트에 필요한 모의 객체(Mock)를 제공합니다.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public WebClient webClient() {
        return Mockito.mock(WebClient.class);
    }

    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
        WebClient mockWebClient = Mockito.mock(WebClient.class);
        Mockito.when(mockBuilder.build()).thenReturn(mockWebClient);
        return mockBuilder;
    }
}