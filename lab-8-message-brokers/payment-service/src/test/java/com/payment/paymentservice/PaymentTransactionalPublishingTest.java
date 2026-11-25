package com.payment.paymentservice;

import com.payment.common.PaymentStatus;
import com.payment.common.event.BrokerConstants;
import com.payment.common.event.PaymentCompletedEvent;
import com.payment.common.event.PaymentRefundedEvent;
import com.payment.common.event.SettlementRequestedEvent;
import com.payment.paymentservice.entity.PaymentEntity;
import com.payment.paymentservice.repository.PaymentRepository;
import com.payment.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest
class PaymentTransactionalPublishingTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        reset(rabbitTemplate);
    }

    @Test
    void completePaymentPublishesPersistentMessageAfterCommit() {
        paymentService.completePayment("pay-ok-1", new BigDecimal("150.00"));

        PaymentEntity saved = paymentRepository.findById("pay-ok-1").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(saved.getAmount()).isEqualByComparingTo("150.00");

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        verify(rabbitTemplate).convertAndSend(
                eq(BrokerConstants.EXCHANGE_NAME),
                eq(BrokerConstants.ROUTING_KEY_PAYMENT_COMPLETED),
                eventCaptor.capture(),
                processorCaptor.capture()
        );
        verifyNoMoreInteractions(rabbitTemplate);

        PaymentCompletedEvent published = eventCaptor.getValue();
        assertThat(published.eventId()).isNotNull();
        assertThat(published.paymentId()).isEqualTo("pay-ok-1");
        assertThat(published.amount()).isEqualByComparingTo("150.00");
        assertThat(published.date()).isNotNull();
        assertThat(published.occurredAt()).isNotNull();

        Message processedMessage = processorCaptor.getValue()
                .postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertThat(processedMessage.getMessageProperties().getDeliveryMode())
                .isEqualTo(MessageDeliveryMode.PERSISTENT);
    }

    @Test
    void completePaymentWithNegativeAmountRollsBackAndPublishesZeroMessages() {
        assertThatThrownBy(() -> paymentService.completePayment("pay-neg-1", new BigDecimal("-25.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(paymentRepository.findById("pay-neg-1")).isEmpty();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void completePaymentWithZeroAmountRollsBackAndPublishesZeroMessages() {
        assertThatThrownBy(() -> paymentService.completePayment("pay-zero-1", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(paymentRepository.findById("pay-zero-1")).isEmpty();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void refundPaymentPublishesAfterCommitAndRejectsAlreadyRefunded() {
        paymentService.completePayment("pay-ref-1", new BigDecimal("80.00"));
        reset(rabbitTemplate);

        paymentService.refundPayment("pay-ref-1");

        PaymentEntity refunded = paymentRepository.findById("pay-ref-1").orElseThrow();
        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        ArgumentCaptor<PaymentRefundedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(BrokerConstants.EXCHANGE_NAME),
                eq(BrokerConstants.ROUTING_KEY_PAYMENT_REFUNDED),
                eventCaptor.capture(),
                processorCaptor.capture()
        );
        assertThat(eventCaptor.getValue().paymentId()).isEqualTo("pay-ref-1");
        assertThat(eventCaptor.getValue().amount()).isEqualByComparingTo("80.00");

        reset(rabbitTemplate);
        assertThatThrownBy(() -> paymentService.refundPayment("pay-ref-1"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void requestSettlementPublishesAfterCommit() {
        LocalDate settlementDate = LocalDate.of(2026, 11, 25);
        paymentService.requestSettlement(settlementDate);

        ArgumentCaptor<SettlementRequestedEvent> eventCaptor = ArgumentCaptor.forClass(SettlementRequestedEvent.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(BrokerConstants.EXCHANGE_NAME),
                eq(BrokerConstants.ROUTING_KEY_SETTLEMENT_REQUESTED),
                eventCaptor.capture(),
                processorCaptor.capture()
        );
        assertThat(eventCaptor.getValue().date()).isEqualTo(settlementDate);
        assertThat(eventCaptor.getValue().eventId()).isNotNull();
    }
}
