package com.payment.common.event;

/**
 * Shared RabbitMQ exchange, queue, and routing key constants.
 */
public final class BrokerConstants {

    public static final String EXCHANGE_NAME = "payment.events.exchange";

    public static final String QUEUE_PAYMENT_COMPLETED = "settlement.payment.completed.queue";
    public static final String QUEUE_PAYMENT_REFUNDED = "settlement.payment.refunded.queue";
    public static final String QUEUE_SETTLEMENT_REQUESTED = "settlement.requested.queue";

    public static final String ROUTING_KEY_PAYMENT_COMPLETED = "payment.completed";
    public static final String ROUTING_KEY_PAYMENT_REFUNDED = "payment.refunded";
    public static final String ROUTING_KEY_SETTLEMENT_REQUESTED = "settlement.requested";

    private BrokerConstants() {
    }
}
