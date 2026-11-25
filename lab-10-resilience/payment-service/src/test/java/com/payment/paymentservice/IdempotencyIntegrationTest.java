package com.payment.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import com.payment.paymentservice.dto.PaymentCalculationRequest;
import com.payment.paymentservice.entity.IdempotencyRecordEntity;
import com.payment.paymentservice.entity.PaymentRecordEntity;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import com.payment.paymentservice.exception.ServiceCircuitOpenException;
import com.payment.paymentservice.port.DailyLimitLookup;
import com.payment.paymentservice.port.MerchantFeeLookup;
import com.payment.paymentservice.port.SettlementDayLookup;
import com.payment.paymentservice.repository.IdempotencyRecordRepository;
import com.payment.paymentservice.repository.PaymentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private com.payment.paymentservice.adapter.http.SettlementHttpClient settlementHttpClient;

    @BeforeEach
    void cleanUp() {
        circuitBreakerRegistry.circuitBreaker("settlementService").reset();
        paymentRecordRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
    }

    @Test
    void shouldProcessNormallyWithoutIdempotencyKey() throws Exception {
        when(settlementHttpClient.getSettlementDay("2026-11-25"))
                .thenReturn(new SettlementDayInfo("2026-11-25", "OPEN"));
        when(settlementHttpClient.getMerchantFee("merchant-1"))
                .thenReturn(new MerchantFeeInfo("merchant-1", 1.5));
        when(settlementHttpClient.getDailyLimit("merchant-1"))
                .thenReturn(new DailyLimitInfo("merchant-1", 5000.0));

        PaymentCalculationRequest request = new PaymentCalculationRequest(
                "merchant-1",
                "2026-11-25",
                BigDecimal.valueOf(100.0)
        );

        mockMvc.perform(post("/api/payments/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netAmount").value(98.50));

        // Payment record saved, idempotency record NOT saved
        assertEquals(1, paymentRecordRepository.count());
        assertEquals(0, idempotencyRecordRepository.count());
    }

    @Test
    void shouldReturnCachedResponseAndNotDuplicatePaymentWhenSameKeyProvided() throws Exception {
        when(settlementHttpClient.getSettlementDay("2026-11-25"))
                .thenReturn(new SettlementDayInfo("2026-11-25", "OPEN"));
        when(settlementHttpClient.getMerchantFee("merchant-1"))
                .thenReturn(new MerchantFeeInfo("merchant-1", 1.5));
        when(settlementHttpClient.getDailyLimit("merchant-1"))
                .thenReturn(new DailyLimitInfo("merchant-1", 5000.0));

        PaymentCalculationRequest request = new PaymentCalculationRequest(
                "merchant-1",
                "2026-11-25",
                BigDecimal.valueOf(100.0)
        );

        // First call
        mockMvc.perform(post("/api/payments/calculate")
                        .header("Idempotency-Key", "key-12345")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netAmount").value(98.50));

        assertEquals(1, paymentRecordRepository.count());
        assertEquals(1, idempotencyRecordRepository.count());

        // Second call with same user and key
        mockMvc.perform(post("/api/payments/calculate")
                        .header("Idempotency-Key", "key-12345")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netAmount").value(98.50));

        // DB records should NOT be duplicated
        assertEquals(1, paymentRecordRepository.count());
        assertEquals(1, idempotencyRecordRepository.count());

        // Settlement service should only have been called ONCE
        verify(settlementHttpClient, times(1)).getSettlementDay("2026-11-25");
        verify(settlementHttpClient, times(1)).getMerchantFee("merchant-1");
        verify(settlementHttpClient, times(1)).getDailyLimit("merchant-1");
    }

    @Test
    void shouldTreatDifferentUsersWithSameKeyAsSeparateRequests() throws Exception {
        when(settlementHttpClient.getSettlementDay("2026-11-25"))
                .thenReturn(new SettlementDayInfo("2026-11-25", "OPEN"));
        when(settlementHttpClient.getMerchantFee("merchant-1"))
                .thenReturn(new MerchantFeeInfo("merchant-1", 1.5));
        when(settlementHttpClient.getDailyLimit("merchant-1"))
                .thenReturn(new DailyLimitInfo("merchant-1", 5000.0));

        PaymentCalculationRequest request = new PaymentCalculationRequest(
                "merchant-1",
                "2026-11-25",
                BigDecimal.valueOf(100.0)
        );

        // User A
        mockMvc.perform(post("/api/payments/calculate")
                        .header("Idempotency-Key", "shared-key")
                        .header("X-User-Id", "user-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // User B with SAME key
        mockMvc.perform(post("/api/payments/calculate")
                        .header("Idempotency-Key", "shared-key")
                        .header("X-User-Id", "user-B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Both records saved
        assertEquals(2, paymentRecordRepository.count());
        assertEquals(2, idempotencyRecordRepository.count());
    }

    @Test
    void shouldNotSaveIdempotencyRecordWhenRequestFailsWith5xx() throws Exception {
        when(settlementHttpClient.getSettlementDay("2026-11-25"))
                .thenThrow(new org.springframework.web.client.HttpServerErrorException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Down"));

        PaymentCalculationRequest request = new PaymentCalculationRequest(
                "merchant-1",
                "2026-11-25",
                BigDecimal.valueOf(100.0)
        );

        mockMvc.perform(post("/api/payments/calculate")
                        .header("Idempotency-Key", "fail-key")
                        .header("X-User-Id", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());

        // No records should be persisted on 5xx failure
        assertEquals(0, paymentRecordRepository.count());
        assertEquals(0, idempotencyRecordRepository.count());
    }
}
