package com.payment.paymentservice.repository;

import com.payment.paymentservice.entity.PaymentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecordEntity, Long> {
    List<PaymentRecordEntity> findByUserId(String userId);
}
