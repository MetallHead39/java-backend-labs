package com.payment.paymentservice.adapter.http;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative HTTP interface for querying settlement information.
 */
@HttpExchange("/api/settlements")
public interface SettlementHttpClient {

    @GetExchange("/days/{date}")
    SettlementDayInfo getSettlementDay(@PathVariable("date") String date);

    @GetExchange("/merchants/{merchantId}/fee")
    MerchantFeeInfo getMerchantFee(@PathVariable("merchantId") String merchantId);

    @GetExchange("/merchants/{merchantId}/limit")
    DailyLimitInfo getDailyLimit(@PathVariable("merchantId") String merchantId);
}
