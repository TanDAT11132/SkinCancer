package com.skincancer.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record RetrainSampleResponse(
        UUID feedbackId,
        UUID predictionId,
        UUID imageId,
        String fileUri,
        String predictedClass,
        String userLabel,
        Boolean isCorrect,
        LocalDateTime createdAt
) {
}




