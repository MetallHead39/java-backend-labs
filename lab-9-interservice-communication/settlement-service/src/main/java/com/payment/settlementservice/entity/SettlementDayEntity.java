package com.payment.settlementservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity representing the settlement status of a calendar date.
 */
@Entity
@Table(name = "settlement_days")
public class SettlementDayEntity {

    @Id
    @Column(name = "settlement_date", nullable = false, length = 32)
    private String settlementDate;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected SettlementDayEntity() {
    }

    public SettlementDayEntity(String settlementDate, String status) {
        this.settlementDate = settlementDate;
        this.status = status;
    }

    public String getSettlementDate() {
        return settlementDate;
    }

    public String getStatus() {
        return status;
    }
}
