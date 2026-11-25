package com.payment.paymentservice.repository;

import com.payment.paymentservice.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, Long> {
    Optional<IdempotencyRecordEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}
