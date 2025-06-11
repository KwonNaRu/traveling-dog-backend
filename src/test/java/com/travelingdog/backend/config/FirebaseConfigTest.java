package com.travelingdog.backend.config;

import org.checkerframework.checker.units.qual.C;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.google.firebase.auth.FirebaseAuth;

/**
 * 테스트 환경에서 사용할 Firebase 설정
 * 실제 Firebase 연결 없이 테스트가 가능하도록 목 객체를 제공합니다.
 */
@TestConfiguration
@Profile("test")
public class FirebaseConfigTest {

    /**
     * 테스트용 FirebaseAuth 목 객체를 제공합니다.
     * 
     * @return FirebaseAuth 목 객체
     */
    @Bean
    @Primary
    public FirebaseAuth firebaseAuthTest() {
        return Mockito.mock(FirebaseAuth.class);
    }
}
