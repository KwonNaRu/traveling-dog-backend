package com.travelingdog.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelingdog.backend.dto.travelPlan.ItineraryDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanDTO;
import com.travelingdog.backend.dto.travelPlan.TravelPlanRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanSearchRequest;
import com.travelingdog.backend.dto.travelPlan.TravelPlanSearchResponse;
import com.travelingdog.backend.dto.travelPlan.TravelPlanUpdateRequest;
import com.travelingdog.backend.exception.UnauthorizedException;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.service.TravelPlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/travel/plan")
@Tag(name = "여행 계획", description = "여행 계획 생성 API")
public class TravelPlanController {

        @Autowired
        private TravelPlanService travelPlanService;

        @Operation(summary = "여행 계획 생성", description = "국가, 도시, 여행 날짜를 입력받아 여행 계획을 생성합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 생성 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ItineraryDTO.class)))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "503", description = "외부 API 오류")
        })
        @PostMapping
        public ResponseEntity<TravelPlanDTO> generateTripPlan(
                        @Parameter(description = "여행 계획 요청 정보", required = true) @Valid @RequestBody TravelPlanRequest request,
                        @AuthenticationPrincipal User user) {
                TravelPlanDTO travelPlanDTO = travelPlanService.createTravelPlan(request, user);
                if (travelPlanDTO == null) {
                        return ResponseEntity.internalServerError().build();
                }
                return ResponseEntity.ok(travelPlanDTO);
        }

        @Operation(summary = "여행 계획 리스트 조회", description = "여행 계획 리스트를 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 리스트 조회 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TravelPlanDTO.class)))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @GetMapping("/list")
        public ResponseEntity<List<TravelPlanDTO>> getTravelPlanList(@AuthenticationPrincipal User user) {
                List<TravelPlanDTO> travelPlanDTOs = travelPlanService.getTravelPlanList(user);
                return ResponseEntity.ok(travelPlanDTOs);
        }

        @Operation(summary = "여행 계획 검색", description = "키워드, 도시, 국가 등으로 여행 계획을 검색합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 검색 성공", content = @Content(schema = @Schema(implementation = TravelPlanSearchResponse.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @PostMapping("/search")
        public ResponseEntity<TravelPlanSearchResponse> searchTravelPlans(
                        @Parameter(description = "검색 조건", required = false) @RequestBody(required = false) TravelPlanSearchRequest searchRequest) {

                // 요청이 null인 경우 기본값으로 초기화
                if (searchRequest == null) {
                        searchRequest = new TravelPlanSearchRequest();
                }

                TravelPlanSearchResponse response = travelPlanService.searchTravelPlans(searchRequest);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "여행 계획 좋아요 조회", description = "여행 계획을 좋아요 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 좋아요 조회 성공", content = @Content(schema = @Schema(implementation = TravelPlanDTO.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "접근 금지된 여행 계획"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @GetMapping("/like")
        public ResponseEntity<List<TravelPlanDTO>> getLike(@AuthenticationPrincipal User user) {

                List<TravelPlanDTO> travelPlanDTOs = travelPlanService.getLikedTravelPlanList(user);
                return ResponseEntity.ok(travelPlanDTOs);
        }

        @Operation(summary = "여행 계획 상세 조회", description = "여행 계획 상세 정보를 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 상세 조회 성공", content = @Content(schema = @Schema(implementation = TravelPlanDTO.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "접근 금지된 여행 계획"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @GetMapping("/{id}")
        public ResponseEntity<TravelPlanDTO> getTravelPlanDetail(@PathVariable("id") Long id,
                        @AuthenticationPrincipal User user) {

                TravelPlanDTO travelPlanDTO = travelPlanService.getTravelPlanDetail(id, user);
                return ResponseEntity.ok(travelPlanDTO);

        }

        @Operation(summary = "여행 계획 수정", description = "여행 계획을 수정합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 수정 성공", content = @Content(schema = @Schema(implementation = TravelPlanDTO.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "접근 금지된 여행 계획"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @PutMapping("/{id}")
        public ResponseEntity<TravelPlanDTO> updateTravelPlan(@PathVariable("id") Long id,
                        @RequestBody TravelPlanUpdateRequest request,
                        @AuthenticationPrincipal User user) {

                TravelPlanDTO travelPlanDTO = travelPlanService.updateTravelPlan(id, request, user);
                return ResponseEntity.ok(travelPlanDTO);

        }

        @Operation(summary = "여행 계획 삭제", description = "여행 계획을 삭제합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 삭제 성공"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "접근 금지된 여행 계획"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteTravelPlan(@PathVariable("id") Long id,
                        @AuthenticationPrincipal User user) {

                travelPlanService.deleteTravelPlan(id, user);
                return ResponseEntity.noContent().build(); // 204 No Content
        }

        @Operation(summary = "여행 계획 좋아요 토글", description = "여행 계획 좋아요를 추가하거나 취소합니다. 이미 좋아요를 누른 상태면 취소하고, 아니면 추가합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "좋아요 토글 성공 (true: 좋아요 추가됨, false: 좋아요 취소됨)"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "접근 금지된 여행 계획"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @PostMapping("/{id}/like")
        public ResponseEntity<Boolean> toggleLike(@PathVariable("id") Long id,
                        @AuthenticationPrincipal User user) {

                boolean isLiked = travelPlanService.toggleLike(id, user);
                return ResponseEntity.ok(isLiked); // true: 좋아요 추가됨, false: 좋아요 취소됨
        }

        @Operation(summary = "여행 계획 좋아요 취소", description = "여행 계획을 좋아요 취소합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 좋아요 취소 성공"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "접근 금지된 여행 계획"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @DeleteMapping("/{id}/like")
        public ResponseEntity<Void> removeLike(@PathVariable("id") Long id,
                        @AuthenticationPrincipal User user) {

                travelPlanService.removeLike(id, user);
                return ResponseEntity.noContent().build(); // 204 No Content
        }

        @Operation(summary = "여행 계획 좋아요 상태 확인", description = "사용자가 특정 여행 계획에 좋아요를 눌렀는지 확인합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "좋아요 상태 확인 성공"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "404", description = "여행 계획을 찾을 수 없음"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @GetMapping("/{id}/like/status")
        public ResponseEntity<Boolean> getLikeStatus(@PathVariable("id") Long id,
                        @AuthenticationPrincipal User user) {

                boolean isLiked = travelPlanService.isLiked(id, user);
                return ResponseEntity.ok(isLiked);
        }

        @Operation(summary = "여행 계획 공개", description = "여행 계획을 공개합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 공개 성공"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "접근 금지된 여행 계획"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @PutMapping("/{id}/publish")
        public ResponseEntity<TravelPlanDTO> publishTravelPlan(@PathVariable("id") Long id,
                        @AuthenticationPrincipal User user) {

                TravelPlanDTO travelPlanDTO = travelPlanService.publishTravelPlan(id, user);
                return ResponseEntity.ok(travelPlanDTO);
        }

        @Operation(summary = "여행 계획 비공개", description = "여행 계획을 비공개합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 비공개 성공"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "접근 금지된 여행 계획"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @PutMapping("/{id}/unpublish")
        public ResponseEntity<TravelPlanDTO> unpublishTravelPlan(@PathVariable("id") Long id,
                        @AuthenticationPrincipal User user) {

                TravelPlanDTO travelPlanDTO = travelPlanService.unpublishTravelPlan(id, user);
                return ResponseEntity.ok(travelPlanDTO);
        }
}
