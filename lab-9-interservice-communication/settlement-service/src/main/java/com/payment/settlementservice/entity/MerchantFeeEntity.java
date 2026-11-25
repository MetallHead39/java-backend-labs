package com.payment.settlementservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity representing a merchant's fee percentage.
 */
@Entity
@Table(name = "merchant_fees")
public class MerchantFeeEntity {

    @Id
    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(name = "fee_percent", nullable = false)
    private double feePercent;

    protected MerchantFeeEntity() {
    }

    public MerchantFeeEntity(String merchantId, double feePercent) {
        this.merchantId = merchantId;
        this.feePercent = feePercent;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public double getFeePercent() {
        return feePercent;
    }
}
