package com.skincancer.backend.dto.response;

import com.skincancer.backend.entity.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID userId,
        String email,
        String fullName,
        String gender,
        Integer age,
        Role role,
        LocalDateTime createdAt,
        long predictionCount,
        long feedbackCount
) {
}
