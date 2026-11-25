package com.payment.settlementservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity representing a merchant's remaining daily payment limit.
 */
@Entity
@Table(name = "daily_limits")
public class DailyLimitEntity {

    @Id
    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(name = "remaining_limit", nullable = false)
    private double remainingLimit;

    protected DailyLimitEntity() {
    }

    public DailyLimitEntity(String merchantId, double remainingLimit) {
        this.merchantId = merchantId;
        this.remainingLimit = remainingLimit;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public double getRemainingLimit() {
        return remainingLimit;
    }
}
