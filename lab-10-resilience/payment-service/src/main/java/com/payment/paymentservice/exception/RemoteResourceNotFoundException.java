package com.payment.paymentservice.exception;

/**
 * Thrown when a remote resource is not found (HTTP 404 or gRPC NOT_FOUND).
 */
public class RemoteResourceNotFoundException extends RuntimeException {

    public RemoteResourceNotFoundException(String message) {
        super(message);
    }

    public RemoteResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
