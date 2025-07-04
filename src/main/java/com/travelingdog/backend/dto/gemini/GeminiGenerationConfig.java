package com.travelingdog.backend.dto.gemini;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiGenerationConfig {
    private float temperature;
    private int topK;
    private int topP;
    private int maxOutputTokens;
}