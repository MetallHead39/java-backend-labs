package com.payment.paymentservice.port;

import com.payment.common.dto.MerchantFeeInfo;

/**
 * Port for looking up merchant fee percentage.
 */
public interface MerchantFeeLookup {
    MerchantFeeInfo getMerchantFee(String merchantId);
}
