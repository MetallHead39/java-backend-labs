package com.payment.settlementservice.repository;

import com.payment.settlementservice.entity.SettlementReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link SettlementReportEntity}.
 */
public interface SettlementReportRepository extends JpaRepository<SettlementReportEntity, Long> {

    Optional<SettlementReportEntity> findByReportDate(LocalDate reportDate);
}
