package com.payment.common.dto;

/**
 * Merchant remaining daily payment limit information.
 */
public record DailyLimitInfo(String merchantId, double remainingLimit) {
}
