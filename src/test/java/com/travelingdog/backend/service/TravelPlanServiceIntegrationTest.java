package com.travelingdog.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import com.travelingdog.backend.config.FirebaseConfigTest;
import com.travelingdog.backend.dto.gemini.GeminiCandidate;
import com.travelingdog.backend.dto.gemini.GeminiContent;
import com.travelingdog.backend.dto.gemini.GeminiPart;
import com.travelingdog.backend.dto.gemini.GeminiRequest;
import com.travelingdog.backend.dto.gemini.GeminiResponse;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanUpdateRequest;
import com.travelingdog.backend.exception.ForbiddenResourceAccessException;
import com.travelingdog.backend.model.Itinerary;
import com.travelingdog.backend.model.ItineraryActivity;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.TravelPlanRepository;
import com.travelingdog.backend.repository.UserRepository;
import com.travelingdog.backend.status.PlanStatus;

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
@Import(FirebaseConfigTest.class)
public class TravelPlanServiceIntegrationTest {

        @Autowired
        private TravelPlanService travelPlanService;

        @Autowired
        private TravelPlanRepository travelPlanRepository;

        @Autowired
        private UserRepository userRepository;

        @MockBean
        private RestClient restClient;

        private User user;
        private TravelPlan travelPlan;

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
                user = userRepository.save(user);

                // 현재 날짜와 미래 날짜 설정
                LocalDate today = LocalDate.now();
                LocalDate futureDate = today.plusDays(7);

                // 먼저 TravelPlan 생성 및 저장
                travelPlan = TravelPlan.builder()
                                .country("Korea")
                                .city("Seoul")
                                .user(user)
                                .title("Test Travel Plan")
                                .itineraries(new ArrayList<>())
                                .startDate(today)
                                .transportationTips("Transportation Tips")
                                .travelStyles(new ArrayList<>())
                                .interests(new ArrayList<>())
                                .accommodationTypes(new ArrayList<>())
                                .transportationTypes(new ArrayList<>())
                                .restaurantRecommendations(new ArrayList<>())
                                .endDate(futureDate)
                                .status(PlanStatus.PUBLISHED)
                                .build();
                travelPlan = travelPlanRepository.save(travelPlan);

                // Itineraries 생성
                for (int i = 0; i < 3; i++) {
                        // 먼저 Itinerary 생성
                        Itinerary itinerary = Itinerary.builder()
                                        .location("Location " + (i + 1))
                                        .activities(new ArrayList<>())
                                        .date("2024-05-10")
                                        .build();
                        // itinerary = itineraryRepository.save(itinerary);

                        ItineraryActivity activity1 = ItineraryActivity.builder()
                                        .title("Activity")
                                        .description("Activity")
                                        .locationName("Test Location Name")
                                        .build();
                        itinerary.addActivity(activity1);

                        travelPlan.addItinerary(itinerary);
                }

                // TravelPlan의 itineraries 리스트 업데이트 (양방향 관계 유지)
                travelPlanRepository.save(travelPlan);

                // RestClient 모킹 설정
                GeminiResponse mockResponse = new GeminiResponse();
                List<GeminiCandidate> candidates = new ArrayList<>();
                GeminiCandidate candidate = new GeminiCandidate();
                GeminiContent content = new GeminiContent();

                // Mock GPT 응답 생성 - 테스트에 적합한 전체 여행 계획 JSON
                String jsonResponse = "{"
                                + "\"trip_name\":\"서울 여행 계획\","
                                + "\"start_date\":\"" + today + "\","
                                + "\"end_date\":\"" + today.plusDays(7) + "\","
                                + "\"travel_style\":[\"도시\",\"문화\"],"
                                + "\"country\":\"한국\","
                                + "\"destination\":\"서울\","
                                + "\"interests\":[\"역사\",\"쇼핑\"],"
                                + "\"accommodation\":[\"호텔\"],"
                                + "\"transportation\":[\"지하철\",\"버스\"],"
                                + "\"itinerary\":["
                                + "  {\"date\":1,\"location\":\"종로구\","
                                + "   \"activities\":[{\"title\":\"경복궁\",\"location_name\":\"Test Location Name\",\"description\":\"조선 왕조의 정궁\"}]"
                                + "  },"
                                + "  {\"date\":2,\"location\":\"용산구\","
                                + "   \"activities\":[{\"title\":\"남산타워\",\"location_name\":\"Test Location Name\",\"description\":\"서울의 랜드마크\"}]"
                                + "  }"
                                + "],"
                                + "\"restaurant_recommendations\":[{\"location_name\":\"명동 음식점\",\"description\":\"인기 관광지의 맛집\"}],"
                                + "\"transportation_tips\":\"서울은 대중교통이 잘 발달되어 있습니다.\""
                                + "}";

