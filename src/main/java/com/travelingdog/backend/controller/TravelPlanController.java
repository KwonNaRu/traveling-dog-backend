package com.travelingdog.backend.controller;

import com.travelingdog.backend.dto.TravelLocationDTO;
import com.travelingdog.backend.dto.TravelPlanRequest;
import com.travelingdog.backend.model.TravelLocation;
import com.travelingdog.backend.service.TripPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/trip")
public class TravelPlanController {

    @Autowired
    private TripPlanService tripPlanService;

    @PostMapping("/plan")
    public ResponseEntity<List<TravelLocationDTO>> getTripPlan(@Valid @RequestBody TravelPlanRequest request) {
        List<TravelLocation> locations = tripPlanService.generateTripPlan(request);
        List<TravelLocationDTO> locationDTOs = locations.stream()
                .map(TravelLocationDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(locationDTOs);
    }
}