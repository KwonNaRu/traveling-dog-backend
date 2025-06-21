package com.travelingdog.backend.dto.restaurant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantRecommendationRequestDTO {

    private String mealType; // 아침, 점심, 저녁, 간식 등
    private String cuisine; // 한식, 중식, 일식, 양식 등
    private String priceRange; // 저렴, 보통, 비싼 등
    private Integer numberOfPeople; // 인원 수
    private String specialRequests; // 특별 요청사항 (비건, 할랄 등)

}