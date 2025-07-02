package com.travelingdog.backend.repository;

import com.travelingdog.backend.model.SavedActivity;
import com.travelingdog.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedActivityRepository extends JpaRepository<SavedActivity, Long> {

    /**
     * 사용자별 저장된 활동 목록 조회 (최신순)
     */
    List<SavedActivity> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 사용자별 카테고리별 저장된 활동 목록 조회
     */
    List<SavedActivity> findByUserAndCategoryOrderByCreatedAtDesc(User user, String category);

    /**
     * 사용자가 특정 활동을 이미 저장했는지 확인
     */
    @Query("SELECT sa FROM SavedActivity sa WHERE sa.user = :user AND sa.locationName = :locationName AND sa.category = :category")
    Optional<SavedActivity> findByUserAndLocationNameAndCategory(@Param("user") User user,
            @Param("locationName") String locationName,
            @Param("category") String category);

    /**
     * 사용자별 저장된 활동 개수
     */
    long countByUser(User user);
}