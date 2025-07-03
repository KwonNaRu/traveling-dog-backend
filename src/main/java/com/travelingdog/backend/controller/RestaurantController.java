package com.travelingdog.backend.controller;

import com.travelingdog.backend.dto.restaurant.RestaurantRecommendationRequestDTO;
import com.travelingdog.backend.dto.restaurant.RestaurantRecommendationResponseDTO;
import com.travelingdog.backend.service.RestaurantRecommendationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/travel/plan/{planId}/restaurants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Restaurant Recommendation", description = "맛집 추천 API")
public class RestaurantController {

    private final RestaurantRecommendationService restaurantRecommendationService;

    @PostMapping
    @Operation(summary = "맛집 추천 생성", description = "여행 계획을 기반으로 맛집을 추천받습니다.")
    public ResponseEntity<RestaurantRecommendationResponseDTO> generateRestaurantRecommendations(
            @Parameter(description = "여행 계획 ID") @PathVariable("planId") Long planId,
            @RequestBody(required = false) RestaurantRecommendationRequestDTO requestDTO) {

        log.info("맛집 추천 요청 - 여행 계획 ID: {}", planId);

        // 요청 DTO가 null인 경우 빈 객체 생성
        if (requestDTO == null) {
            requestDTO = new RestaurantRecommendationRequestDTO();
        }

        RestaurantRecommendationResponseDTO response = restaurantRecommendationService
                .generateRestaurantRecommendations(planId, requestDTO);

        log.info("맛집 추천 완료 - 여행 계획 ID: {}, 추천 개수: {}",
                planId, response.getRestaurants().size());

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "맛집 추천 조회", description = "여행 계획을 기반으로 기본 맛집을 추천받습니다.")
    public ResponseEntity<RestaurantRecommendationResponseDTO> getRestaurantRecommendations(
            @Parameter(description = "여행 계획 ID") @PathVariable("planId") Long planId) {

        log.info("맛집 추천 조회 - 여행 계획 ID: {}", planId);

        // 기본 요청 DTO 생성
        RestaurantRecommendationRequestDTO requestDTO = new RestaurantRecommendationRequestDTO();

        RestaurantRecommendationResponseDTO response = restaurantRecommendationService
                .generateRestaurantRecommendations(planId, requestDTO);

        log.info("맛집 추천 조회 완료 - 여행 계획 ID: {}, 추천 개수: {}",
                planId, response.getRestaurants().size());

        return ResponseEntity.ok(response);
    }
}