package com.skincancer.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fastapi")
public record FastApiProperties(String baseUrl, int defaultTopK) {
}




