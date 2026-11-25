package com.payment.paymentservice.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResilienceEventListener {

    private static final Logger log = LoggerFactory.getLogger(ResilienceEventListener.class);

    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ResilienceEventListener(RetryRegistry retryRegistry,
                                   CircuitBreakerRegistry circuitBreakerRegistry) {
        this.retryRegistry = retryRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @PostConstruct
    public void registerEventListeners() {
        retryRegistry.getAllRetries().forEach(this::registerRetryEvents);
        retryRegistry.getEventPublisher().onEntryAdded(entryAddedEvent ->
                registerRetryEvents(entryAddedEvent.getAddedEntry()));

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerCircuitBreakerEvents);
        circuitBreakerRegistry.getEventPublisher().onEntryAdded(entryAddedEvent ->
                registerCircuitBreakerEvents(entryAddedEvent.getAddedEntry()));
    }

    private void registerRetryEvents(Retry retry) {
        retry.getEventPublisher().onRetry(event ->
                log.warn("[Resilience-Retry] Retry attempt #{} for '{}' due to: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getName(),
                        event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "unknown"));
    }

    private void registerCircuitBreakerEvents(CircuitBreaker cb) {
        cb.getEventPublisher().onStateTransition(event ->
                log.warn("[Resilience-CircuitBreaker] CircuitBreaker '{}' state transition: {} -> {}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
    }
}
