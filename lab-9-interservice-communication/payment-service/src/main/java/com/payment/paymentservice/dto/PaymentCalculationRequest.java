package com.payment.paymentservice.dto;

import java.math.BigDecimal;

/**
 * Request payload for payment quote/fee/limit calculation.
 */
public record PaymentCalculationRequest(
        String merchantId,
        String date,
        BigDecimal amount
) {
}
