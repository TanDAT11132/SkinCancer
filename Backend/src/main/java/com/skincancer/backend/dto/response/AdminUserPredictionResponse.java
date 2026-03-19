package com.skincancer.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserPredictionResponse(
        UUID predictionId,
        UUID imageId,
        String imageUrl,
        String predictedClass,
        BigDecimal probability,
        String topKJson,
        String rawResponseJson,
        String modelName,
        String modelVersion,
        LocalDateTime requestedAt,
        Boolean isCorrect,
        String userLabel,
        String comment,
        boolean allowForRetrain,
        LocalDateTime feedbackCreatedAt
) {
}
