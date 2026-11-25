package com.payment.settlementservice.service;

/**
 * Exception thrown when a requested settlement day or merchant record does not exist.
 */
public class SettlementResourceNotFoundException extends RuntimeException {

    public SettlementResourceNotFoundException(String message) {
        super(message);
    }
}
