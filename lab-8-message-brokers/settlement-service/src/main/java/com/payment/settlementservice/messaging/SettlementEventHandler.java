package com.payment.settlementservice.messaging;

import com.payment.common.event.BrokerConstants;
import com.payment.common.event.PaymentCompletedEvent;
import com.payment.common.event.PaymentRefundedEvent;
import com.payment.common.event.SettlementRequestedEvent;
import com.payment.settlementservice.entity.EntryType;
import com.payment.settlementservice.entity.ProcessedEventEntity;
import com.payment.settlementservice.entity.SettlementEntryEntity;
import com.payment.settlementservice.entity.SettlementReportEntity;
import com.payment.settlementservice.repository.ProcessedEventRepository;
import com.payment.settlementservice.repository.SettlementEntryRepository;
import com.payment.settlementservice.repository.SettlementReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Idempotent RabbitMQ event consumer for payment and settlement events.
 */
@Component
public class SettlementEventHandler {

    private static final Logger log = LoggerFactory.getLogger(SettlementEventHandler.class);

    private final ProcessedEventRepository processedEventRepository;
    private final SettlementEntryRepository settlementEntryRepository;
    private final SettlementReportRepository settlementReportRepository;

    public SettlementEventHandler(ProcessedEventRepository processedEventRepository,
                                  SettlementEntryRepository settlementEntryRepository,
                                  SettlementReportRepository settlementReportRepository) {
        this.processedEventRepository = processedEventRepository;
        this.settlementEntryRepository = settlementEntryRepository;
        this.settlementReportRepository = settlementReportRepository;
    }

    @RabbitListener(queues = BrokerConstants.QUEUE_PAYMENT_COMPLETED)
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (isAlreadyProcessed(event.eventId(), "PaymentCompletedEvent")) {
            return;
        }
        processedEventRepository.save(new ProcessedEventEntity(event.eventId(), "PAYMENT_COMPLETED", Instant.now()));
        settlementEntryRepository.save(new SettlementEntryEntity(
                event.paymentId(),
                EntryType.PAYMENT_COMPLETED,
                event.amount(),
                event.date(),
                event.occurredAt()
        ));
        log.info("Recorded completed payment {} (event {})", event.paymentId(), event.eventId());
    }

    @RabbitListener(queues = BrokerConstants.QUEUE_PAYMENT_REFUNDED)
    @Transactional
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        if (isAlreadyProcessed(event.eventId(), "PaymentRefundedEvent")) {
            return;
        }
        processedEventRepository.save(new ProcessedEventEntity(event.eventId(), "PAYMENT_REFUNDED", Instant.now()));
        settlementEntryRepository.save(new SettlementEntryEntity(
                event.paymentId(),
                EntryType.PAYMENT_REFUNDED,
                event.amount().negate(),
                event.date(),
                event.occurredAt()
        ));
        log.info("Recorded refunded payment {} (event {})", event.paymentId(), event.eventId());
    }

    @RabbitListener(queues = BrokerConstants.QUEUE_SETTLEMENT_REQUESTED)
    @Transactional
    public void handleSettlementRequested(SettlementRequestedEvent event) {
        if (isAlreadyProcessed(event.eventId(), "SettlementRequestedEvent")) {
            return;
        }
        processedEventRepository.save(new ProcessedEventEntity(event.eventId(), "SETTLEMENT_REQUESTED", Instant.now()));

        List<SettlementEntryEntity> entries = settlementEntryRepository.findByEntryDate(event.date());
        int totalPayments = (int) entries.stream()
                .filter(entry -> entry.getEntryType() == EntryType.PAYMENT_COMPLETED)
                .count();
        BigDecimal totalAmount = entries.stream()
                .map(SettlementEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant now = Instant.now();
        SettlementReportEntity report = settlementReportRepository.findByReportDate(event.date())
                .map(existing -> {
                    existing.setTotalPayments(totalPayments);
                    existing.setTotalAmount(totalAmount);
                    existing.setGeneratedAt(now);
                    return existing;
                })
                .orElseGet(() -> new SettlementReportEntity(event.date(), totalPayments, totalAmount, now));

        settlementReportRepository.save(report);
        log.info("Generated settlement report for {} (event {}): {} payments, total {}",
                event.date(), event.eventId(), totalPayments, totalAmount);
    }

    private boolean isAlreadyProcessed(UUID eventId, String eventName) {
        if (processedEventRepository.existsById(eventId)) {
            log.info("Skipping duplicate {} with eventId {}", eventName, eventId);
            return true;
        }
        return false;
    }
}
