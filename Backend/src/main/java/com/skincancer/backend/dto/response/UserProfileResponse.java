package com.skincancer.backend.dto.response;

import com.skincancer.backend.entity.Role;

import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String email,
        String fullName,
        String gender,
        Integer age,
        Role role,
        boolean profileCompleted
) {
}
