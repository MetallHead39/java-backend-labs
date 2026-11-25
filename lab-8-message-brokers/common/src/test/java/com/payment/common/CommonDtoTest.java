package com.payment.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CommonDtoTest {

    @Test
    void paymentDtoStoresExpectedFields() {
        PaymentDto dto = new PaymentDto("pay-1", new BigDecimal("99.50"), PaymentStatus.COMPLETED);

        assertThat(dto.id()).isEqualTo("pay-1");
        assertThat(dto.amount()).isEqualByComparingTo("99.50");
        assertThat(dto.status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void settlementReportDtoStoresExpectedFields() {
        LocalDate date = LocalDate.of(2026, 11, 25);
        SettlementReportDto report = new SettlementReportDto(date, 3, new BigDecimal("250.00"));

        assertThat(report.date()).isEqualTo(date);
        assertThat(report.totalPayments()).isEqualTo(3);
        assertThat(report.totalAmount()).isEqualByComparingTo("250.00");
    }
}
