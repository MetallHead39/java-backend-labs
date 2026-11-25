package com.payment.paymentservice;

import com.payment.paymentservice.config.SettlementIntegrationProperties;
import com.payment.paymentservice.config.SettlementTransportValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementTransportValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"http", "HTTP", "Http", "grpc", "GRPC", "Grpc"})
    void shouldAcceptValidTransports(String transport) {
        SettlementIntegrationProperties properties = new SettlementIntegrationProperties(
                transport,
                null,
                null
        );
        SettlementTransportValidator validator = new SettlementTransportValidator(properties);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"kafka", "rabbitmq", "rest", "soap", "invalid"})
    void shouldFailFastOnInvalidTransport(String transport) {
        // If an explicit invalid transport is configured
        SettlementIntegrationProperties properties = new SettlementIntegrationProperties(
                transport,
                null,
                null
        );
        SettlementTransportValidator validator = new SettlementTransportValidator(properties);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid settlement transport");
    }
}
