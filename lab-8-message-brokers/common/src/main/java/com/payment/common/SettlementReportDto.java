package com.payment.common;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data transfer object representing a daily settlement summary report.
 */
public record SettlementReportDto(LocalDate date, int totalPayments, BigDecimal totalAmount) {
}
