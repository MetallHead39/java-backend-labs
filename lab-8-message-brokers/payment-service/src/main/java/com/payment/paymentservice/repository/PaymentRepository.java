package com.payment.paymentservice.repository;

import com.payment.paymentservice.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link PaymentEntity}.
 */
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
}
