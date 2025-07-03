package com.travelingdog.backend.dto.todayActivity;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveActivityRequestDTO {

    @NotBlank(message = "위치명은 필수입니다")
    private String locationName;

    @NotBlank(message = "카테고리는 필수입니다")
    private String category;

    // 저장할 때의 위치 정보 (참고용, 선택사항)
    private String savedLocation;
}