package com.payment.settlementservice.controller;

import com.payment.common.SettlementReportDto;
import com.payment.settlementservice.entity.EntryType;
import com.payment.settlementservice.repository.SettlementEntryRepository;
import com.payment.settlementservice.repository.SettlementReportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller exposing settlement ledger entries and generated settlement reports.
 */
@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementEntryRepository settlementEntryRepository;
    private final SettlementReportRepository settlementReportRepository;

    public SettlementController(SettlementEntryRepository settlementEntryRepository,
                                SettlementReportRepository settlementReportRepository) {
        this.settlementEntryRepository = settlementEntryRepository;
        this.settlementReportRepository = settlementReportRepository;
    }

    @GetMapping("/entries")
    public ResponseEntity<List<SettlementEntryDto>> getEntries() {
        List<SettlementEntryDto> entries = settlementEntryRepository.findAll().stream()
                .map(entry -> new SettlementEntryDto(
                        entry.getId(),
                        entry.getPaymentId(),
                        entry.getEntryType(),
                        entry.getAmount(),
                        entry.getEntryDate(),
                        entry.getOccurredAt()
                ))
                .toList();
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<SettlementReportDto>> getReports() {
        List<SettlementReportDto> reports = settlementReportRepository.findAll().stream()
                .map(report -> new SettlementReportDto(
                        report.getReportDate(),
                        report.getTotalPayments(),
                        report.getTotalAmount()
                ))
                .toList();
        return ResponseEntity.ok(reports);
    }

    public record SettlementEntryDto(
            Long id,
            String paymentId,
            EntryType entryType,
            BigDecimal amount,
            LocalDate entryDate,
            Instant occurredAt
    ) {
    }
}
