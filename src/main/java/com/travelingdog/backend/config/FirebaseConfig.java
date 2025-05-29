package com.travelingdog.backend.config;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class FirebaseConfig {

    private static final String FIREBASE_SERVICE_ACCOUNT_PATH = "firebase-service-account.json";

    @Value("${FIREBASE_SERVICE_ACCOUNT}")
    private String firebaseServiceAccountJson;

    @PostConstruct
    public void initialize() {
        try {
            // Firebase 앱이 이미 초기화되었는지 확인
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount;

                // 환경 변수에서 서비스 계정 정보 확인
                if (firebaseServiceAccountJson != null && !firebaseServiceAccountJson.isEmpty()) {
                    log.info("환경 변수에서 Firebase 서비스 계정 정보를 사용합니다.");
                    serviceAccount = new ByteArrayInputStream(
                            firebaseServiceAccountJson.getBytes(StandardCharsets.UTF_8));
                } else {
                    // 환경 변수가 없으면 파일에서 로드 시도
                    log.info("파일에서 Firebase 서비스 계정 정보를 로드합니다: {}", FIREBASE_SERVICE_ACCOUNT_PATH);
                    try {
                        serviceAccount = new FileInputStream(FIREBASE_SERVICE_ACCOUNT_PATH);
                    } catch (IOException e) {
                        log.warn("파일에서 Firebase 서비스 계정 정보를 로드할 수 없습니다: {}", e.getMessage());
                        return;
                    }
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase 앱이 성공적으로 초기화되었습니다.");
            } else {
                log.info("Firebase 앱이 이미 초기화되어 있습니다.");
            }
        } catch (IOException e) {
            log.error("Firebase 초기화 중 오류 발생: {}", e.getMessage());
            // 서비스 계정 파일이 없는 경우에도 애플리케이션은 실행되도록 처리
            log.warn("Firebase 서비스를 사용할 수 없습니다. 소셜 로그인 기능이 제한될 수 있습니다.");
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        // Firebase 앱이 초기화되지 않았다면 null 반환
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase 앱이 초기화되지 않았습니다. FirebaseAuth를 사용할 수 없습니다.");
            return null;
        }
        return FirebaseAuth.getInstance();
    }
}