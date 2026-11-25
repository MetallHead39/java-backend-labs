package com.payment.common.dto;

/**
 * Merchant processing fee percentage information.
 */
public record MerchantFeeInfo(String merchantId, double feePercent) {
}
