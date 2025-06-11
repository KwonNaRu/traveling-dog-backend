package com.travelingdog.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.ItineraryActivity;

@Repository
public interface ItineraryActivityRepository extends JpaRepository<ItineraryActivity, Long> {

}
