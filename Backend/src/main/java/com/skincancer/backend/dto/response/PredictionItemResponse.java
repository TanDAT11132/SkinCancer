package com.skincancer.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PredictionItemResponse(
        UUID predictionId,
        UUID imageId,
        String fileUri,
        String predictedClass,
        BigDecimal probability,
        String topKJson,
        String rawResponseJson,
        String modelName,
        String modelVersion,
        LocalDateTime requestedAt
) {
}




