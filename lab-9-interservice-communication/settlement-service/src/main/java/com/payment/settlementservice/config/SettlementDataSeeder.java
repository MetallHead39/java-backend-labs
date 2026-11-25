package com.payment.settlementservice.config;

import com.payment.settlementservice.entity.DailyLimitEntity;
import com.payment.settlementservice.entity.MerchantFeeEntity;
import com.payment.settlementservice.entity.SettlementDayEntity;
import com.payment.settlementservice.repository.DailyLimitRepository;
import com.payment.settlementservice.repository.MerchantFeeRepository;
import com.payment.settlementservice.repository.SettlementDayRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds initial settlement days, merchant fees, and daily limits on startup.
 */
@Component
public class SettlementDataSeeder implements ApplicationRunner {

    private final SettlementDayRepository settlementDayRepository;
    private final MerchantFeeRepository merchantFeeRepository;
    private final DailyLimitRepository dailyLimitRepository;

    public SettlementDataSeeder(SettlementDayRepository settlementDayRepository,
                                MerchantFeeRepository merchantFeeRepository,
                                DailyLimitRepository dailyLimitRepository) {
        this.settlementDayRepository = settlementDayRepository;
        this.merchantFeeRepository = merchantFeeRepository;
        this.dailyLimitRepository = dailyLimitRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (settlementDayRepository.count() == 0) {
            settlementDayRepository.saveAll(List.of(
                    new SettlementDayEntity("2026-11-25", "OPEN"),
                    new SettlementDayEntity("2026-11-24", "CLOSED")
            ));
        }

        if (merchantFeeRepository.count() == 0) {
            merchantFeeRepository.saveAll(List.of(
                    new MerchantFeeEntity("merchant-1", 1.5),
                    new MerchantFeeEntity("merchant-2", 2.2)
            ));
        }

        if (dailyLimitRepository.count() == 0) {
            dailyLimitRepository.saveAll(List.of(
                    new DailyLimitEntity("merchant-1", 5000.0),
                    new DailyLimitEntity("merchant-2", 1200.0)
            ));
        }
    }
}
