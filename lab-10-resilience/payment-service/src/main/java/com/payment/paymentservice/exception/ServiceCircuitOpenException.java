package com.payment.paymentservice.exception;

public class ServiceCircuitOpenException extends RuntimeException {
    public ServiceCircuitOpenException(String message) {
        super(message);
    }

    public ServiceCircuitOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
