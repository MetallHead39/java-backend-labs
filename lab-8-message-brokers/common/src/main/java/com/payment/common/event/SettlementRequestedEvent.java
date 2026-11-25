package com.payment.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Event published when a daily settlement close is requested.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SettlementRequestedEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("date") LocalDate date,
        @JsonProperty("occurredAt") Instant occurredAt
) {
}
