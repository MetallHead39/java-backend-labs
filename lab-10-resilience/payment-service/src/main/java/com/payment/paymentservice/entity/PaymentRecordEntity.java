package com.payment.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_records")
public class PaymentRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String settlementDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private double feePercent;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    public PaymentRecordEntity() {
    }

    public PaymentRecordEntity(String userId,
                               String merchantId,
                               String settlementDate,
                               BigDecimal amount,
                               double feePercent,
                               BigDecimal feeAmount,
                               BigDecimal netAmount,
                               String status,
                               Instant createdAt) {
        this.userId = userId;
        this.merchantId = merchantId;
        this.settlementDate = settlementDate;
        this.amount = amount;
        this.feePercent = feePercent;
        this.feeAmount = feeAmount;
        this.netAmount = netAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(String settlementDate) {
        this.settlementDate = settlementDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public double getFeePercent() {
        return feePercent;
    }

    public void setFeePercent(double feePercent) {
        this.feePercent = feePercent;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
