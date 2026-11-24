package com.example.quartz.job;

import com.example.quartz.service.PaymentMaintenanceService;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class DailySettlementReportJob implements Job {

    public static final String KEY_CURRENCY = "currency";
    public static final String KEY_TIMEZONE = "timezone";

    private static final Logger log = LoggerFactory.getLogger(DailySettlementReportJob.class);
    private final PaymentMaintenanceService maintenanceService;

    public DailySettlementReportJob(PaymentMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String currency = dataMap.getString(KEY_CURRENCY);
        if (currency == null || currency.isBlank()) {
            throw new JobExecutionException("Missing required parameter: " + KEY_CURRENCY, false);
        }

        String tz = dataMap.containsKey(KEY_TIMEZONE) ? dataMap.getString(KEY_TIMEZONE) : "UTC";
        LocalDate reportDate = LocalDate.now(ZoneId.of(tz));

        try {
            maintenanceService.generateDailySettlementReport(reportDate, currency);
        } catch (Exception ex) {
            log.error("Daily settlement report failed for currency={}; will wait for next schedule", currency, ex);
            throw new JobExecutionException(ex, false);
        }
    }
}
