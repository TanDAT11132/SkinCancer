package com.skincancer.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(List<String> emails) {
    public boolean isAdminEmail(String email) {
        if (email == null || emails == null) {
            return false;
        }
        return emails.stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(value -> value.equalsIgnoreCase(email));
    }
}
