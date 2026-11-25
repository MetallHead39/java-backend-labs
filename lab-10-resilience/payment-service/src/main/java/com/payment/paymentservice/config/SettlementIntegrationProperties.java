package com.payment.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Integration properties for settlement service communication via HTTP or gRPC.
 */
@ConfigurationProperties(prefix = "integration.settlement")
public record SettlementIntegrationProperties(
        String transport,
        HttpProperties http,
        GrpcProperties grpc
) {
    public SettlementIntegrationProperties {
        if (transport == null || transport.isBlank()) {
            transport = "http";
        }
        if (http == null) {
            http = new HttpProperties("http://localhost:8082", 2000, 3000);
        }
        if (grpc == null) {
            grpc = new GrpcProperties("localhost", 9090, 2000);
        }
    }

    public record HttpProperties(
            String baseUrl,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        public HttpProperties {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "http://localhost:8082";
            }
            if (connectTimeoutMs <= 0) {
                connectTimeoutMs = 2000;
            }
            if (readTimeoutMs <= 0) {
                readTimeoutMs = 3000;
            }
        }
    }

    public record GrpcProperties(
            String host,
            int port,
            int deadlineMs
    ) {
        public GrpcProperties {
            if (host == null || host.isBlank()) {
                host = "localhost";
            }
            if (port <= 0) {
                port = 9090;
            }
            if (deadlineMs <= 0) {
                deadlineMs = 2000;
            }
        }
    }
}
