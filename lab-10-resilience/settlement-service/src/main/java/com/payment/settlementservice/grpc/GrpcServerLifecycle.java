package com.payment.settlementservice.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the embedded gRPC server lifecycle alongside the Spring ApplicationContext.
 */
@Component
public class GrpcServerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final GrpcServerProperties properties;
    private final SettlementGrpcServiceImpl settlementGrpcService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Server server;

    public GrpcServerLifecycle(GrpcServerProperties properties,
                               SettlementGrpcServiceImpl settlementGrpcService) {
        this.properties = properties;
        this.settlementGrpcService = settlementGrpcService;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.enabled();
    }

    @Override
    public void start() {
        if (!properties.enabled() || running.get()) {
            return;
        }
        try {
            server = ServerBuilder.forPort(properties.port())
                    .addService(settlementGrpcService)
                    .build()
                    .start();
            running.set(true);
            log.info("gRPC server started on port {}", server.getPort());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start gRPC server on port " + properties.port(), ex);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            log.info("Shutting down gRPC server on port {}", server.getPort());
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                server.shutdownNow();
            } finally {
                server = null;
                running.set(false);
            }
        } else {
            running.set(false);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    public int getPort() {
        return server != null ? server.getPort() : properties.port();
    }
}
