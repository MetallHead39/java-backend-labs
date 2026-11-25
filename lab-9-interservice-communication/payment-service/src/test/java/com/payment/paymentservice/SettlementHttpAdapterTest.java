package com.payment.paymentservice;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import com.payment.paymentservice.adapter.http.SettlementHttpAdapter;
import com.payment.paymentservice.adapter.http.SettlementHttpClient;
import com.payment.paymentservice.exception.RemoteResourceNotFoundException;
import com.payment.paymentservice.exception.RemoteServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementHttpAdapterTest {

    @Mock
    private SettlementHttpClient httpClient;

    private SettlementHttpAdapter httpAdapter;

    @BeforeEach
    void setUp() {
        httpAdapter = new SettlementHttpAdapter(httpClient);
    }

    @Test
    void shouldSuccessfullyRetrieveSettlementDay() {
        when(httpClient.getSettlementDay("2026-11-25"))
                .thenReturn(new SettlementDayInfo("2026-11-25", "OPEN"));

        SettlementDayInfo info = httpAdapter.getDayStatus("2026-11-25");
        assertThat(info.date()).isEqualTo("2026-11-25");
        assertThat(info.status()).isEqualTo("OPEN");
    }

    @Test
    void shouldSuccessfullyRetrieveMerchantFee() {
        when(httpClient.getMerchantFee("merchant-1"))
                .thenReturn(new MerchantFeeInfo("merchant-1", 1.5));

        MerchantFeeInfo info = httpAdapter.getMerchantFee("merchant-1");
        assertThat(info.merchantId()).isEqualTo("merchant-1");
        assertThat(info.feePercent()).isEqualTo(1.5);
    }

    @Test
    void shouldSuccessfullyRetrieveDailyLimit() {
        when(httpClient.getDailyLimit("merchant-1"))
                .thenReturn(new DailyLimitInfo("merchant-1", 5000.0));

        DailyLimitInfo info = httpAdapter.getDailyLimit("merchant-1");
        assertThat(info.merchantId()).isEqualTo("merchant-1");
        assertThat(info.remainingLimit()).isEqualTo(5000.0);
    }

    @Test
    void shouldTranslateHttp404ToRemoteResourceNotFoundException() {
        when(httpClient.getSettlementDay("1999-01-01"))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatusCode.valueOf(404),
                        "Not Found",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ));

        assertThatThrownBy(() -> httpAdapter.getDayStatus("1999-01-01"))
                .isInstanceOf(RemoteResourceNotFoundException.class)
                .hasMessageContaining("Settlement day not found: 1999-01-01");
    }

    @Test
    void shouldTranslateHttp5xxToRemoteServiceUnavailableException() {
        when(httpClient.getMerchantFee("merchant-1"))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatusCode.valueOf(500),
                        "Internal Server Error",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ));

        assertThatThrownBy(() -> httpAdapter.getMerchantFee("merchant-1"))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasMessageContaining("500");
    }

    @Test
    void shouldTranslateTimeoutToRemoteServiceUnavailableException() {
        when(httpClient.getDailyLimit("merchant-1"))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        assertThatThrownBy(() -> httpAdapter.getDailyLimit("merchant-1"))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasMessageContaining("unavailable or timed out");
    }
}
