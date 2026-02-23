package com.skincancer.backend.security;

import com.skincancer.backend.entity.Role;

import java.util.UUID;

public record UserPrincipal(UUID userId, String email, Role role) {
}




