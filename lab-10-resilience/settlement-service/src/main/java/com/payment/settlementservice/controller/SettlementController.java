package com.payment.settlementservice.controller;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import com.payment.settlementservice.service.SettlementQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing synchronous settlement lookup endpoints.
 */
@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementQueryService settlementQueryService;

    public SettlementController(SettlementQueryService settlementQueryService) {
        this.settlementQueryService = settlementQueryService;
    }

    @GetMapping("/days/{date}")
    public ResponseEntity<SettlementDayInfo> getSettlementDay(@PathVariable String date) {
        return ResponseEntity.ok(settlementQueryService.getSettlementDay(date));
    }

    @GetMapping("/merchants/{merchantId}/fee")
    public ResponseEntity<MerchantFeeInfo> getMerchantFee(@PathVariable String merchantId) {
        return ResponseEntity.ok(settlementQueryService.getMerchantFee(merchantId));
    }

    @GetMapping("/merchants/{merchantId}/limit")
    public ResponseEntity<DailyLimitInfo> getDailyLimit(@PathVariable String merchantId) {
        return ResponseEntity.ok(settlementQueryService.getDailyLimit(merchantId));
    }
}
