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

        @Operation(summary = "인기 여행 리스트 조회", description = "인기 여행 리스트를 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "인기 여행 리스트 조회 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TravelPlanDTO.class)))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @GetMapping("/popular")
        public ResponseEntity<List<TravelPlanDTO>> getPopularTravelPlanList() {
                List<TravelPlanDTO> travelPlanDTOs = travelPlanService.getPopularTravelPlanList();
                return ResponseEntity.ok(travelPlanDTOs);
        }

        @Operation(summary = "최근 여행 계획 조회", description = "최근 여행 계획을 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "최근 여행 계획 조회 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TravelPlanDTO.class)))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @GetMapping("/recent")
        public ResponseEntity<List<TravelPlanDTO>> getRecentTravelPlanList() {
                List<TravelPlanDTO> travelPlanDTOs = travelPlanService.getRecentTravelPlanList();
                return ResponseEntity.ok(travelPlanDTOs);
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

        @Operation(summary = "여행 계획 좋아요 추가", description = "여행 계획을 좋아요 추가합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "여행 계획 좋아요 추가 성공"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "403", description = "접근 금지된 여행 계획"),
                        @ApiResponse(responseCode = "500", description = "서버 오류")
        })
        @PostMapping("/{id}/like")
        public ResponseEntity<Void> addLike(@PathVariable("id") Long id,
                        @AuthenticationPrincipal User user) {

                travelPlanService.addLike(id, user);
                return ResponseEntity.noContent().build(); // 204 No Content
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
