package com.payment.settlementservice.repository;

import com.payment.settlementservice.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ProcessedEventEntity}.
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}
