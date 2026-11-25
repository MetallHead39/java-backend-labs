package com.payment.settlementservice;

import com.payment.common.event.PaymentCompletedEvent;
import com.payment.common.event.PaymentRefundedEvent;
import com.payment.common.event.SettlementRequestedEvent;
import com.payment.settlementservice.entity.EntryType;
import com.payment.settlementservice.entity.ProcessedEventEntity;
import com.payment.settlementservice.entity.SettlementEntryEntity;
import com.payment.settlementservice.entity.SettlementReportEntity;
import com.payment.settlementservice.messaging.SettlementEventHandler;
import com.payment.settlementservice.repository.ProcessedEventRepository;
import com.payment.settlementservice.repository.SettlementEntryRepository;
import com.payment.settlementservice.repository.SettlementReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementEventHandlerIdempotencyTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private SettlementEntryRepository settlementEntryRepository;

    @Mock
    private SettlementReportRepository settlementReportRepository;

    @InjectMocks
    private SettlementEventHandler settlementEventHandler;

    @Test
    void handlePaymentCompletedProcessesDuplicateEventIdOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 11, 25);
        Instant now = Instant.now();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                eventId, "pay-1", new BigDecimal("100.00"), date, now
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false).thenReturn(true);

        settlementEventHandler.handlePaymentCompleted(event);
        settlementEventHandler.handlePaymentCompleted(event);

        verify(processedEventRepository, times(2)).existsById(eventId);
        verify(processedEventRepository, times(1)).save(any(ProcessedEventEntity.class));

        ArgumentCaptor<SettlementEntryEntity> entryCaptor = ArgumentCaptor.forClass(SettlementEntryEntity.class);
        verify(settlementEntryRepository, times(1)).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getPaymentId()).isEqualTo("pay-1");
        assertThat(entryCaptor.getValue().getEntryType()).isEqualTo(EntryType.PAYMENT_COMPLETED);
        assertThat(entryCaptor.getValue().getAmount()).isEqualByComparingTo("100.00");
        verifyNoInteractions(settlementReportRepository);
    }

    @Test
    void handlePaymentRefundedProcessesDuplicateEventIdOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 11, 25);
        Instant now = Instant.now();
        PaymentRefundedEvent event = new PaymentRefundedEvent(
                eventId, "pay-2", new BigDecimal("40.00"), date, now
        );

        when(processedEventRepository.existsById(eventId)).thenReturn(false).thenReturn(true);

        settlementEventHandler.handlePaymentRefunded(event);
        settlementEventHandler.handlePaymentRefunded(event);

        verify(processedEventRepository, times(1)).save(any(ProcessedEventEntity.class));

        ArgumentCaptor<SettlementEntryEntity> entryCaptor = ArgumentCaptor.forClass(SettlementEntryEntity.class);
        verify(settlementEntryRepository, times(1)).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getPaymentId()).isEqualTo("pay-2");
        assertThat(entryCaptor.getValue().getEntryType()).isEqualTo(EntryType.PAYMENT_REFUNDED);
        assertThat(entryCaptor.getValue().getAmount()).isEqualByComparingTo("-40.00");
        verifyNoInteractions(settlementReportRepository);
    }

    @Test
    void handleSettlementRequestedProcessesDuplicateEventIdOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 11, 25);
        Instant now = Instant.now();
        SettlementRequestedEvent event = new SettlementRequestedEvent(eventId, date, now);

        when(processedEventRepository.existsById(eventId)).thenReturn(false).thenReturn(true);
        when(settlementEntryRepository.findByEntryDate(date)).thenReturn(List.of(
                new SettlementEntryEntity("pay-1", EntryType.PAYMENT_COMPLETED, new BigDecimal("100.00"), date, now),
                new SettlementEntryEntity("pay-1", EntryType.PAYMENT_REFUNDED, new BigDecimal("-25.00"), date, now)
        ));
        when(settlementReportRepository.findByReportDate(date)).thenReturn(Optional.empty());

        settlementEventHandler.handleSettlementRequested(event);
        settlementEventHandler.handleSettlementRequested(event);

        verify(processedEventRepository, times(1)).save(any(ProcessedEventEntity.class));
        verify(settlementEntryRepository, times(1)).findByEntryDate(date);

        ArgumentCaptor<SettlementReportEntity> reportCaptor = ArgumentCaptor.forClass(SettlementReportEntity.class);
        verify(settlementReportRepository, times(1)).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getReportDate()).isEqualTo(date);
        assertThat(reportCaptor.getValue().getTotalPayments()).isEqualTo(1);
        assertThat(reportCaptor.getValue().getTotalAmount()).isEqualByComparingTo("75.00");
    }
}
