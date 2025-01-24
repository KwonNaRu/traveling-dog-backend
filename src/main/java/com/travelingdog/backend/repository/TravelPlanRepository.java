package com.travelingdog.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.TravelPlan;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

}
