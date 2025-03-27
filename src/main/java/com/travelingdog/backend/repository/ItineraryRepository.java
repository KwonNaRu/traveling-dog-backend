package com.travelingdog.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.Itinerary;
import com.travelingdog.backend.model.TravelPlan;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    List<Itinerary> findAllByTravelPlanIdOrderByDayAsc(Long travelPlanId);
}
