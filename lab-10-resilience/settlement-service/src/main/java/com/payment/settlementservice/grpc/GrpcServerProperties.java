package com.payment.settlementservice.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the embedded gRPC server.
 */
@ConfigurationProperties(prefix = "grpc.server")
public record GrpcServerProperties(boolean enabled, int port) {

    public GrpcServerProperties {
        if (port == 0 && enabled) {
            // Port 0 is allowed for ephemeral port allocation in tests.
        } else if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid gRPC server port: " + port);
        }
    }
}
