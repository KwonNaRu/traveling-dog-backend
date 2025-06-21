package com.travelingdog.backend.dto.travelPlan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanSearchRequest {

    private String keyword; // 검색 키워드 (제목, 도시, 국가에서 검색)
    private String country; // 국가 필터
    private String city; // 도시 필터
    private String sortBy; // 정렬 기준 (popular, recent, oldest)
    private Integer page; // 페이지 번호 (0부터 시작, 기본값: 0)
    private Integer size; // 페이지 크기 (기본값: 10)

    // 기본값 설정
    public Integer getPage() {
        return page != null ? page : 0;
    }

    public Integer getSize() {
        return size != null && size > 0 && size <= 50 ? size : 10;
    }

    public String getSortBy() {
        return sortBy != null ? sortBy : "recent";
    }
}