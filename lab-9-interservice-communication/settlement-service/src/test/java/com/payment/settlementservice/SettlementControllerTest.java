package com.payment.settlementservice;

import com.payment.common.dto.DailyLimitInfo;
import com.payment.common.dto.MerchantFeeInfo;
import com.payment.common.dto.SettlementDayInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SettlementControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnSettlementDayWhenOpen() {
        ResponseEntity<SettlementDayInfo> response = restTemplate.getForEntity(
                "/api/settlements/days/2026-11-25",
                SettlementDayInfo.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().date()).isEqualTo("2026-11-25");
        assertThat(response.getBody().status()).isEqualTo("OPEN");
    }

    @Test
    void shouldReturnSettlementDayWhenClosed() {
        ResponseEntity<SettlementDayInfo> response = restTemplate.getForEntity(
                "/api/settlements/days/2026-11-24",
                SettlementDayInfo.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().date()).isEqualTo("2026-11-24");
        assertThat(response.getBody().status()).isEqualTo("CLOSED");
    }

    @Test
    void shouldReturn404WhenSettlementDayNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/settlements/days/1999-01-01",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnMerchantFee() {
        ResponseEntity<MerchantFeeInfo> response = restTemplate.getForEntity(
                "/api/settlements/merchants/merchant-1/fee",
                MerchantFeeInfo.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().merchantId()).isEqualTo("merchant-1");
        assertThat(response.getBody().feePercent()).isEqualTo(1.5);
    }

    @Test
    void shouldReturn404WhenMerchantFeeNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/settlements/merchants/unknown-merchant/fee",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnDailyLimit() {
        ResponseEntity<DailyLimitInfo> response = restTemplate.getForEntity(
                "/api/settlements/merchants/merchant-2/limit",
                DailyLimitInfo.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().merchantId()).isEqualTo("merchant-2");
        assertThat(response.getBody().remainingLimit()).isEqualTo(1200.0);
    }

    @Test
    void shouldReturn404WhenDailyLimitNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/settlements/merchants/unknown-merchant/limit",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
