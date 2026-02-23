package com.skincancer.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skincancer.backend.exception.ApiErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse payload = ApiErrorResponse.of(
                401,
                "Unauthorized",
                "UNAUTHORIZED",
                "Authentication is required or token is invalid",
                request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }
}
