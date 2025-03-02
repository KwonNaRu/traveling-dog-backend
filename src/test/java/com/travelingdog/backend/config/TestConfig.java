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
 * 
 * 이 클래스는 테스트 환경에서 필요한 모의 객체(Mock)와 설정을 제공합니다.
 * 주요 기능:
 * 1. WebClient 모킹: 외부 API 호출을 시뮬레이션하기 위한 WebClient 모의 객체 제공
 * 2. WebClient.Builder 모킹: WebClient 빌더를 모의하여 테스트 중 WebClient 생성 제어
 * 
 * @Profile("test") 어노테이션을 통해 테스트 프로필에서만 활성화됩니다.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * 모의 WebClient 빈 제공
     * 
     * 이 빈은 테스트 중 실제 HTTP 요청을 보내지 않고 모의 응답을 반환하는
     * WebClient 객체를 제공합니다. 주로 Google Maps API와 같은 외부 API 호출을
     * 시뮬레이션하는 데 사용됩니다.
     * 
     * @return 모의 WebClient 객체
     */
    @Bean
    @Primary
    public WebClient webClient() {
        return Mockito.mock(WebClient.class);
    }

    /**
     * 모의 WebClient.Builder 빈 제공
     * 
     * 이 빈은 WebClient를 생성하는 빌더 객체를 모의합니다.
     * build() 메소드가 호출될 때 모의 WebClient를 반환하도록 설정되어 있습니다.
     * 
     * @return 모의 WebClient.Builder 객체
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder mockBuilder = Mockito.mock(WebClient.Builder.class);
        WebClient mockWebClient = Mockito.mock(WebClient.class);
        Mockito.when(mockBuilder.build()).thenReturn(mockWebClient);
        return mockBuilder;
    }
}