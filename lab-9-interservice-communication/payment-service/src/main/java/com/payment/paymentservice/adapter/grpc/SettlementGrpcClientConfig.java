package com.payment.paymentservice.adapter.grpc;

import com.payment.paymentservice.config.SettlementIntegrationProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Spring configuration providing the ManagedChannel for gRPC communication with settlement-service.
 */
@Configuration
@ConditionalOnProperty(name = "integration.settlement.transport", havingValue = "grpc")
public class SettlementGrpcClientConfig {

    private static final Logger log = LoggerFactory.getLogger(SettlementGrpcClientConfig.class);
    private ManagedChannel channel;

    @Bean
    public ManagedChannel settlementGrpcChannel(SettlementIntegrationProperties properties) {
        log.info("Configuring gRPC channel to {}:{}", properties.grpc().host(), properties.grpc().port());
        this.channel = ManagedChannelBuilder.forAddress(properties.grpc().host(), properties.grpc().port())
                .usePlaintext()
                .build();
        return this.channel;
    }

    @PreDestroy
    public void shutdownChannel() {
        if (channel != null && !channel.isShutdown()) {
            log.info("Shutting down gRPC channel");
            channel.shutdown();
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                channel.shutdownNow();
            }
        }
    }
}
