package com.example.quartz.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface SettlementReportRepository extends JpaRepository<SettlementReport, Long> {
    Optional<SettlementReport> findByReportDateAndCurrency(LocalDate reportDate, String currency);
}
