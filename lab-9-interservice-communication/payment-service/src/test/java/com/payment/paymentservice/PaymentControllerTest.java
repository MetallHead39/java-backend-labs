package com.payment.paymentservice;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import com.payment.paymentservice.controller.PaymentController;
import com.payment.paymentservice.dto.PaymentCalculationRequest;
import com.payment.paymentservice.exception.RemoteResourceNotFoundException;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import com.payment.paymentservice.port.DailyLimitLookup;
import com.payment.paymentservice.port.MerchantFeeLookup;
import com.payment.paymentservice.port.SettlementDayLookup;
import com.payment.paymentservice.service.PaymentQuoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(PaymentQuoteService.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SettlementDayLookup settlementDayLookup;

    @MockBean
    private MerchantFeeLookup merchantFeeLookup;

    @MockBean
    private DailyLimitLookup dailyLimitLookup;

    @Test
    void shouldReturnCheckDaySuccess() throws Exception {
        when(settlementDayLookup.getDayStatus("2026-11-25"))
                .thenReturn(new SettlementDayInfo("2026-11-25", "OPEN"));

        mockMvc.perform(get("/api/payments/check-day/2026-11-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-25"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldReturn404WhenSettlementDayNotFound() throws Exception {
        when(settlementDayLookup.getDayStatus("unknown-day"))
                .thenThrow(new RemoteResourceNotFoundException("Settlement day not found: unknown-day"));

        mockMvc.perform(get("/api/payments/check-day/unknown-day"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Settlement day not found: unknown-day"));
    }

    @Test
    void shouldReturn503WhenSettlementServiceUnavailable() throws Exception {
        when(merchantFeeLookup.getMerchantFee("merchant-1"))
                .thenThrow(new RemoteServiceUnavailableException("Remote service timeout"));

        mockMvc.perform(get("/api/payments/merchants/merchant-1/fee"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Remote service timeout"));
    }

    @Test
    void shouldCalculatePaymentSuccessfully() throws Exception {
        when(settlementDayLookup.getDayStatus("2026-11-25"))
                .thenReturn(new SettlementDayInfo("2026-11-25", "OPEN"));
        when(merchantFeeLookup.getMerchantFee("merchant-1"))
                .thenReturn(new MerchantFeeInfo("merchant-1", 1.5));
        when(dailyLimitLookup.getDailyLimit("merchant-1"))
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
                .andExpect(jsonPath("$.merchantId").value("merchant-1"))
                .andExpect(jsonPath("$.date").value("2026-11-25"))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.feePercent").value(1.5))
                .andExpect(jsonPath("$.feeAmount").value(1.50))
                .andExpect(jsonPath("$.netAmount").value(98.50))
                .andExpect(jsonPath("$.remainingLimitBefore").value(5000.0))
                .andExpect(jsonPath("$.remainingLimitAfter").value(4900.0))
                .andExpect(jsonPath("$.settlementDayStatus").value("OPEN"));
    }

    @Test
    void shouldReturnConflictWhenDayIsClosed() throws Exception {
        when(settlementDayLookup.getDayStatus("2026-11-24"))
                .thenReturn(new SettlementDayInfo("2026-11-24", "CLOSED"));

        PaymentCalculationRequest request = new PaymentCalculationRequest(
                "merchant-1",
                "2026-11-24",
                BigDecimal.valueOf(100.0)
        );

        mockMvc.perform(post("/api/payments/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Settlement day is not open: CLOSED"));
    }

    @Test
    void shouldReturnConflictWhenLimitExceeded() throws Exception {
        when(settlementDayLookup.getDayStatus("2026-11-25"))
                .thenReturn(new SettlementDayInfo("2026-11-25", "OPEN"));
        when(dailyLimitLookup.getDailyLimit("merchant-2"))
                .thenReturn(new DailyLimitInfo("merchant-2", 100.0));

        PaymentCalculationRequest request = new PaymentCalculationRequest(
                "merchant-2",
                "2026-11-25",
                BigDecimal.valueOf(200.0)
        );

        mockMvc.perform(post("/api/payments/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Payment amount 200.0 exceeds remaining daily limit 100.0"));
    }
}
