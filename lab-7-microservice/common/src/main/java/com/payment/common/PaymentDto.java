package com.payment.common;

import java.math.BigDecimal;

/**
 * Data transfer object representing a payment.
 */
public record PaymentDto(String id, BigDecimal amount, PaymentStatus status) {
}
