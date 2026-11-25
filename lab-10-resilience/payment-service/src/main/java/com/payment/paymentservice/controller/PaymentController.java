package com.payment.paymentservice.controller;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import com.payment.paymentservice.dto.PaymentCalculationRequest;
import com.payment.paymentservice.dto.PaymentCalculationResponse;
import com.payment.paymentservice.port.DailyLimitLookup;
import com.payment.paymentservice.port.MerchantFeeLookup;
import com.payment.paymentservice.port.SettlementDayLookup;
import com.payment.paymentservice.service.PaymentQuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for payment lookups and calculations.
 * Depends ONLY on the 3 port interfaces (or the business service that wraps them).
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final SettlementDayLookup settlementDayLookup;
    private final MerchantFeeLookup merchantFeeLookup;
    private final DailyLimitLookup dailyLimitLookup;
    private final PaymentQuoteService paymentQuoteService;
    private final com.payment.paymentservice.service.IdempotentPaymentService idempotentPaymentService;

    public PaymentController(SettlementDayLookup settlementDayLookup,
                             MerchantFeeLookup merchantFeeLookup,
                             DailyLimitLookup dailyLimitLookup,
                             PaymentQuoteService paymentQuoteService,
                             com.payment.paymentservice.service.IdempotentPaymentService idempotentPaymentService) {
        this.settlementDayLookup = settlementDayLookup;
        this.merchantFeeLookup = merchantFeeLookup;
        this.dailyLimitLookup = dailyLimitLookup;
        this.paymentQuoteService = paymentQuoteService;
        this.idempotentPaymentService = idempotentPaymentService;
    }

    @GetMapping("/check-day/{date}")
    public ResponseEntity<SettlementDayInfo> checkDay(@PathVariable String date) {
        return ResponseEntity.ok(settlementDayLookup.getDayStatus(date));
    }

    @GetMapping("/merchants/{merchantId}/fee")
    public ResponseEntity<MerchantFeeInfo> getMerchantFee(@PathVariable String merchantId) {
        return ResponseEntity.ok(merchantFeeLookup.getMerchantFee(merchantId));
    }

    @GetMapping("/merchants/{merchantId}/limit")
    public ResponseEntity<DailyLimitInfo> getDailyLimit(@PathVariable String merchantId) {
        return ResponseEntity.ok(dailyLimitLookup.getDailyLimit(merchantId));
    }

    @PostMapping("/calculate")
    public ResponseEntity<?> calculate(
            @RequestBody PaymentCalculationRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String userId) {
        return idempotentPaymentService.calculateWithIdempotency(request, idempotencyKey, userId);
    }
}
