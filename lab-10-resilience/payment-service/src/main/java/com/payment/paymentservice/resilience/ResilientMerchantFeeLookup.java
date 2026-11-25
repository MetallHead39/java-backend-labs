package com.payment.paymentservice.resilience;

import com.payment.common.dto.MerchantFeeInfo;
import com.payment.paymentservice.exception.RemoteResourceNotFoundException;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import com.payment.paymentservice.exception.ServiceCircuitOpenException;
import com.payment.paymentservice.port.MerchantFeeLookup;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class ResilientMerchantFeeLookup implements MerchantFeeLookup {

    private final MerchantFeeLookup delegate;

    public ResilientMerchantFeeLookup(List<MerchantFeeLookup> delegates) {
        this.delegate = delegates.stream()
                .filter(d -> !(d instanceof ResilientMerchantFeeLookup))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No non-resilient MerchantFeeLookup delegate bean found"));
    }

    @Override
    @Retry(name = "settlementService")
    @CircuitBreaker(name = "settlementService", fallbackMethod = "fallbackGetMerchantFee")
    public MerchantFeeInfo getMerchantFee(String merchantId) {
        return delegate.getMerchantFee(merchantId);
    }

    public MerchantFeeInfo fallbackGetMerchantFee(String merchantId, CallNotPermittedException ex) {
        throw new ServiceCircuitOpenException("Settlement service circuit breaker is OPEN; calls are temporarily rejected", ex);
    }

    public MerchantFeeInfo fallbackGetMerchantFee(String merchantId, RemoteResourceNotFoundException ex) {
        throw ex;
    }

    public MerchantFeeInfo fallbackGetMerchantFee(String merchantId, RemoteServiceUnavailableException ex) {
        throw ex;
    }

    public MerchantFeeInfo fallbackGetMerchantFee(String merchantId, Throwable ex) throws Throwable {
        if (ex instanceof CallNotPermittedException cne) {
            throw new ServiceCircuitOpenException("Settlement service circuit breaker is OPEN; calls are temporarily rejected", cne);
        }
        throw ex;
    }
}
