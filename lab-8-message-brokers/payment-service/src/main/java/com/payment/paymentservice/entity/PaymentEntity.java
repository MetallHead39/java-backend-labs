package com.payment.paymentservice.entity;

import com.payment.common.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA entity representing a persisted payment record.
 */
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PaymentEntity() {
    }

    public PaymentEntity(String id, BigDecimal amount, PaymentStatus status, LocalDate paymentDate, Instant updatedAt) {
        this.id = id;
        this.amount = amount;
        this.status = status;
        this.paymentDate = paymentDate;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
