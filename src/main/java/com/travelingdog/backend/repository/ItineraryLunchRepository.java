package com.travelingdog.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.ItineraryLunch;

@Repository
public interface ItineraryLunchRepository extends JpaRepository<ItineraryLunch, Long> {

}
