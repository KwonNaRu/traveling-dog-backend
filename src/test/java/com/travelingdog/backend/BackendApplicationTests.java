package com.travelingdog.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.travelingdog.backend.config.FirebaseConfigTest;

import org.springframework.context.annotation.Import;

/**
 * 스프링 부트 애플리케이션 컨텍스트 로드 테스트
 * 
 * 이 테스트는 애플리케이션이 정상적으로 시작되는지 확인합니다.
 * 스프링 컨텍스트가 올바르게 로드되는지 검증하여 기본적인 애플리케이션 구성이 정상인지 테스트합니다.
 * 테스트 프로필을 사용하여 실제 운영 환경과 분리된 테스트 환경에서 실행됩니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(FirebaseConfigTest.class)
class BackendApplicationTests {

	/**
	 * 스프링 애플리케이션 컨텍스트 로드 테스트
	 * 
	 * 이 테스트는 스프링 부트 애플리케이션의 컨텍스트가 성공적으로 로드되는지 확인합니다.
	 * 모든 빈이 올바르게 생성되고 주입되는지 검증합니다.
	 */
	@Test
	void contextLoads() {
	}

}
