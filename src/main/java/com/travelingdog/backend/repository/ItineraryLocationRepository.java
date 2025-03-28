package com.travelingdog.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.ItineraryLocation;

@Repository
public interface ItineraryLocationRepository extends JpaRepository<ItineraryLocation, Long> {

}
