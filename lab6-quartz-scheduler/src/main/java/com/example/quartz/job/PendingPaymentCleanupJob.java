package com.example.quartz.job;

import com.example.quartz.service.PaymentMaintenanceService;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class PendingPaymentCleanupJob implements Job {

    public static final String KEY_TIMEOUT_MINUTES = "timeoutMinutes";
    public static final String KEY_MAX_RETRIES = "maxRetries";
    public static final String KEY_RETRY_COUNT = "retryCount";

    private static final Logger log = LoggerFactory.getLogger(PendingPaymentCleanupJob.class);
    private final PaymentMaintenanceService maintenanceService;

    public PendingPaymentCleanupJob(PaymentMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();

        if (!dataMap.containsKey(KEY_TIMEOUT_MINUTES)) {
            throw new JobExecutionException("Missing required parameter: " + KEY_TIMEOUT_MINUTES, false);
        }

        int timeoutMinutes;
        try {
            Object raw = dataMap.get(KEY_TIMEOUT_MINUTES);
            timeoutMinutes = (raw instanceof Number n) ? n.intValue() : Integer.parseInt(raw.toString());
            if (timeoutMinutes <= 0) {
                throw new IllegalArgumentException("timeoutMinutes must be > 0");
            }
        } catch (Exception ex) {
            throw new JobExecutionException("Invalid parameter value for " + KEY_TIMEOUT_MINUTES, ex, false);
        }

        int maxRetries = dataMap.containsKey(KEY_MAX_RETRIES) ? dataMap.getInt(KEY_MAX_RETRIES) : 0;
        int currentRetry = context.getJobDetail().getJobDataMap().containsKey(KEY_RETRY_COUNT)
                ? context.getJobDetail().getJobDataMap().getInt(KEY_RETRY_COUNT)
                : 0;

        try {
            maintenanceService.expireStalePendingPayments(timeoutMinutes);
            context.getJobDetail().getJobDataMap().put(KEY_RETRY_COUNT, 0);
        } catch (Exception ex) {
            if (currentRetry < maxRetries) {
                int nextAttempt = currentRetry + 1;
                context.getJobDetail().getJobDataMap().put(KEY_RETRY_COUNT, nextAttempt);
                log.warn("Cleanup job failed (attempt {}/{}), scheduling immediate retry", nextAttempt, maxRetries, ex);
                throw new JobExecutionException(ex, true);
            }
            log.error("Cleanup job failed after {} retries; waiting for next scheduled trigger", maxRetries, ex);
            context.getJobDetail().getJobDataMap().put(KEY_RETRY_COUNT, 0);
            throw new JobExecutionException(ex, false);
        }
    }
}
