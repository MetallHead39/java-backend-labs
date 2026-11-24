package com.example.quartz.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime threshold);
    List<Payment> findByStatusAndCurrencyAndCreatedAtBetween(
            PaymentStatus status, String currency, LocalDateTime start, LocalDateTime end);
}
