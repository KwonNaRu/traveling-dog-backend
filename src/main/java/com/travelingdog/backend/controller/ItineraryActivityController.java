package com.travelingdog.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import com.travelingdog.backend.dto.itinerary.ItineraryActivityCreateRequest;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityResponseDTO;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityUpdateRequest;
import com.travelingdog.backend.exception.UnauthorizedException;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.service.ItineraryActivityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/itinerary/activities")
@RequiredArgsConstructor
@Tag(name = "일정 활동", description = "여행 일정 내 활동 관리 API")
public class ItineraryActivityController {

    private final ItineraryActivityService activityService;

    @Operation(summary = "활동 상세 조회", description = "특정 활동의 상세 정보를 조회합니다.", security = {
            @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 조회 성공", content = @Content(schema = @Schema(implementation = ItineraryActivityResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "활동을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ItineraryActivityResponseDTO> getActivity(
            @Parameter(description = "활동 ID", required = true) @PathVariable("id") Long id,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new UnauthorizedException("인증이 필요한 요청입니다.");
        }

        ItineraryActivityResponseDTO activity = activityService.getActivity(id, user);
        return ResponseEntity.ok(activity);
    }

    @Operation(summary = "일정별 활동 목록 조회", description = "특정 일정에 속한 모든 활동을 조회합니다.", security = {
            @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음")
    })
    @GetMapping("/itinerary/{id}")
    public ResponseEntity<List<ItineraryActivityResponseDTO>> getActivitiesByItinerary(
            @Parameter(description = "일정 ID", required = true) @PathVariable("id") Long id,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new UnauthorizedException("인증이 필요한 요청입니다.");
        }

        List<ItineraryActivityResponseDTO> activities = activityService.getActivitiesByItineraryId(id, user);
        return ResponseEntity.ok(activities);
    }

    @Operation(summary = "활동 생성", description = "새로운 활동을 생성합니다.", security = {
            @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "활동 생성 성공", content = @Content(schema = @Schema(implementation = ItineraryActivityResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<ItineraryActivityResponseDTO> createActivity(
            @Parameter(description = "활동 생성 정보", required = true) @Valid @RequestBody ItineraryActivityCreateRequest request,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new UnauthorizedException("인증이 필요한 요청입니다.");
        }

        ItineraryActivityResponseDTO createdActivity = activityService.createActivity(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdActivity);
    }

    @Operation(summary = "활동 수정", description = "기존 활동을 수정합니다.", security = {
            @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 수정 성공", content = @Content(schema = @Schema(implementation = ItineraryActivityResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "활동을 찾을 수 없음")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ItineraryActivityResponseDTO> updateActivity(
            @Parameter(description = "활동 ID", required = true) @PathVariable("id") Long id,
            @Parameter(description = "활동 수정 정보", required = true) @Valid @RequestBody ItineraryActivityUpdateRequest request,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new UnauthorizedException("인증이 필요한 요청입니다.");
        }

        ItineraryActivityResponseDTO updatedActivity = activityService.updateActivity(id, request, user);
        return ResponseEntity.ok(updatedActivity);
    }

    @Operation(summary = "활동 삭제", description = "활동을 삭제합니다.", security = {
            @SecurityRequirement(name = "bearerAuth") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "활동 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "활동을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(
            @Parameter(description = "활동 ID", required = true) @PathVariable("id") Long id,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new UnauthorizedException("인증이 필요한 요청입니다.");
        }

        activityService.deleteActivity(id, user);
        return ResponseEntity.noContent().build();
    }
}