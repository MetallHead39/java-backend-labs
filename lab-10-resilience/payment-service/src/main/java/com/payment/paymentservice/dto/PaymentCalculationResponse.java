package com.payment.paymentservice.dto;

import java.math.BigDecimal;

/**
 * Breakdown response of a calculated payment.
 */
public record PaymentCalculationResponse(
        String merchantId,
        String date,
        BigDecimal amount,
        double feePercent,
        BigDecimal feeAmount,
        BigDecimal netAmount,
        double remainingLimitBefore,
        double remainingLimitAfter,
        String settlementDayStatus
) {
}
