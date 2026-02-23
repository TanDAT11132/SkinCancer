package com.skincancer.backend.security;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenInfo(
        @JsonProperty("sub") String sub,
        @JsonProperty("email") String email,
        @JsonProperty("name") String name,
        @JsonProperty("aud") String aud,
        @JsonProperty("email_verified") String emailVerified
) {
    public boolean isVerified() {
        return "true".equalsIgnoreCase(emailVerified);
    }
}




