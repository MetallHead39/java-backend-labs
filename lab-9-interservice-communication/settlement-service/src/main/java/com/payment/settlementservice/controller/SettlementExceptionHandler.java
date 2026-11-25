package com.payment.settlementservice.controller;

import com.payment.settlementservice.service.SettlementResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps settlement domain exceptions to HTTP status responses.
 */
@RestControllerAdvice
public class SettlementExceptionHandler {

    @ExceptionHandler(SettlementResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(SettlementResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
