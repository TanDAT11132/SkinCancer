package com.skincancer.backend.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends ApiException {
    public ExternalServiceException(String code, String message) {
        super(code, message, HttpStatus.BAD_GATEWAY);
    }
}
