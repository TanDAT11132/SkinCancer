package com.skincancer.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skincancer.backend.config.FrontendProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FrontendConfigController {

    private final FrontendProperties frontendProperties;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/config.js", produces = "application/javascript")
    public String configJs() throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("API_BASE_URL", blankToDefault(frontendProperties.apiBaseUrl(), ""));
        payload.put("GOOGLE_CLIENT_ID", blankToDefault(frontendProperties.googleClientId(), ""));
        return "window.APP_CONFIG = " + objectMapper.writeValueAsString(payload) + ";";
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
