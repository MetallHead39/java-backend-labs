package com.payment.settlementservice.service;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import com.payment.settlementservice.repository.DailyLimitRepository;
import com.payment.settlementservice.repository.MerchantFeeRepository;
import com.payment.settlementservice.repository.SettlementDayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business query service backing both REST and gRPC settlement lookup endpoints.
 */
@Service
@Transactional(readOnly = true)
public class SettlementQueryService {

    private final SettlementDayRepository settlementDayRepository;
    private final MerchantFeeRepository merchantFeeRepository;
    private final DailyLimitRepository dailyLimitRepository;

    public SettlementQueryService(SettlementDayRepository settlementDayRepository,
                                  MerchantFeeRepository merchantFeeRepository,
                                  DailyLimitRepository dailyLimitRepository) {
        this.settlementDayRepository = settlementDayRepository;
        this.merchantFeeRepository = merchantFeeRepository;
        this.dailyLimitRepository = dailyLimitRepository;
    }

    public SettlementDayInfo getSettlementDay(String date) {
        return settlementDayRepository.findById(date)
                .map(entity -> new SettlementDayInfo(entity.getSettlementDate(), entity.getStatus()))
                .orElseThrow(() -> new SettlementResourceNotFoundException("Settlement day not found: " + date));
    }

    public MerchantFeeInfo getMerchantFee(String merchantId) {
        return merchantFeeRepository.findById(merchantId)
                .map(entity -> new MerchantFeeInfo(entity.getMerchantId(), entity.getFeePercent()))
                .orElseThrow(() -> new SettlementResourceNotFoundException("Merchant fee not found: " + merchantId));
    }

    public DailyLimitInfo getDailyLimit(String merchantId) {
        return dailyLimitRepository.findById(merchantId)
                .map(entity -> new DailyLimitInfo(entity.getMerchantId(), entity.getRemainingLimit()))
                .orElseThrow(() -> new SettlementResourceNotFoundException("Daily limit not found: " + merchantId));
    }
}
