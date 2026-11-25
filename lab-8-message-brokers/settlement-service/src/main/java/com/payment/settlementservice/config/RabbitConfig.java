package com.payment.settlementservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.event.BrokerConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ consumer topology configuration with durable exchange, durable queues, and JSON converter.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange(BrokerConstants.EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(BrokerConstants.QUEUE_PAYMENT_COMPLETED).build();
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return QueueBuilder.durable(BrokerConstants.QUEUE_PAYMENT_REFUNDED).build();
    }

    @Bean
    public Queue settlementRequestedQueue() {
        return QueueBuilder.durable(BrokerConstants.QUEUE_SETTLEMENT_REQUESTED).build();
    }

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, TopicExchange paymentEventsExchange) {
        return BindingBuilder.bind(paymentCompletedQueue)
                .to(paymentEventsExchange)
                .with(BrokerConstants.ROUTING_KEY_PAYMENT_COMPLETED);
    }

    @Bean
    public Binding paymentRefundedBinding(Queue paymentRefundedQueue, TopicExchange paymentEventsExchange) {
        return BindingBuilder.bind(paymentRefundedQueue)
                .to(paymentEventsExchange)
                .with(BrokerConstants.ROUTING_KEY_PAYMENT_REFUNDED);
    }

    @Bean
    public Binding settlementRequestedBinding(Queue settlementRequestedQueue, TopicExchange paymentEventsExchange) {
        return BindingBuilder.bind(settlementRequestedQueue)
                .to(paymentEventsExchange)
                .with(BrokerConstants.ROUTING_KEY_SETTLEMENT_REQUESTED);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
