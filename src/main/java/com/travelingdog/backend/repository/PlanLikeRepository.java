package com.travelingdog.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.PlanLike;
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
}
