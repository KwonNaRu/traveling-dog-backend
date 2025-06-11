package com.travelingdog.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travelingdog.backend.dto.itinerary.ItineraryActivityCreateRequest;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityResponseDTO;
import com.travelingdog.backend.dto.itinerary.ItineraryActivityUpdateRequest;
import com.travelingdog.backend.exception.ForbiddenResourceAccessException;
import com.travelingdog.backend.exception.ResourceNotFoundException;
import com.travelingdog.backend.model.Itinerary;
import com.travelingdog.backend.model.ItineraryActivity;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.repository.ItineraryActivityRepository;
import com.travelingdog.backend.repository.ItineraryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItineraryActivityService {

    private final ItineraryActivityRepository activityRepository;
    private final ItineraryRepository itineraryRepository;

    /**
     * 특정 활동 조회
     */
    @Transactional(readOnly = true)
    public ItineraryActivityResponseDTO getActivity(Long activityId, User user) {
        ItineraryActivity activity = findActivityAndValidateAccess(activityId, user);
        return ItineraryActivityResponseDTO.fromEntity(activity);
    }

    /**
     * 여행 일정에 속한 모든 활동 조회
     */
    @Transactional(readOnly = true)
    public List<ItineraryActivityResponseDTO> getActivitiesByItineraryId(Long id, User user) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("일정을 찾을 수 없습니다. ID: " + id));

        validateAccess(itinerary, user);

        return itinerary.getActivities().stream()
                .map(ItineraryActivityResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 새로운 활동 생성
     */
    @Transactional
    public ItineraryActivityResponseDTO createActivity(ItineraryActivityCreateRequest request, User user) {
        Itinerary itinerary = itineraryRepository.findById(request.getItineraryId())
                .orElseThrow(() -> new ResourceNotFoundException("일정을 찾을 수 없습니다. ID: " + request.getItineraryId()));

        validateAccess(itinerary, user);

        ItineraryActivity activity = ItineraryActivity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .locationName(request.getLocationName())
                .itinerary(itinerary)
                .build();

        return ItineraryActivityResponseDTO.fromEntity(activityRepository.save(activity));
    }

    /**
     * 활동 수정
     */
    @Transactional
    public ItineraryActivityResponseDTO updateActivity(Long activityId, ItineraryActivityUpdateRequest request,
            User user) {
        ItineraryActivity activity = findActivityAndValidateAccess(activityId, user);

        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setLocationName(request.getLocationName());

        return ItineraryActivityResponseDTO.fromEntity(activityRepository.save(activity));
    }

    /**
     * 활동 삭제
     */
    @Transactional
    public void deleteActivity(Long activityId, User user) {
        ItineraryActivity activity = findActivityAndValidateAccess(activityId, user);
        activityRepository.delete(activity);
    }

    /**
     * 활동을 찾고 접근 권한을 검증
     */
    private ItineraryActivity findActivityAndValidateAccess(Long activityId, User user) {
        ItineraryActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("활동을 찾을 수 없습니다. ID: " + activityId));

        validateAccess(activity.getItinerary(), user);

        return activity;
    }

    /**
     * 사용자가 일정에 대한 접근 권한이 있는지 검증
     */
    private void validateAccess(Itinerary itinerary, User user) {
        TravelPlan travelPlan = itinerary.getTravelPlan();

        if (!travelPlan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenResourceAccessException("해당 일정에 대한 접근 권한이 없습니다.");
        }
    }
}