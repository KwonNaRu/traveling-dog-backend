package com.travelingdog.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.FailedGptResponse;

@Repository
public interface FailedGptResponseRepository extends JpaRepository<FailedGptResponse, Long> {
    // 기본 CRUD 메서드 외에 필요한 메서드가 있다면 여기에 추가
}