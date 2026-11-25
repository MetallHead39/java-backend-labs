package com.payment.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Event published after a payment is successfully completed and persisted.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCompletedEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("paymentId") String paymentId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("date") LocalDate date,
        @JsonProperty("occurredAt") Instant occurredAt
) {
}
