package com.payment.paymentservice.controller;

import com.payment.paymentservice.exception.RemoteResourceNotFoundException;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Unified REST controller advice translating remote service exceptions and domain errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RemoteResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRemoteNotFound(RemoteResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RemoteServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleRemoteUnavailable(RemoteServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(com.payment.paymentservice.exception.ServiceCircuitOpenException.class)
    public ResponseEntity<Map<String, String>> handleCircuitOpen(com.payment.paymentservice.exception.ServiceCircuitOpenException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}
