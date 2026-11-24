package com.example.quartz.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_reports")
public class SettlementReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "payments_count", nullable = false)
    private int paymentsCount;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public SettlementReport() {}

    public SettlementReport(LocalDate reportDate, String currency, BigDecimal totalAmount, int paymentsCount, LocalDateTime generatedAt) {
        this.reportDate = reportDate;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.paymentsCount = paymentsCount;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public LocalDate getReportDate() { return reportDate; }
    public String getCurrency() { return currency; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getPaymentsCount() { return paymentsCount; }
}
