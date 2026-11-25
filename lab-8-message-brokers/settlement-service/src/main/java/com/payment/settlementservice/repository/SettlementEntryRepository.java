package com.payment.settlementservice.repository;

import com.payment.settlementservice.entity.SettlementEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for {@link SettlementEntryEntity}.
 */
public interface SettlementEntryRepository extends JpaRepository<SettlementEntryEntity, Long> {

    List<SettlementEntryEntity> findByEntryDate(LocalDate entryDate);
}
