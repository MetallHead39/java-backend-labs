package com.payment.paymentservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Validates the configured settlement transport on application startup.
 * Fails fast by throwing IllegalStateException if transport is neither "http" nor "grpc".
 */
@Component
public class SettlementTransportValidator {

    private final SettlementIntegrationProperties properties;

    public SettlementTransportValidator(SettlementIntegrationProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validate() {
        String transport = properties.transport();
        if (transport == null ||
                (!transport.trim().equalsIgnoreCase("http") && !transport.trim().equalsIgnoreCase("grpc"))) {
            throw new IllegalStateException("Invalid settlement transport: '" + transport
                    + "'. Supported transports are 'http' and 'grpc' (case-insensitive).");
        }
    }
}
