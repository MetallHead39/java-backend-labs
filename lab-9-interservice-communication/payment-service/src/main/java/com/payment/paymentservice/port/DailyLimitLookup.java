package com.payment.paymentservice.port;

import com.payment.common.dto.DailyLimitInfo;

/**
 * Port for looking up merchant daily limit.
 */
public interface DailyLimitLookup {
    DailyLimitInfo getDailyLimit(String merchantId);
}
