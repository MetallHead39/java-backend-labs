package com.payment.settlementservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA entity representing a generated daily settlement summary report.
 */
@Entity
@Table(name = "settlement_reports")
public class SettlementReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate reportDate;

    @Column(nullable = false)
    private int totalPayments;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Instant generatedAt;

    protected SettlementReportEntity() {
    }

    public SettlementReportEntity(LocalDate reportDate, int totalPayments, BigDecimal totalAmount, Instant generatedAt) {
        this.reportDate = reportDate;
        this.totalPayments = totalPayments;
        this.totalAmount = totalAmount;
        this.generatedAt = generatedAt;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public int getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(int totalPayments) {
        this.totalPayments = totalPayments;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
