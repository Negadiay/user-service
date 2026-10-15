package com.innowise.userservice.dto;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;

public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {

    public static ErrorResponseDto of(HttpStatus status, String message, String path) {
        return of(status, message, path, Map.of());
    }

    public static ErrorResponseDto of(HttpStatus status, String message, String path,
                                      Map<String, String> validationErrors) {
        return new ErrorResponseDto(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                message, path, validationErrors);
    }
}