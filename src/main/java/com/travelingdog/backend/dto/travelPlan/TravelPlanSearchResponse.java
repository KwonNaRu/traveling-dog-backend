package com.travelingdog.backend.dto.travelPlan;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanSearchResponse {

    private List<TravelPlanDTO> content; // 검색 결과 리스트
    private int page; // 현재 페이지
    private int size; // 페이지 크기
    private long totalElements; // 전체 결과 개수
    private int totalPages; // 전체 페이지 수
    private boolean first; // 첫 번째 페이지 여부
    private boolean last; // 마지막 페이지 여부
    private String sortBy; // 정렬 기준
    private String keyword; // 검색 키워드
}