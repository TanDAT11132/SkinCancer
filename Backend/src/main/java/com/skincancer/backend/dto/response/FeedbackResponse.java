package com.skincancer.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResponse(
        UUID feedbackId,
        UUID predictionId,
        UUID userId,
        Boolean isCorrect,
        String userLabel,
        String comment,
        boolean allowForRetrain,
        LocalDateTime createdAt
) {
}
