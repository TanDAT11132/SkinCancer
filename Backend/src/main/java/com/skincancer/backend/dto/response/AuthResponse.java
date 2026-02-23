package com.skincancer.backend.dto.response;

import com.skincancer.backend.entity.Role;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        UUID userId,
        String email,
        Role role,
        boolean profileCompleted
) {
}




