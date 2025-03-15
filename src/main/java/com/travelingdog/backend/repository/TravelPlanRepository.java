package com.travelingdog.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    /**
     * 특정 사용자의 여행 계획 목록을 조회합니다.
     * 
     * @param user 조회할 사용자
     * @return 사용자의 여행 계획 목록
     */
    List<TravelPlan> findAllByUser(User user);

    /**
     * 공유된 여행 계획 또는 나의 여행 계획을 상세 조회합니다.
     * 
     * @param id   조회할 여행 계획의 ID
     * @param user 조회할 사용자
     * @return 여행 계획 상세
     */
    @Query("SELECT p FROM TravelPlan p WHERE p.id = :id AND (p.user.id = :userId OR p.status = 'PUBLISHED')")
    Optional<TravelPlan> findByIdWithSecurity(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 특정 사용자의 여행 계획 상세를 조회합니다.
     * 
     * @param id   조회할 여행 계획의 ID
     * @param user 조회할 사용자
     * @return 여행 계획 상세
     */
    @Query("SELECT p FROM TravelPlan p WHERE p.id = :id AND p.user.id = :userId")
    Optional<TravelPlan> findByIdWithUser(@Param("id") Long id, @Param("userId") Long userId);
}
