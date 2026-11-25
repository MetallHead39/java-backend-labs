package com.payment.paymentservice;

import com.payment.common.dto.SettlementDayInfo;
import com.payment.paymentservice.exception.RemoteResourceNotFoundException;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import com.payment.paymentservice.exception.ServiceCircuitOpenException;
import com.payment.paymentservice.port.SettlementDayLookup;
import com.payment.paymentservice.resilience.ResilientSettlementDayLookup;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "RESILIENCE_RETRY_MAX_ATTEMPTS=3",
        "RESILIENCE_RETRY_WAIT_MS=50",
        "RESILIENCE_CB_WINDOW_SIZE=4",
        "RESILIENCE_CB_MIN_CALLS=4",
        "RESILIENCE_CB_FAILURE_THRESHOLD=50",
        "RESILIENCE_CB_OPEN_WAIT_MS=5000",
        "RESILIENCE_CB_HALF_OPEN_CALLS=2"
})
class ResilienceRetryAndCircuitBreakerTest {

    @Autowired
    private SettlementDayLookup settlementDayLookup; // This is the @Primary ResilientSettlementDayLookup

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @MockBean
    private com.payment.paymentservice.adapter.http.SettlementHttpClient settlementHttpClient;

    @BeforeEach
    void resetResilience() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("settlementService");
        cb.reset();
        Retry retry = retryRegistry.retry("settlementService");
        // Reset retry metrics if any
    }

    @Test
    void shouldRetryOnRemoteServiceUnavailableExceptionAndEventuallySucceed() {
        when(settlementHttpClient.getSettlementDay("2026-11-25"))
                .thenThrow(new org.springframework.web.client.HttpServerErrorException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Server busy"))
                .thenThrow(new org.springframework.web.client.HttpServerErrorException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Server busy"))
                .thenReturn(new SettlementDayInfo("2026-11-25", "OPEN"));

        SettlementDayInfo result = settlementDayLookup.getDayStatus("2026-11-25");

        assertEquals("OPEN", result.status());
        verify(settlementHttpClient, times(3)).getSettlementDay("2026-11-25");
    }

    @Test
    void shouldNotRetryOnRemoteResourceNotFoundException() {
        when(settlementHttpClient.getSettlementDay("unknown-day"))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.NOT_FOUND, "Not found"));

        assertThrows(RemoteResourceNotFoundException.class, () -> settlementDayLookup.getDayStatus("unknown-day"));

        // Should not retry 404, only 1 invocation
        verify(settlementHttpClient, times(1)).getSettlementDay("unknown-day");
    }

    @Test
    void shouldOpenCircuitBreakerAfterConsecutiveFailuresAndRejectSubsequentCallsWithServiceCircuitOpenException() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("settlementService");
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());

        // Configure mock to throw 503
        when(settlementHttpClient.getSettlementDay(anyString()))
                .thenThrow(new org.springframework.web.client.HttpServerErrorException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Server down"));

        // In minimumNumberOfCalls=4, retry wraps CB, so each port call attempts 3 times, each recording a failure in CB.
        // Once 4 failures are recorded, CB transitions to OPEN.
        // Therefore, during the retries of earlier calls or on call 2, CB opens.
        // Let's make calls until ServiceCircuitOpenException is thrown (meaning CB opened).
        boolean circuitOpened = false;
        for (int i = 0; i < 5; i++) {
            final String date = "2026-11-2" + i;
            try {
                settlementDayLookup.getDayStatus(date);
            } catch (ServiceCircuitOpenException ex) {
                circuitOpened = true;
                break;
            } catch (RemoteServiceUnavailableException ex) {
                // Expected before CB opens
            }
        }

        assertTrue(circuitOpened, "Circuit breaker should have opened and thrown ServiceCircuitOpenException");
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        // Subsequent call should be rejected immediately with ServiceCircuitOpenException
        ServiceCircuitOpenException thrown = assertThrows(
                ServiceCircuitOpenException.class,
                () -> settlementDayLookup.getDayStatus("2026-11-30")
        );
        assertTrue(thrown.getMessage().contains("circuit breaker is OPEN"));
    }

    @Test
    void shouldNotCount404AsCircuitBreakerFailure() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("settlementService");
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());

        when(settlementHttpClient.getSettlementDay(anyString()))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.NOT_FOUND, "Not found"));

        for (int i = 0; i < 5; i++) {
            final String date = "2026-11-2" + i;
            assertThrows(RemoteResourceNotFoundException.class, () -> settlementDayLookup.getDayStatus(date));
        }

        // Circuit breaker should STILL be CLOSED because 404 is ignored
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }
}
