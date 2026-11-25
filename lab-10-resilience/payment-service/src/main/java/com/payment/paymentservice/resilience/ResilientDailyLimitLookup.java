package com.payment.paymentservice.resilience;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.paymentservice.exception.RemoteResourceNotFoundException;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import com.payment.paymentservice.exception.ServiceCircuitOpenException;
import com.payment.paymentservice.port.DailyLimitLookup;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class ResilientDailyLimitLookup implements DailyLimitLookup {

    private final DailyLimitLookup delegate;

    public ResilientDailyLimitLookup(List<DailyLimitLookup> delegates) {
        this.delegate = delegates.stream()
                .filter(d -> !(d instanceof ResilientDailyLimitLookup))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No non-resilient DailyLimitLookup delegate bean found"));
    }

    @Override
    @Retry(name = "settlementService")
    @CircuitBreaker(name = "settlementService", fallbackMethod = "fallbackGetDailyLimit")
    public DailyLimitInfo getDailyLimit(String merchantId) {
        return delegate.getDailyLimit(merchantId);
    }

    public DailyLimitInfo fallbackGetDailyLimit(String merchantId, CallNotPermittedException ex) {
        throw new ServiceCircuitOpenException("Settlement service circuit breaker is OPEN; calls are temporarily rejected", ex);
    }

    public DailyLimitInfo fallbackGetDailyLimit(String merchantId, RemoteResourceNotFoundException ex) {
        throw ex;
    }

    public DailyLimitInfo fallbackGetDailyLimit(String merchantId, RemoteServiceUnavailableException ex) {
        throw ex;
    }

    public DailyLimitInfo fallbackGetDailyLimit(String merchantId, Throwable ex) throws Throwable {
        if (ex instanceof CallNotPermittedException cne) {
            throw new ServiceCircuitOpenException("Settlement service circuit breaker is OPEN; calls are temporarily rejected", cne);
        }
        throw ex;
    }
}
