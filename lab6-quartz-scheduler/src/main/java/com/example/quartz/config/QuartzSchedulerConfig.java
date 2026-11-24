package com.example.quartz.config;

import com.example.quartz.job.DailySettlementReportJob;
import com.example.quartz.job.PendingPaymentCleanupJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class QuartzSchedulerConfig {

    public static final String GROUP_PAYMENTS = "payments-maintenance";
    public static final String GROUP_REPORTS = "financial-reports";

    public static final JobKey CLEANUP_JOB_KEY = JobKey.jobKey("pendingPaymentCleanupJob", GROUP_PAYMENTS);
    public static final JobKey REPORT_JOB_KEY = JobKey.jobKey("dailySettlementReportJob", GROUP_REPORTS);

    @Bean
    public JobDetail pendingPaymentCleanupJobDetail() {
        return JobBuilder.newJob(PendingPaymentCleanupJob.class)
                .withIdentity(CLEANUP_JOB_KEY)
                .withDescription("Expires stale PENDING payments")
                .usingJobData(PendingPaymentCleanupJob.KEY_TIMEOUT_MINUTES, 15)
                .usingJobData(PendingPaymentCleanupJob.KEY_MAX_RETRIES, 2)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger daytimeCleanupTrigger(JobDetail pendingPaymentCleanupJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(pendingPaymentCleanupJobDetail)
                .withIdentity("daytimeCleanupTrigger", GROUP_PAYMENTS)
                .usingJobData(PendingPaymentCleanupJob.KEY_TIMEOUT_MINUTES, 30)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/15 8-22 * * ?")
                        .inTimeZone(TimeZone.getTimeZone("Europe/Kyiv"))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public Trigger nighttimeCleanupTrigger(JobDetail pendingPaymentCleanupJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(pendingPaymentCleanupJobDetail)
                .withIdentity("nighttimeCleanupTrigger", GROUP_PAYMENTS)
                .usingJobData(PendingPaymentCleanupJob.KEY_TIMEOUT_MINUTES, 5)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(10)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    @Bean
    public JobDetail dailySettlementReportJobDetail() {
        return JobBuilder.newJob(DailySettlementReportJob.class)
                .withIdentity(REPORT_JOB_KEY)
                .withDescription("Generates daily settlement report")
                .usingJobData(DailySettlementReportJob.KEY_CURRENCY, "UAH")
                .usingJobData(DailySettlementReportJob.KEY_TIMEZONE, "Europe/Kyiv")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger dailySettlementReportTrigger(JobDetail dailySettlementReportJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(dailySettlementReportJobDetail)
                .withIdentity("dailySettlementReportTrigger", GROUP_REPORTS)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 55 23 * * ?")
                        .inTimeZone(TimeZone.getTimeZone("Europe/Kyiv"))
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }
}
