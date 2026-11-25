package com.payment.paymentservice.messaging;

import com.payment.common.event.BrokerConstants;
import com.payment.common.event.PaymentCompletedEvent;
import com.payment.common.event.PaymentRefundedEvent;
import com.payment.common.event.SettlementRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes domain events to RabbitMQ only after the originating database transaction commits.
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private static final MessagePostProcessor PERSISTENT_DELIVERY = message -> {
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        return message;
    };

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing PaymentCompletedEvent {} for payment {}", event.eventId(), event.paymentId());
        rabbitTemplate.convertAndSend(
                BrokerConstants.EXCHANGE_NAME,
                BrokerConstants.ROUTING_KEY_PAYMENT_COMPLETED,
                event,
                PERSISTENT_DELIVERY
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        log.info("Publishing PaymentRefundedEvent {} for payment {}", event.eventId(), event.paymentId());
        rabbitTemplate.convertAndSend(
                BrokerConstants.EXCHANGE_NAME,
                BrokerConstants.ROUTING_KEY_PAYMENT_REFUNDED,
                event,
                PERSISTENT_DELIVERY
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSettlementRequested(SettlementRequestedEvent event) {
        log.info("Publishing SettlementRequestedEvent {} for date {}", event.eventId(), event.date());
        rabbitTemplate.convertAndSend(
                BrokerConstants.EXCHANGE_NAME,
                BrokerConstants.ROUTING_KEY_SETTLEMENT_REQUESTED,
                event,
                PERSISTENT_DELIVERY
        );
    }
}
