package com.skincancer.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean hasAuthHeader = authHeader != null && !authHeader.isBlank();
        boolean hasBearerToken = hasAuthHeader && authHeader.startsWith("Bearer ");
        boolean jwtValidated = false;
        String principalInfo = "anonymous";

        log.info("[API][REQ] {} {} authHeaderPresent={} bearerToken={}", method, path, hasAuthHeader, hasBearerToken);

        if (hasBearerToken) {
            String token = authHeader.substring(7);
            try {
                UserPrincipal principal = jwtService.parse(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                jwtValidated = true;
                principalInfo = "%s(%s)".formatted(principal.email(), principal.userId());
                log.info("[API][JWT] {} {} tokenValid=true principal={}", method, path, principalInfo);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                log.warn("[API][JWT] {} {} tokenValid=false reason={}", method, path, ex.getClass().getSimpleName());
            }
        } else if (hasAuthHeader) {
            log.warn("[API][JWT] {} {} malformedAuthorizationHeader=true", method, path);
        }

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - start;
        boolean authenticated = SecurityContextHolder.getContext().getAuthentication() != null;
        log.info(
                "[API][RES] {} {} status={} durationMs={} jwtValidated={} authenticated={} principal={}",
                method,
                path,
                response.getStatus(),
                duration,
                jwtValidated,
                authenticated,
                principalInfo
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !request.getRequestURI().startsWith("/api");
    }
}




