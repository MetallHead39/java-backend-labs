package com.payment.paymentservice.service;

import com.payment.common.PaymentDto;
import com.payment.common.PaymentStatus;
import com.payment.common.event.PaymentCompletedEvent;
import com.payment.common.event.PaymentRefundedEvent;
import com.payment.common.event.SettlementRequestedEvent;
import com.payment.paymentservice.entity.PaymentEntity;
import com.payment.paymentservice.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Business service for payment completion, refunds, and daily settlement closure requests.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository, ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentDto completePayment(String id, BigDecimal amount) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Payment id must not be blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (paymentRepository.existsById(id)) {
            throw new IllegalStateException("Payment already exists with id: " + id);
        }

        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        PaymentEntity entity = new PaymentEntity(id, amount, PaymentStatus.COMPLETED, today, now);
        PaymentEntity saved = paymentRepository.save(entity);

        eventPublisher.publishEvent(new PaymentCompletedEvent(
                UUID.randomUUID(),
                saved.getId(),
                saved.getAmount(),
                saved.getPaymentDate(),
                now
        ));

        return new PaymentDto(saved.getId(), saved.getAmount(), saved.getStatus());
    }

    @Transactional
    public PaymentDto refundPayment(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Payment id must not be blank");
        }

        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Payment already refunded: " + id);
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only completed payments can be refunded: " + id);
        }

        Instant now = Instant.now();
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(now);
        PaymentEntity saved = paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentRefundedEvent(
                UUID.randomUUID(),
                saved.getId(),
                saved.getAmount(),
                saved.getPaymentDate(),
                now
        ));

        return new PaymentDto(saved.getId(), saved.getAmount(), saved.getStatus());
    }

    @Transactional
    public SettlementRequestedEvent requestSettlement(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Settlement date must not be null");
        }

        SettlementRequestedEvent event = new SettlementRequestedEvent(
                UUID.randomUUID(),
                date,
                Instant.now()
        );
        eventPublisher.publishEvent(event);
        return event;
    }
}
