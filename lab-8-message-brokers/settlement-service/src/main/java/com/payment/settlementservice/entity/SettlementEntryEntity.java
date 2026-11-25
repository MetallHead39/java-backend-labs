package com.payment.settlementservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA entity representing an immutable ledger entry in the settlement service.
 */
@Entity
@Table(name = "settlement_entries")
public class SettlementEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private Instant occurredAt;

    protected SettlementEntryEntity() {
    }

    public SettlementEntryEntity(String paymentId, EntryType entryType, BigDecimal amount,
                                 LocalDate entryDate, Instant occurredAt) {
        this.paymentId = paymentId;
        this.entryType = entryType;
        this.amount = amount;
        this.entryDate = entryDate;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
