package com.payment.paymentservice.service;

/**
 * Thrown when a referenced payment cannot be found.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
