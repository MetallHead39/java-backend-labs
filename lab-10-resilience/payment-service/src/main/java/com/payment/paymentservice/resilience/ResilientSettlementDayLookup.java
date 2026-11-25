package com.payment.paymentservice.resilience;

import com.payment.common.dto.SettlementDayInfo;
import com.payment.paymentservice.exception.RemoteResourceNotFoundException;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import com.payment.paymentservice.exception.ServiceCircuitOpenException;
import com.payment.paymentservice.port.SettlementDayLookup;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class ResilientSettlementDayLookup implements SettlementDayLookup {

    private final SettlementDayLookup delegate;

    public ResilientSettlementDayLookup(List<SettlementDayLookup> delegates) {
        this.delegate = delegates.stream()
                .filter(d -> !(d instanceof ResilientSettlementDayLookup))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No non-resilient SettlementDayLookup delegate bean found"));
    }

    @Override
    @Retry(name = "settlementService")
    @CircuitBreaker(name = "settlementService", fallbackMethod = "fallbackGetDayStatus")
    public SettlementDayInfo getDayStatus(String date) {
        return delegate.getDayStatus(date);
    }

    public SettlementDayInfo fallbackGetDayStatus(String date, CallNotPermittedException ex) {
        throw new ServiceCircuitOpenException("Settlement service circuit breaker is OPEN; calls are temporarily rejected", ex);
    }

    public SettlementDayInfo fallbackGetDayStatus(String date, RemoteResourceNotFoundException ex) {
        throw ex;
    }

    public SettlementDayInfo fallbackGetDayStatus(String date, RemoteServiceUnavailableException ex) {
        throw ex;
    }

    public SettlementDayInfo fallbackGetDayStatus(String date, Throwable ex) throws Throwable {
        if (ex instanceof CallNotPermittedException cne) {
            throw new ServiceCircuitOpenException("Settlement service circuit breaker is OPEN; calls are temporarily rejected", cne);
        }
        throw ex;
    }
}
