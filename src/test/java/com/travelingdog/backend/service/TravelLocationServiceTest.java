package com.travelingdog.backend.service;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * TravelLocationService 단위 테스트
 * 
 * 이 테스트 클래스는 TravelLocationService의 기능을 단위 테스트합니다.
 * 주요 테스트 대상:
 * 1. 여행 장소에 리뷰 및 평점 추가 기능
 * 2. 이름 또는 국가별 여행 장소 검색 기능
 * 
 * 이 테스트는 JPA 관련 컴포넌트만 로드하여 데이터 접근 계층을 테스트합니다.
 * 실제 데이터베이스 대신 인메모리 데이터베이스를 사용하여 테스트를 수행합니다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Tag("unit")
public class TravelLocationServiceTest {

    /*
     * 여행 위치에 리뷰와 평점을 추가할 수 있다.
     */

    /*
     * 여행 위치는 이름이나 국가로 검색할 수 있다.
     */

}
