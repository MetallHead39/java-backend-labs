package com.payment.paymentservice.exception;

/**
 * Thrown when a remote service is unavailable, times out, or returns a 5xx/unexpected error.
 */
public class RemoteServiceUnavailableException extends RuntimeException {

    public RemoteServiceUnavailableException(String message) {
        super(message);
    }

    public RemoteServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
