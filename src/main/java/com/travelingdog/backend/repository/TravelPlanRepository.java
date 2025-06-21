package com.travelingdog.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travelingdog.backend.model.TravelPlan;
import com.travelingdog.backend.model.User;
import com.travelingdog.backend.status.PlanStatus;

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

    // 기존의 findByStatusOrderByLikeCountDesc()와 findByStatusOrderByCreatedAtDesc()
    // 메서드는
    // 이제 아래의 통합 검색 메서드들로 대체되었습니다.

    /**
     * 키워드로 여행 계획을 검색하고 좋아요 수 기준으로 정렬합니다.
     * 
     * @param keyword  검색 키워드 (제목, 도시, 국가에서 검색)
     * @param status   계획 상태
     * @param pageable 페이징 정보
     * @return 검색된 여행 계획 페이지
     */
    @Query("SELECT p FROM TravelPlan p WHERE p.status = :status AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.country) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<TravelPlan> searchByKeywordOrderByPopular(@Param("keyword") String keyword,
            @Param("status") PlanStatus status,
            Pageable pageable);

    /**
     * 키워드로 여행 계획을 검색하고 생성일 기준으로 내림차순 정렬합니다.
     * 
     * @param keyword  검색 키워드
     * @param status   계획 상태
     * @param pageable 페이징 정보
     * @return 검색된 여행 계획 페이지
     */
    @Query("SELECT p FROM TravelPlan p WHERE p.status = :status AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.country) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY p.createdAt DESC")
    Page<TravelPlan> searchByKeywordOrderByRecent(@Param("keyword") String keyword,
            @Param("status") PlanStatus status,
            Pageable pageable);

    /**
     * 키워드로 여행 계획을 검색하고 생성일 기준으로 오름차순 정렬합니다.
     * 
     * @param keyword  검색 키워드
     * @param status   계획 상태
     * @param pageable 페이징 정보
     * @return 검색된 여행 계획 페이지
     */
    @Query("SELECT p FROM TravelPlan p WHERE p.status = :status AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.country) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY p.createdAt ASC")
    Page<TravelPlan> searchByKeywordOrderByOldest(@Param("keyword") String keyword,
            @Param("status") PlanStatus status,
            Pageable pageable);

    /**
     * 도시와 국가로 여행 계획을 필터링하고 좋아요 수 기준으로 정렬합니다.
     * 
     * @param city     도시 (null 가능)
     * @param country  국가 (null 가능)
     * @param status   계획 상태
     * @param pageable 페이징 정보
     * @return 필터링된 여행 계획 페이지
     */
    @Query("SELECT p FROM TravelPlan p WHERE p.status = :status AND " +
            "(:city IS NULL OR LOWER(p.city) = LOWER(:city)) AND " +
            "(:country IS NULL OR LOWER(p.country) = LOWER(:country)) " +
            "ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<TravelPlan> findByLocationOrderByPopular(@Param("city") String city,
            @Param("country") String country,
            @Param("status") PlanStatus status,
            Pageable pageable);

    /**
     * 도시와 국가로 여행 계획을 필터링하고 생성일 기준으로 내림차순 정렬합니다.
     * 
     * @param city     도시 (null 가능)
     * @param country  국가 (null 가능)
     * @param status   계획 상태
     * @param pageable 페이징 정보
     * @return 필터링된 여행 계획 페이지
     */
    @Query("SELECT p FROM TravelPlan p WHERE p.status = :status AND " +
            "(:city IS NULL OR LOWER(p.city) = LOWER(:city)) AND " +
            "(:country IS NULL OR LOWER(p.country) = LOWER(:country)) " +
            "ORDER BY p.createdAt DESC")
    Page<TravelPlan> findByLocationOrderByRecent(@Param("city") String city,
            @Param("country") String country,
            @Param("status") PlanStatus status,
            Pageable pageable);

    /**
     * 도시와 국가로 여행 계획을 필터링하고 생성일 기준으로 오름차순 정렬합니다.
     * 
     * @param city     도시 (null 가능)
     * @param country  국가 (null 가능)
     * @param status   계획 상태
     * @param pageable 페이징 정보
     * @return 필터링된 여행 계획 페이지
     */
    @Query("SELECT p FROM TravelPlan p WHERE p.status = :status AND " +
            "(:city IS NULL OR LOWER(p.city) = LOWER(:city)) AND " +
            "(:country IS NULL OR LOWER(p.country) = LOWER(:country)) " +
            "ORDER BY p.createdAt ASC")
    Page<TravelPlan> findByLocationOrderByOldest(@Param("city") String city,
            @Param("country") String country,
            @Param("status") PlanStatus status,
            Pageable pageable);
}