                List<GeminiPart> parts = new ArrayList<>();
                parts.add(GeminiPart.builder()
                                .text(jsonResponse)
                                .build());
                content.setParts(parts);
                candidate.setContent(content);
                candidates.add(candidate);
                mockResponse.setCandidates(candidates);

                RequestBodySpec requestBodySpec = mock(RequestBodySpec.class);
                RequestBodyUriSpec requestBodyUriSpec = mock(RequestBodyUriSpec.class);
                ResponseSpec responseSpec = mock(ResponseSpec.class);

                when(restClient.post()).thenReturn(requestBodyUriSpec);
                when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
                when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
                when(requestBodySpec.body(any(GeminiRequest.class))).thenReturn(requestBodySpec);
                when(requestBodySpec.retrieve()).thenReturn(responseSpec);
                when(responseSpec.body(GeminiResponse.class)).thenReturn(mockResponse);

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
                // 소유자로 인증 설정
                setAuthenticationUser(user);

                // Given
                TravelPlanRequest request = new TravelPlanRequest();
                request.setCity("서울");
                request.setStartDate(LocalDate.now());
                request.setEndDate(LocalDate.now().plusDays(7));
                request.setTravelStyle("Adventure");
                request.setInterests("Interests");
                request.setAccommodation("Accommodation");
                request.setTransportation("Transportation");

                // When
                TravelPlanDTO createdPlan = travelPlanService.createTravelPlan(request, user);

                // Then
                assertNotNull(createdPlan);
                assertEquals(request.getCity(), createdPlan.getCity());
                assertEquals(request.getStartDate(), createdPlan.getStartDate());
                assertEquals(request.getEndDate(), createdPlan.getEndDate());

                // 생성된 여행 장소 확인
                assertNotNull(createdPlan.getItineraries());
                assertFalse(createdPlan.getItineraries().isEmpty());
                assertEquals(2, createdPlan.getItineraries().size());
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
                secondRequest.setCity("Tokyo");
                secondRequest.setStartDate(LocalDate.now());
                secondRequest.setEndDate(LocalDate.now().plusDays(7));
                secondRequest.setTravelStyle("Adventure");
                secondRequest.setInterests("Interests");
                secondRequest.setAccommodation("Accommodation");
                secondRequest.setTransportation("Transportation");

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
                                .status(PlanStatus.PRIVATE)
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
                                .status(PlanStatus.PRIVATE)
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

                // 삭제 후 엔티티의 상태가 DELETED 여야 함
                Optional<TravelPlan> deletedPlan = travelPlanRepository.findById(travelPlan.getId());
                assertTrue(deletedPlan.get().getStatus() == PlanStatus.DELETED,
                                "삭제된 여행 계회 상태가 DELETED여야 합니다");
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
                                .status(PlanStatus.PRIVATE)
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
                                "{\"title\":\"Gyeongbokgung Palace\", \"type\":\"LOCATION\", \"description\":\"조선 왕조의 정궁\"},");
                jsonBuilder.append(
                                "{\"title\":\"Insadong\", \"type\":\"LOCATION\", \"description\":\"인사동 거리\"},");

                // 둘째 날 장소들
                LocalDate secondDay = startDate.plusDays(1);
                jsonBuilder
                                .append("{\"title\":\"Namsan Tower\", \"type\":\"LOCATION\", \"description\":\"남산 타워\"},");
                jsonBuilder.append(
                                "{\"title\":\"Myeongdong\", \"type\":\"LOCATION\", \"description\":\"명동 거리\"}");

                jsonBuilder.append("]");
                return jsonBuilder.toString();
        }
}
