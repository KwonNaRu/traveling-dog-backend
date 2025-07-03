package com.travelingdog.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.PlanLike;
import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;

@Repository
public interface PlanLikeRepository extends JpaRepository<PlanLike, Long> {

    /**
     * 특정 사용자가 좋아요한 여행 계획 목록을 조회합니다.
     * 
     * @param user 조회할 사용자
     * @return 사용자가 좋아요한 여행 계획 목록
     */
    @EntityGraph(attributePaths = { "travelPlan" })
    List<PlanLike> findByUser(User user);

    /**
     * 특정 사용자와 여행 계획의 좋아요를 조회합니다.
     * 
     * @param user       사용자
     * @param travelPlan 여행 계획
     * @return 좋아요 엔티티 (존재하지 않으면 Optional.empty())
     */
    Optional<PlanLike> findByUserAndTravelPlan(User user, TravelPlan travelPlan);

    /**
     * 특정 사용자가 특정 여행 계획에 좋아요를 눌렀는지 확인합니다.
     * 
     * @param user       사용자
     * @param travelPlan 여행 계획
     * @return 좋아요 존재 여부
     */
    boolean existsByUserAndTravelPlan(User user, TravelPlan travelPlan);
}
