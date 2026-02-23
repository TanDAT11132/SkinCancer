package com.skincancer.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PredictionQuery(
        @Min(1) @Max(10) Integer topK,
        String clientApp
) {
}




