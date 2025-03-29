package com.travelingdog.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.ItineraryDinner;

@Repository
public interface ItineraryDinnerRepository extends JpaRepository<ItineraryDinner, Long> {

}
