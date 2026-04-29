package com.Spendsum.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 *
 * Converts unhandled RuntimeExceptions (e.g., "Budget not found", "Insight not found")
 * into structured JSON 500 responses instead of a raw Spring error page.
 *
 * This also makes controller error-case tests predictable (5xx with JSON body).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", ex.getMessage() != null ? ex.getMessage() : "Unexpected error",
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 500
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", ex.getMessage() != null ? ex.getMessage() : "Bad request",
                        "timestamp", LocalDateTime.now().toString(),
                        "status", 400
                ));
    }
}
