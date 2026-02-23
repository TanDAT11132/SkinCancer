package com.skincancer.backend.exception;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String code,
        String message,
        String path
) {
    public static ApiErrorResponse of(int status, String error, String code, String message, String path) {
        return new ApiErrorResponse(LocalDateTime.now().toString(), status, error, code, message, path);
    }
}
