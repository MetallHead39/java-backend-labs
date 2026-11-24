package com.example.quartz.service;

import com.example.quartz.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class PaymentMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMaintenanceService.class);

    private final PaymentRepository paymentRepository;
    private final SettlementReportRepository reportRepository;

    public PaymentMaintenanceService(PaymentRepository paymentRepository,
                                     SettlementReportRepository reportRepository) {
        this.paymentRepository = paymentRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional
    public int expireStalePendingPayments(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(timeoutMinutes);
        List<Payment> stale = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, threshold);
        if (stale.isEmpty()) {
            return 0;
        }
        stale.forEach(p -> p.setStatus(PaymentStatus.EXPIRED));
        paymentRepository.saveAll(stale);
        log.info("Expired {} stale PENDING payments older than {} min", stale.size(), timeoutMinutes);
        return stale.size();
    }

    @Transactional
    public SettlementReport generateDailySettlementReport(LocalDate reportDate, String currency) {
        return reportRepository.findByReportDateAndCurrency(reportDate, currency)
                .orElseGet(() -> {
                    LocalDateTime start = reportDate.atStartOfDay();
                    LocalDateTime end = reportDate.plusDays(1).atStartOfDay();
                    List<Payment> completed = paymentRepository.findByStatusAndCurrencyAndCreatedAtBetween(
                            PaymentStatus.COMPLETED, currency, start, end);

                    BigDecimal total = completed.stream()
                            .map(Payment::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    SettlementReport report = new SettlementReport(
                            reportDate, currency, total, completed.size(), LocalDateTime.now(ZoneOffset.UTC));
                    log.info("Generated daily report for {} [{}]: count={}, total={}",
                            reportDate, currency, completed.size(), total);
                    return reportRepository.save(report);
                });
    }
}
