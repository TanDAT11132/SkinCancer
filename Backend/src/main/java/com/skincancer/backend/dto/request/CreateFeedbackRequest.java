package com.skincancer.backend.dto.request;

import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
        Boolean isCorrect,
        @Size(max = 100) String userLabel,
        @Size(max = 1000) String comment,
        Boolean allowForRetrain
) {
}




