package com.travelingdog.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.TravelLocation;

@Repository
public interface TravelLocationRepository extends JpaRepository<TravelLocation, Long> {

    @Query("SELECT tl FROM TravelLocation tl WHERE tl.travelPlan.id = :travelPlanId ORDER BY tl.locationOrder ASC")
    List<TravelLocation> findByTravelPlanId(@Param("travelPlanId") Long travelPlanId);
}
