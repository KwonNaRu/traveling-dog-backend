package com.travelingdog.backend.dto.todayActivity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodayActivityRequestDTO {

    @NotBlank(message = "위치 정보는 필수입니다")
    private String location;

    @NotNull(message = "맛집 개수는 필수입니다")
    @Min(value = 0, message = "맛집 개수는 0개 이상이어야 합니다")
    @Max(value = 10, message = "맛집 개수는 5개 이하여야 합니다")
    private Integer restaurantCount;

    @NotNull(message = "관광/문화 개수는 필수입니다")
    @Min(value = 0, message = "관광/문화 개수는 0개 이상이어야 합니다")
    @Max(value = 10, message = "관광/문화 개수는 5개 이하여야 합니다")
    private Integer cultureCount;

    @NotNull(message = "쇼핑/엔터테인먼트 개수는 필수입니다")
    @Min(value = 0, message = "쇼핑/엔터테인먼트 개수는 0개 이상이어야 합니다")
    @Max(value = 10, message = "쇼핑/엔터테인먼트 개수는 5개 이하여야 합니다")
    private Integer shoppingCount;

    @NotNull(message = "자연/휴식 개수는 필수입니다")
    @Min(value = 0, message = "자연/휴식 개수는 0개 이상이어야 합니다")
    @Max(value = 10, message = "자연/휴식 개수는 5개 이하여야 합니다")
    private Integer natureCount;
}