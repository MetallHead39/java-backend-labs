package com.payment.settlementservice.repository;

import com.payment.settlementservice.entity.SettlementDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementDayRepository extends JpaRepository<SettlementDayEntity, String> {
}
