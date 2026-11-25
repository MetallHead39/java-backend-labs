package com.payment.paymentservice.service;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import com.payment.paymentservice.dto.PaymentCalculationRequest;
import com.payment.paymentservice.dto.PaymentCalculationResponse;
import com.payment.paymentservice.port.DailyLimitLookup;
import com.payment.paymentservice.port.MerchantFeeLookup;
import com.payment.paymentservice.port.SettlementDayLookup;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Business service orchestrating synchronous settlement lookups and payment calculations.
 * Depends ONLY on the 3 port interfaces.
 */
@Service
public class PaymentQuoteService {

    private final SettlementDayLookup settlementDayLookup;
    private final MerchantFeeLookup merchantFeeLookup;
    private final DailyLimitLookup dailyLimitLookup;

    public PaymentQuoteService(SettlementDayLookup settlementDayLookup,
                               MerchantFeeLookup merchantFeeLookup,
                               DailyLimitLookup dailyLimitLookup) {
        this.settlementDayLookup = settlementDayLookup;
        this.merchantFeeLookup = merchantFeeLookup;
        this.dailyLimitLookup = dailyLimitLookup;
    }

    public SettlementDayInfo checkDay(String date) {
        return settlementDayLookup.getDayStatus(date);
    }

    public MerchantFeeInfo getMerchantFee(String merchantId) {
        return merchantFeeLookup.getMerchantFee(merchantId);
    }

    public DailyLimitInfo getDailyLimit(String merchantId) {
        return dailyLimitLookup.getDailyLimit(merchantId);
    }

    public PaymentCalculationResponse calculate(PaymentCalculationRequest request) {
        if (request.merchantId() == null || request.merchantId().isBlank()) {
            throw new IllegalArgumentException("merchantId must not be blank");
        }
        if (request.date() == null || request.date().isBlank()) {
            throw new IllegalArgumentException("date must not be blank");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        SettlementDayInfo dayInfo = settlementDayLookup.getDayStatus(request.date());
        if (!"OPEN".equalsIgnoreCase(dayInfo.status())) {
            throw new IllegalStateException("Settlement day is not open: " + dayInfo.status());
        }

        DailyLimitInfo limitInfo = dailyLimitLookup.getDailyLimit(request.merchantId());
        if (request.amount().doubleValue() > limitInfo.remainingLimit()) {
            throw new IllegalStateException("Payment amount " + request.amount()
                    + " exceeds remaining daily limit " + limitInfo.remainingLimit());
        }

        MerchantFeeInfo feeInfo = merchantFeeLookup.getMerchantFee(request.merchantId());

        BigDecimal feePercent = BigDecimal.valueOf(feeInfo.feePercent());
        BigDecimal feeAmount = request.amount().multiply(feePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = request.amount().subtract(feeAmount);
        double remainingLimitAfter = limitInfo.remainingLimit() - request.amount().doubleValue();

        return new PaymentCalculationResponse(
                request.merchantId(),
                request.date(),
                request.amount(),
                feeInfo.feePercent(),
                feeAmount,
                netAmount,
                limitInfo.remainingLimit(),
                remainingLimitAfter,
                dayInfo.status()
        );
    }
}
