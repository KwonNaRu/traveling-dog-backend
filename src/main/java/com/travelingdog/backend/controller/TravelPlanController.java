package com.travelingdog.backend.controller;

import com.travelingdog.backend.dto.TravelLocationDTO;
import com.travelingdog.backend.dto.TravelPlanRequest;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.service.TripPlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/trip")
@Tag(name = "여행 계획", description = "여행 계획 생성 API")
public class TravelPlanController {

    @Autowired
    private TripPlanService tripPlanService;

    @Operation(summary = "여행 계획 생성", description = "국가, 도시, 여행 날짜를 입력받아 여행 계획을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "여행 계획 생성 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TravelLocationDTO.class)))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "503", description = "외부 API 오류")
    })
    @PostMapping("/plan")
    public ResponseEntity<List<TravelLocationDTO>> getTripPlan(
            @Parameter(description = "여행 계획 요청 정보", required = true) @Valid @RequestBody TravelPlanRequest request) {
        List<TravelLocation> locations = tripPlanService.generateTripPlan(request);
        List<TravelLocationDTO> locationDTOs = locations.stream()
                .map(TravelLocationDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(locationDTOs);
    }
}