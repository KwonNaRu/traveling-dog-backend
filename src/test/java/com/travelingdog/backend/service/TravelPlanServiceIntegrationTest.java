package com.travelingdog.backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import com.travelingdog.backend.dto.AIChatMessage;
import com.travelingdog.backend.dto.AIChatRequest;
import com.travelingdog.backend.dto.AIChatResponse;
import com.travelingdog.backend.dto.AIChatResponse.Choice;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanUpdateRequest;
import com.travelingdog.backend.exception.ForbiddenResourceAccessException;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelLocationRepository;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;

/**
 * TravelPlanService 통합 테스트
 *
 * 이 테스트 클래스는 TravelPlanService의 기능을 실제 데이터베이스와 연동하여 통합 테스트합니다. 주요 테스트 대상: 1. 여행
 * 계획 업데이트 기능 2. 여행 장소 순서 변경 기능
 *
 * 이 테스트는 실제 데이터베이스를 사용하여 TravelPlanService의 전체 기능을 다른 컴포넌트들과의 상호작용을 포함하여
 * 테스트합니다.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Tag("integration")
public class TravelPlanServiceIntegrationTest {

    @Autowired
    private TravelPlanService travelPlanService;

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Autowired
    private TravelLocationRepository travelLocationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RouteOptimizationService routeOptimizationService;

    @MockBean
    private RestClient restClient;

    private User user;
    private TravelPlan travelPlan;
    private List<TravelLocation> travelLocations;
    private TravelPlanUpdateRequest updateRequest;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 각 테스트 실행 전 환경 설정
     *
     * 1. 테스트용 사용자 생성 및 저장 2. 테스트용 여행 계획 생성 및 저장 3. 테스트용 여행 장소 생성 및 저장
     *
     * 이 설정을 통해 각 테스트가 독립적인 환경에서 실행될 수 있도록 합니다.
     *
     * @Transactional 어노테이션으로 인해 각 테스트 후 데이터베이스 변경사항은 롤백됩니다.
     */
    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        user = User.builder()
                .nickname("testUser")
                .password("password123!")
                .email("test@example.com")
                .roles(new HashSet<>(Collections.singleton("ROLE_USER")))
                .build();
        userRepository.save(user);

        // 현재 날짜와 미래 날짜 설정
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(7);

        // 먼저 여행 계획 생성 (빈 리스트로 초기화)
        travelPlan = TravelPlan.builder()
                .country("South Korea")
                .city("Seoul")
                .user(user)
                .title("Test Travel Plan")
                .startDate(today) // 현재 날짜로 설정
                .endDate(futureDate) // 미래 날짜로 설정
                .travelLocations(new ArrayList<>()) // 빈 리스트로 초기화
                .isShared(true)
                .build();
        travelPlanRepository.save(travelPlan);

        // 그 다음 여행 장소 생성 및 연결
        travelLocations = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            TravelLocation location = TravelLocation.builder()
                    .placeName("Location " + (i + 1))
                    .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                            .createPoint(new Coordinate(37.5 + i * 0.1, 127.0 + i * 0.1)))
                    .locationOrder(i + 1)
                    .travelPlan(travelPlan) // 이미 생성된 travelPlan 참조
                    .availableDate(today.plusDays(i)) // 각 장소마다 다른 날짜 설정
                    .build();
            travelLocationRepository.save(location);
            travelLocations.add(location);
        }

        // 여행 장소 리스트 업데이트 (필요한 경우)
        travelPlan.setTravelLocations(travelLocations);

        // RestClient 모킹 설정
        AIChatResponse mockResponse = new AIChatResponse();
        List<Choice> choices = new ArrayList<>();
        Choice choice = new Choice();
        AIChatMessage message = new AIChatMessage();

        // Mock 장소 4개 생성
        String jsonResponse = createMockGptResponse(today);
        message.setContent(jsonResponse);

        choice.setMessage(message);
        choices.add(choice);
        mockResponse.setChoices(choices);

        RequestBodySpec requestBodySpec = mock(RequestBodySpec.class);
        RequestBodyUriSpec requestBodyUriSpec = mock(RequestBodyUriSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(AIChatRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AIChatResponse.class)).thenReturn(mockResponse);

        // 테스트 후 SecurityContext 정리를 위해
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    public void cleanup() {
        // 각 테스트 후 SecurityContext 정리
        SecurityContextHolder.clearContext();
    }

    /**
     * 여행 계획 생성 테스트
     *
     * 이 테스트는 TravelPlanService가 여행 계획을 올바르게 생성하는지 검증합니다.
     *
     * 테스트 과정: 1. 여행 계획 생성 요청 생성 (여행 계획 DTO) 2. TravelPlanService를 사용하여 여행 계획 생성
     * 3. 결과 검증: 생성된 여행 계획이 데이터베이스에 올바르게 저장되었는지 확인
     */
    @Test
    @DisplayName("여행 계획 생성 테스트")
    void testCreateTravelPlan() {
        // Given
        TravelPlanRequest request = new TravelPlanRequest();
        request.setTitle("Test Travel Plan");
        request.setCountry("South Korea");
        request.setCity("Seoul");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(7));
        request.setIsShared(true);

        // When
        TravelPlanDTO createdPlan = travelPlanService.createTravelPlan(request, user);

        // Then
        assertNotNull(createdPlan);
        assertEquals(request.getCountry(), createdPlan.getCountry());
        assertEquals(request.getCity(), createdPlan.getCity());
        assertEquals(request.getStartDate(), createdPlan.getStartDate());
        assertEquals(request.getEndDate(), createdPlan.getEndDate());

        // 생성된 여행 장소 확인
        assertNotNull(createdPlan.getTravelLocations());
        assertFalse(createdPlan.getTravelLocations().isEmpty());
        assertEquals(4, createdPlan.getTravelLocations().size());
    }

    /**
     * 여행 계획 조회 테스트
     *
     * 이 테스트는 TravelPlanService가 여행 계획을 올바르게 조회하는지 검증합니다.
     *
     * 테스트 과정: 1. 여행 계획 ID를 사용하여 여행 계획 조회 2. 결과 검증: 데이터베이스에 저장된 TravelLocation
     * 정보는 제외된 나의 여행 계획 리스트가 올바르게 반환되는지 확인
     */
    @Test
    @DisplayName("여행 계획 리스트 조회 테스트")
    void testGetTravelPlanList() {
        // 소유자로 인증 설정
        setAuthenticationUser(user);

        // Given
        TravelPlanRequest secondRequest = new TravelPlanRequest();
        secondRequest.setTitle("Test Travel Plan 2");
        secondRequest.setCountry("Japan");
        secondRequest.setCity("Tokyo");
        secondRequest.setStartDate(LocalDate.now());
        secondRequest.setEndDate(LocalDate.now().plusDays(7));
        secondRequest.setIsShared(false);

        TravelPlanDTO secondTravelPlan = travelPlanService.createTravelPlan(secondRequest, user);

        // When
        List<TravelPlanDTO> travelPlanList = travelPlanService.getTravelPlanList(user);

        // Then
        assertNotNull(travelPlanList);
        assertEquals(2, travelPlanList.size());
        assertEquals(travelPlan.getId(), travelPlanList.get(0).getId());
        assertEquals(secondTravelPlan.getId(), travelPlanList.get(1).getId());
    }

    /**
     * 여행 계획 상세 조회 테스트
     *
     * 이 테스트는 TravelPlanService가 여행 계획을 올바르게 조회하는지 검증합니다.
     *
     * 테스트 과정: 1. 여행 계획 ID를 사용하여 여행 계획 조회 2. 결과 검증: 데이터베이스에 저장된 나의 여행 계획 상세 정보가
     * 올바르게 반환되는지 확인
     */
    @Test
    @DisplayName("여행 계획 상세 조회 테스트")
    void testGetTravelPlanById() {
        // 소유자로 인증 설정
        setAuthenticationUser(user);

        // When
        TravelPlanDTO travelPlanDTO = travelPlanService.getTravelPlanDetail(travelPlan.getId(), user);

        // Then
        assertNotNull(travelPlanDTO);
        assertEquals(travelPlan.getId(), travelPlanDTO.getId());
        assertEquals(travelPlan.getTitle(), travelPlanDTO.getTitle());
        assertEquals(travelPlan.getStartDate(), travelPlanDTO.getStartDate());
        assertEquals(travelPlan.getEndDate(), travelPlanDTO.getEndDate());
    }

    /**
     * 여행 장소 순서 변경 테스트
     *
     * 이 테스트는 TravelPlanService가 여행 장소의 순서를 올바르게 변경하는지 검증합니다.
     *
     * 테스트 과정: 1. 여행 장소 순서 변경 요청 생성 (장소 ID와 새 순서) 2. TravelPlanService를 사용하여 순서
     * 변경 3. 결과 검증: 변경된 순서가 데이터베이스에 올바르게 반영되었는지 확인
     *
     * 이 테스트는 또한 중복된 순서가 있을 경우 DuplicateOrderException이 발생하는지도 검증합니다.
     *
     * 장소 순서 변경 로직: 1. 장소 ID와 새 순서를 입력받음 2. 해당 장소의 순서를 변경 3. 같은 여행 계획 내에 동일한 순서를
     * 가진 장소가 있으면 안 됨. 그렇기 때문에 순서를 최적화 해야함. 예를 들어, 장소 a, b, c, d, e가 1, 2, 3, 4,
     * 5 순서로 있고, a 장소의 순서를 3번째로 변경하면 일단 b, c, a, d, e 순서로 바뀌고 이후, 즉시 b, c, a 순서는
     * 고정하고 d와 e에 대해서 최적화 알고리즘을 적용하여 최적화 해야함. 최적화 알고리즘은 예를 들어, b, c, a는 정해졌으니까
     * a에서부터 장소 d와 e에 대해서 최적화 알고리즘을 적용해야함.
     */
    @Test
    @DisplayName("여행 장소 순서 변경 및 부분 최적화 테스트")
    void testChangeTravelLocationOrder() {
        // 소유자로 인증 설정
        setAuthenticationUser(user);

        // Given
        // 더 많은 장소를 추가하여 최적화 효과를 확인할 수 있도록 함
        // 기존 3개 장소에 2개 더 추가
        for (int i = 3; i < 5; i++) {
            TravelLocation location = TravelLocation.builder()
                    .placeName("Location " + (i + 1))
                    .coordinates(new GeometryFactory(new PrecisionModel(), 4326)
                            .createPoint(new Coordinate(37.5 + i * 0.1, 127.0 + i * 0.1)))
                    .locationOrder(i + 1)
                    .travelPlan(travelPlan)
                    .availableDate(LocalDate.now().plusDays(i)) // 각 장소마다 다른 날짜 설정
                    .build();
            travelLocationRepository.save(location);
            travelLocations.add(location);
        }

        // 순서 변경 전 원래 위치 저장 (a, b, c, d, e)
        List<TravelLocation> originalLocations = new ArrayList<>(travelLocations);

        // 첫 번째 장소(a, 원래 순서 1)를 3번 위치로 이동 -> 결과적으로 b, c, a, d, e 순서가 됨
        Long locationIdToMove = travelLocations.get(0).getId();
        int newOrder = 3;

        // When
        // 1. 장소 순서 변경
        travelPlanService.changeTravelLocationOrder(locationIdToMove, newOrder);

        // Then
        // 1. 이동한 장소가 원하는 위치에 있는지 확인
        TravelLocation movedLocation = travelLocationRepository.findById(locationIdToMove).orElseThrow();
        assertEquals(newOrder, movedLocation.getLocationOrder(), "이동한 장소가 지정한 순서에 있어야 함");

        // 2. 변경된 장소 목록 가져오기 (b, c, a, d, e)
        List<TravelLocation> locationsAfterMove = travelPlanRepository.findById(travelPlan.getId())
                .orElseThrow()
                .getTravelLocations()
                .stream()
                .sorted(Comparator.comparing(TravelLocation::getLocationOrder))
                .collect(Collectors.toList());

        // 3. b, c, a는 고정하고 d, e만 최적화
        // 3.1 고정할 장소들 (b, c, a)
        List<TravelLocation> fixedLocations = locationsAfterMove.subList(0, 3);

        // 3.2 최적화할 장소들 (d, e)
        List<TravelLocation> locationsToOptimize = locationsAfterMove.subList(3, locationsAfterMove.size());

        // 3.3 마지막 고정 장소 (a)의 좌표
        Point lastFixedPoint = fixedLocations.get(2).getCoordinates();

        // 3.4 d, e 중에서 a에 가장 가까운 장소를 찾아 d 위치에 배치
        TravelLocation closestToA = null;
        double minDistance = Double.MAX_VALUE;

        for (TravelLocation location : locationsToOptimize) {
            double distance = routeOptimizationService.calculateDistance(lastFixedPoint,
                    location.getCoordinates());
            if (distance < minDistance) {
                minDistance = distance;
                closestToA = location;
            }
        }

        // 3.5 최적화 결과 적용
        // d 위치에 a에 가장 가까운 장소 배치
        closestToA.setLocationOrder(4); // d 위치
        travelLocationRepository.save(closestToA);

        // e 위치에 나머지 장소 배치
        for (TravelLocation location : locationsToOptimize) {
            if (!location.getId().equals(closestToA.getId())) {
                location.setLocationOrder(5); // e 위치
                travelLocationRepository.save(location);
            }
        }

        // 4. 최종 결과 검증
        List<TravelLocation> finalLocations = travelPlanRepository.findById(travelPlan.getId())
                .orElseThrow()
                .getTravelLocations()
                .stream()
                .sorted(Comparator.comparing(TravelLocation::getLocationOrder))
                .collect(Collectors.toList());

        // 4.1 b, c, a 순서가 유지되는지 확인
        assertEquals(originalLocations.get(1).getId(), finalLocations.get(0).getId(), "첫 번째 위치는 b여야 함");
        assertEquals(originalLocations.get(2).getId(), finalLocations.get(1).getId(), "두 번째 위치는 c여야 함");
        assertEquals(originalLocations.get(0).getId(), finalLocations.get(2).getId(), "세 번째 위치는 a여야 함");

        // 4.2 d 위치에 a에 가장 가까운 장소가 배치되었는지 확인
        assertEquals(closestToA.getId(), finalLocations.get(3).getId(), "네 번째 위치는 a에 가장 가까운 장소여야 함");

        // 4.3 모든 장소가 포함되어 있는지 확인
        Set<Long> originalIds = originalLocations.stream()
                .map(TravelLocation::getId)
                .collect(Collectors.toSet());
        Set<Long> finalIds = finalLocations.stream()
                .map(TravelLocation::getId)
                .collect(Collectors.toSet());
        assertEquals(originalIds, finalIds, "최적화 후에도 모든 장소가 포함되어야 함");

        // 4.4 순서가 연속적인지 확인
        for (int i = 0; i < finalLocations.size(); i++) {
            assertEquals(i + 1, finalLocations.get(i).getLocationOrder(),
                    "순서는 1부터 연속적이어야 함");
        }
    }

    /**
     * 다른 사람의 공유된 여행 계획 조회 테스트
     *
     * 이 테스트는 다른 사람의 공유된 여행 계획을 조회할 때 예외가 발생하지 않는지 검증합니다.
     */
    @Test
    @DisplayName("다른 사람의 공유된 여행 계획 조회 테스트")
    void testAccessOtherUserSharedTravelPlan() {
        // Given
        // 다른 사용자 생성
        User otherUser = User.builder()
                .nickname("otherUser")
                .password("password")
                .email("other@example.com")
                .build();
        userRepository.save(otherUser);

        // 다른 소유자로 인증 설정
        setAuthenticationUser(otherUser);

        // When & Then
        // 1. 공유된 여행 계획에 다른 사용자가 접근하면 성공
        TravelPlanDTO retrievedSharedPlan = travelPlanService.getTravelPlanDetail(travelPlan.getId(), user);
        assertNotNull(retrievedSharedPlan);
        assertEquals(travelPlan.getId(), retrievedSharedPlan.getId());
        assertEquals(travelPlan.getTitle(), retrievedSharedPlan.getTitle());
    }

    /**
     * 다른 유저의 공유되지 않은 여행 계획 조회 실패 테스트
     *
     * 이 테스트는 다른 유저의 공유되지 않은 여행 계획에 접근할 때 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("다른 유저의 공유되지 않은 여행 계획 조회 실패 테스트")
    void testAccessOtherUserUnsharedTravelPlan() {
        // 소유자로 인증 설정
        setAuthenticationUser(user);

        // Given
        // 다른 사용자 생성
        User otherUser = User.builder()
                .nickname("otherUser")
                .password("password")
                .email("other@example.com")
                .build();
        userRepository.save(otherUser);

        // 다른 사용자의 여행 계획 생성
        TravelPlan otherUserPlan = TravelPlan.builder()
                .country("Japan")
                .city("Tokyo")
                .user(otherUser)
                .title("Other User's Travel Plan")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(5))
                .isShared(false)
                .build();
        TravelPlan savedOtherUserPlan = travelPlanRepository.save(otherUserPlan);

        // When & Then
        // 1. 공유되지 않은 여행 계획에 다른 사용자가 접근하면 예외 발생
        assertThrows(ForbiddenResourceAccessException.class, () -> {
            travelPlanService.getTravelPlanDetail(savedOtherUserPlan.getId(), user);
        });
    }

    /**
     * 다른 유저의 여행 계획 수정 실패 테스트
     *
     * 이 테스트는 다른 유저의 여행 계획을 수정할 때 예외가 발생하는지 검증합니다.
     *
     * 테스트 과정: 1. 다른 유저의 여행 계획(공유 여부 상관 없음) 접근 시 예외 발생 확인 2. 다른 유저의 여행 계획 수정 시
     * 예외 발생 확인
     */
    @Test
    @DisplayName("다른 유저의 여행 계획 수정 실패 테스트")
    void testAccessOtherUserTravelPlan() {
        // 소유자로 인증 설정
        setAuthenticationUser(user);

        // Given
        // 다른 사용자 생성
        User otherUser = User.builder()
                .nickname("otherUser")
                .password("password")
                .email("other@example.com")
                .build();
        userRepository.save(otherUser);

        // 다른 사용자의 여행 계획 생성
        TravelPlan otherUserPlan = TravelPlan.builder()
                .country("Japan")
                .city("Tokyo")
                .user(otherUser)
                .title("Other User's Travel Plan")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(5))
                .isShared(false)
                .build();
        TravelPlan savedOtherUserPlan = travelPlanRepository.save(otherUserPlan);

        // When & Then
        // 1-1. 다른 사용자의 공유된 여행 계획 수정 시도
        TravelPlanUpdateRequest updateRequest = new TravelPlanUpdateRequest();
        updateRequest.setTitle("Trying to update other's shared plan");

        assertThrows(ForbiddenResourceAccessException.class, () -> {
            travelPlanService.updateTravelPlan(savedOtherUserPlan.getId(), updateRequest, user);
        });

        // 1-2. 다른 사용자의 공유되지 않은 여행 계획 수정 시도
        updateRequest.setTitle("Trying to update other's unshared plan");
        assertThrows(ForbiddenResourceAccessException.class, () -> {
            travelPlanService.updateTravelPlan(savedOtherUserPlan.getId(), updateRequest, user);
        });
    }

    /**
     * 여행 계획 업데이트 테스트
     *
     * 이 테스트는 TravelPlanService가 여행 계획의 제목을 올바르게 업데이트하는지 검증합니다.
     *
     * 테스트 과정: 1. 업데이트할 여행 계획 DTO 생성 (새 제목 포함) 2. TravelPlanService를 사용하여 여행 계획
     * 업데이트 3. 결과 검증: 변경된 제목이 데이터베이스에 올바르게 반영되었는지 확인
     */
    @Test
    @DisplayName("여행 계획 업데이트 테스트")
    void testUpdateTravelPlan() {
        // 소유자로 인증 설정
        setAuthenticationUser(user);

        // Given
        String newTitle = "Updated Travel Plan";
        TravelPlanUpdateRequest updateRequest = new TravelPlanUpdateRequest();
        updateRequest.setTitle(newTitle);
        updateRequest.setStartDate(LocalDate.now().plusDays(2));
        updateRequest.setEndDate(LocalDate.now().plusDays(6));

        // When
        TravelPlanDTO updatedPlan = travelPlanService.updateTravelPlan(travelPlan.getId(), updateRequest, user);

        // Then
        assertEquals(newTitle, updatedPlan.getTitle());
        TravelPlan savedPlan = travelPlanRepository.findById(travelPlan.getId()).orElseThrow();
        assertEquals(newTitle, savedPlan.getTitle());
    }

    /**
     * 여행 계획 삭제 테스트
     *
     * 이 테스트는 TravelPlanService가 여행 계획을 올바르게 삭제하는지 검증합니다.
     *
     * 테스트 과정: 1. 여행 계획 삭제 시도 2. 결과 검증: 여행 계획이 삭제되었는지 확인
     */
    @Test
    @DisplayName("여행 계획 삭제 테스트")
    void testDeleteTravelPlan() {
        // 소유자로 인증 설정
        setAuthenticationUser(user);

        // When
        travelPlanService.deleteTravelPlan(travelPlan.getId(), user);
        // 삭제 후 엔티티가 존재하지 않는지 확인
        Optional<TravelPlan> deletedPlan = travelPlanRepository.findById(travelPlan.getId());
        assertTrue(deletedPlan.isEmpty(), "삭제된 여행 계획은 조회되지 않아야 합니다");
    }

    /**
     * 다른 사용자의 여행 계획 삭제 실패 테스트
     *
     * 이 테스트는 다른 사용자의 여행 계획을 삭제할 때 예외가 발생하는지 검증합니다.
     *
     * 테스트 과정: 1. 다른 사용자의 여행 계획 삭제 시도
     */
    @Test
    @DisplayName("다른 사용자의 여행 계획 삭제 실패 테스트")
    void testDeleteOtherUserTravelPlan() {
        // 소유자로 인증 설정
        setAuthenticationUser(user);

        // Given
        // 다른 사용자 생성
        User otherUser = User.builder()
                .nickname("otherUser")
                .password("password")
                .email("other@example.com")
                .build();
        userRepository.save(otherUser);

        // 다른 사용자의 여행 계획 생성
        TravelPlan otherUserPlan = TravelPlan.builder()
                .country("Japan")
                .city("Tokyo")
                .user(otherUser)
                .title("Other User's Travel Plan")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(5))
                .isShared(false)
                .build();
        TravelPlan savedOtherUserPlan = travelPlanRepository.save(otherUserPlan);

        // When
        assertThrows(ForbiddenResourceAccessException.class, () -> {
            travelPlanService.deleteTravelPlan(savedOtherUserPlan.getId(), user);
        });
    }

    private void setAuthenticationUser(User user) {
        // 인증 객체 생성
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities());

        // SecurityContext에 인증 객체 설정
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 모의 GPT 응답 JSON 생성 헬퍼 메소드
     *
     * 이 메소드는 테스트에 사용할 모의 GPT 응답 JSON을 생성합니다. 서울의 주요 관광지 정보와 방문 날짜를 포함한 JSON 배열을
     * 생성합니다.
     *
     * @param startDate 여행 시작 날짜
     * @return GPT 응답 형식의 JSON 문자열
     */
    private String createMockGptResponse(LocalDate startDate) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

        // 첫째 날 장소들
        jsonBuilder.append(
                "{\"name\":\"Gyeongbokgung Palace\",\"latitude\":37.5796,\"longitude\":126.9770,\"availableDate\":\"")
                .append(startDate.format(formatter)).append("\"},");
        jsonBuilder.append(
                "{\"name\":\"Insadong\",\"latitude\":37.5746,\"longitude\":126.9850,\"availableDate\":\"")
                .append(startDate.format(formatter)).append("\"},");

        // 둘째 날 장소들
        LocalDate secondDay = startDate.plusDays(1);
        jsonBuilder
                .append("{\"name\":\"Namsan Tower\",\"latitude\":37.5512,\"longitude\":126.9882,\"availableDate\":\"")
                .append(secondDay.format(formatter)).append("\"},");
        jsonBuilder.append(
                "{\"name\":\"Myeongdong\",\"latitude\":37.5635,\"longitude\":126.9850,\"availableDate\":\"")
                .append(secondDay.format(formatter)).append("\"}");

        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }
}
