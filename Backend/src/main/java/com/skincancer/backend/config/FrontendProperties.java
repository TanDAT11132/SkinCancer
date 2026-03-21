package com.skincancer.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(String apiBaseUrl, String googleClientId) {
}
