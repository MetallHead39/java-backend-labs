package com.payment.settlementservice.repository;

import com.payment.settlementservice.entity.DailyLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyLimitRepository extends JpaRepository<DailyLimitEntity, String> {
}
