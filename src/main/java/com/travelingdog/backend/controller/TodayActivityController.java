package com.travelingdog.backend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelingdog.backend.dto.todayActivity.SaveActivityRequestDTO;
import com.travelingdog.backend.dto.todayActivity.SavedActivityResponseDTO;
import com.travelingdog.backend.dto.todayActivity.TodayActivityRequestDTO;
import com.travelingdog.backend.dto.todayActivity.TodayActivityResponseDTO;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.service.TodayActivityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/today-activity")
@RequiredArgsConstructor
@Tag(name = "Today Activity", description = "당일 활동 추천 API")
public class TodayActivityController {

        private static final Logger log = LoggerFactory.getLogger(TodayActivityController.class);

        private final TodayActivityService todayActivityService;

        @PostMapping("/recommend")
        @Operation(summary = "당일 활동 추천", description = "현재 위치와 선호도를 기반으로 당일 활동을 추천합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "추천 성공"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        public ResponseEntity<TodayActivityResponseDTO> recommendTodayActivity(
                        @Parameter(description = "당일 활동 추천 요청 정보", required = true) @Valid @RequestBody TodayActivityRequestDTO request,
                        @Parameter(hidden = true) @AuthenticationPrincipal User user) {

                log.info("당일 활동 추천 요청 - 사용자: {}, 위치: {}",
                                user != null ? user.getEmail() : "anonymous", request.getLocation());

                try {
                        TodayActivityResponseDTO response = todayActivityService.generateTodayActivity(request);

                        log.info("당일 활동 추천 성공 - 위치: {}, 총 추천 개수: {}",
                                        response.getLocation(),
                                        response.getRestaurants().size() +
                                                        response.getCultureSpots().size() +
                                                        response.getShoppingSpots().size() +
                                                        response.getNatureSpots().size());

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("당일 활동 추천 실패 - 사용자: {}, 위치: {}, 오류: {}",
                                        user != null ? user.getEmail() : "anonymous",
                                        request.getLocation(),
                                        e.getMessage());
                        throw e;
                }
        }

        @PostMapping("/save")
        @Operation(summary = "활동 저장", description = "마음에 드는 활동을 저장합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "저장 성공"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 중복 저장"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        public ResponseEntity<SavedActivityResponseDTO> saveActivity(
                        @Parameter(description = "저장할 활동 정보", required = true) @Valid @RequestBody SaveActivityRequestDTO request,
                        @Parameter(hidden = true) @AuthenticationPrincipal User user) {

                log.info("활동 저장 요청 - 사용자: {}, 활동명: {}, 카테고리: {}",
                                user != null ? user.getEmail() : "anonymous",
                                request.getLocationName(),
                                request.getCategory());

                try {
                        SavedActivityResponseDTO response = todayActivityService.saveActivity(request, user);

                        log.info("활동 저장 성공 - 활동 ID: {}, 활동명: {}",
                                        response.getId(), response.getLocationName());

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("활동 저장 실패 - 사용자: {}, 활동명: {}, 오류: {}",
                                        user != null ? user.getEmail() : "anonymous",
                                        request.getLocationName(),
                                        e.getMessage());
                        throw e;
                }
        }

        @GetMapping("/saved")
        @Operation(summary = "저장된 활동 목록 조회", description = "사용자가 저장한 모든 활동 목록을 조회합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "조회 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        public ResponseEntity<List<SavedActivityResponseDTO>> getSavedActivities(
                        @Parameter(hidden = true) @AuthenticationPrincipal User user) {

                log.info("저장된 활동 목록 조회 요청 - 사용자: {}",
                                user != null ? user.getEmail() : "anonymous");

                try {
                        List<SavedActivityResponseDTO> response = todayActivityService.getSavedActivities(user);

                        log.info("저장된 활동 목록 조회 성공 - 사용자: {}, 개수: {}",
                                        user.getEmail(), response.size());

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("저장된 활동 목록 조회 실패 - 사용자: {}, 오류: {}",
                                        user != null ? user.getEmail() : "anonymous",
                                        e.getMessage());
                        throw e;
                }
        }

        @GetMapping("/saved/category/{category}")
        @Operation(summary = "카테고리별 저장된 활동 조회", description = "특정 카테고리의 저장된 활동 목록을 조회합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "조회 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        public ResponseEntity<List<SavedActivityResponseDTO>> getSavedActivitiesByCategory(
                        @Parameter(description = "카테고리명", required = true) @PathVariable String category,
                        @Parameter(hidden = true) @AuthenticationPrincipal User user) {

                log.info("카테고리별 저장된 활동 조회 요청 - 사용자: {}, 카테고리: {}",
                                user != null ? user.getEmail() : "anonymous", category);

                try {
                        List<SavedActivityResponseDTO> response = todayActivityService
                                        .getSavedActivitiesByCategory(user, category);

                        log.info("카테고리별 저장된 활동 조회 성공 - 사용자: {}, 카테고리: {}, 개수: {}",
                                        user.getEmail(), category, response.size());

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("카테고리별 저장된 활동 조회 실패 - 사용자: {}, 카테고리: {}, 오류: {}",
                                        user != null ? user.getEmail() : "anonymous",
                                        category,
                                        e.getMessage());
                        throw e;
                }
        }

        @DeleteMapping("/saved/{activityId}")
        @Operation(summary = "저장된 활동 삭제", description = "저장된 활동을 삭제합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "삭제 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
                        @ApiResponse(responseCode = "404", description = "활동을 찾을 수 없음"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        public ResponseEntity<Void> deleteSavedActivity(
                        @Parameter(description = "삭제할 활동 ID", required = true) @PathVariable Long activityId,
                        @Parameter(hidden = true) @AuthenticationPrincipal User user) {

                log.info("저장된 활동 삭제 요청 - 사용자: {}, 활동 ID: {}",
                                user != null ? user.getEmail() : "anonymous", activityId);

                try {
                        todayActivityService.deleteSavedActivity(activityId, user);

                        log.info("저장된 활동 삭제 성공 - 사용자: {}, 활동 ID: {}",
                                        user.getEmail(), activityId);

                        return ResponseEntity.noContent().build();

                } catch (Exception e) {
                        log.error("저장된 활동 삭제 실패 - 사용자: {}, 활동 ID: {}, 오류: {}",
                                        user != null ? user.getEmail() : "anonymous",
                                        activityId,
                                        e.getMessage());
                        throw e;
                }
        }

        @GetMapping("/saved/count")
        @Operation(summary = "저장된 활동 개수 조회", description = "사용자가 저장한 활동의 총 개수를 조회합니다.")
        public ResponseEntity<Long> getSavedActivityCount(
                        @Parameter(hidden = true) @AuthenticationPrincipal User user) {

                long count = todayActivityService.getSavedActivityCount(user);
                return ResponseEntity.ok(count);
        }

        @GetMapping("/health")
        @Operation(summary = "서비스 상태 확인", description = "당일 활동 추천 서비스의 상태를 확인합니다.")
        public ResponseEntity<String> healthCheck() {
                return ResponseEntity.ok("Today Activity Service is running");
        }
}