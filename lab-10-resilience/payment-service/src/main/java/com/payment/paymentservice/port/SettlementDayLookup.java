package com.payment.paymentservice.port;

import com.payment.common.dto.SettlementDayInfo;

/**
 * Port for looking up settlement day status.
 */
public interface SettlementDayLookup {
    SettlementDayInfo getDayStatus(String date);
}
