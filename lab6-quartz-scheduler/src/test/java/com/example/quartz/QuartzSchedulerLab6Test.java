package com.example.quartz;

import com.example.quartz.config.QuartzSchedulerConfig;
import com.example.quartz.domain.*;
import java.lang.reflect.Proxy;
import com.example.quartz.job.DailySettlementReportJob;
import com.example.quartz.job.PendingPaymentCleanupJob;
import com.example.quartz.service.PaymentMaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SpringBootTest
class QuartzSchedulerLab6Test {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SettlementReportRepository reportRepository;

    @Autowired
    private PaymentMaintenanceService maintenanceService;

    @BeforeEach
    void cleanDb() {
        reportRepository.deleteAll();
        paymentRepository.deleteAll();
    }



    @Test
    @DisplayName("1. Розклад та таблиці QRTZ_* створені Liquibase (initialize-schema: never)")
    void shouldCreateQuartzTablesViaLiquibase() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) LIKE 'qrtz_%'",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(11);
    }

    @Test
    @DisplayName("2. Завдання та тригери оголошені в @Configuration з чіткими JobKey, name та group")
    void shouldRegisterJobsAndTriggersWithProperKeysAndGroups() throws SchedulerException {
        JobDetail cleanupJob = scheduler.getJobDetail(QuartzSchedulerConfig.CLEANUP_JOB_KEY);
        JobDetail reportJob = scheduler.getJobDetail(QuartzSchedulerConfig.REPORT_JOB_KEY);

        assertThat(cleanupJob).isNotNull();
        assertThat(cleanupJob.getKey().getName()).isEqualTo("pendingPaymentCleanupJob");
        assertThat(cleanupJob.getKey().getGroup()).isEqualTo(QuartzSchedulerConfig.GROUP_PAYMENTS);

        assertThat(reportJob).isNotNull();
        assertThat(reportJob.getKey().getName()).isEqualTo("dailySettlementReportJob");
        assertThat(reportJob.getKey().getGroup()).isEqualTo(QuartzSchedulerConfig.GROUP_REPORTS);

        List<? extends Trigger> cleanupTriggers = scheduler.getTriggersOfJob(QuartzSchedulerConfig.CLEANUP_JOB_KEY);
        assertThat(cleanupTriggers).hasSize(2);
    }

    @Test
    @DisplayName("3. Параметри тригера мають пріоритет над значеннями JobDetail за замовчуванням")
    void shouldMergeJobDataMapWithTriggerPriority() throws SchedulerException {
        JobDetail jobDetail = scheduler.getJobDetail(QuartzSchedulerConfig.CLEANUP_JOB_KEY);
        Trigger dayTrigger = scheduler.getTrigger(TriggerKey.triggerKey("daytimeCleanupTrigger", QuartzSchedulerConfig.GROUP_PAYMENTS));
        Trigger nightTrigger = scheduler.getTrigger(TriggerKey.triggerKey("nighttimeCleanupTrigger", QuartzSchedulerConfig.GROUP_PAYMENTS));

        JobDataMap dayMerged = new JobDataMap();
        dayMerged.putAll(jobDetail.getJobDataMap());
        dayMerged.putAll(dayTrigger.getJobDataMap());

        JobDataMap nightMerged = new JobDataMap();
        nightMerged.putAll(jobDetail.getJobDataMap());
        nightMerged.putAll(nightTrigger.getJobDataMap());

        assertThat(dayMerged.getInt(PendingPaymentCleanupJob.KEY_TIMEOUT_MINUTES)).isEqualTo(30);
        assertThat(nightMerged.getInt(PendingPaymentCleanupJob.KEY_TIMEOUT_MINUTES)).isEqualTo(5);
        assertThat(dayMerged.getInt(PendingPaymentCleanupJob.KEY_MAX_RETRIES)).isEqualTo(2);
    }

    private JobExecutionContext createContext(JobDetail detail, JobDataMap mergedMap) {
        return (JobExecutionContext) Proxy.newProxyInstance(
                JobExecutionContext.class.getClassLoader(),
                new Class<?>[]{JobExecutionContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMergedJobDataMap" -> mergedMap;
                    case "getJobDetail" -> detail;
                    default -> null;
                }
        );
    }

    @Test
    @DisplayName("4. У разі відсутності або помилки ключа в JobDataMap виникає явний виняток")
    void shouldThrowExceptionWhenJobDataKeyMissingOrInvalid() {
        AtomicInteger calls = new AtomicInteger(0);
        PaymentMaintenanceService stubService = new PaymentMaintenanceService(paymentRepository, reportRepository) {
            @Override
            public int expireStalePendingPayments(int timeoutMinutes) {
                calls.incrementAndGet();
                return 0;
            }
        };

        PendingPaymentCleanupJob job = new PendingPaymentCleanupJob(stubService);
        JobDetail emptyDetail = JobBuilder.newJob(PendingPaymentCleanupJob.class)
                .withIdentity("invalidJob", "test")
                .build();

        JobExecutionContext context = createContext(emptyDetail, new JobDataMap());

        assertThatThrownBy(() -> job.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("Missing required parameter");
        assertThat(calls.get()).isZero();
    }

    @Test
    @DisplayName("5. @DisallowConcurrentExecution встановлено на завдання очистки")
    void shouldHaveDisallowConcurrentExecutionAnnotation() throws SchedulerException {
        JobDetail jobDetail = scheduler.getJobDetail(QuartzSchedulerConfig.CLEANUP_JOB_KEY);
        assertThat(jobDetail.isConcurrentExectionDisallowed()).isTrue();
    }

    @Test
    @DisplayName("6 & 7. Кожен тригер має explicit misfire instruction та CronTrigger має TimeZone")
    void shouldHaveExplicitMisfireInstructionsAndTimeZone() throws SchedulerException {
        CronTrigger dayTrigger = (CronTrigger) scheduler.getTrigger(
                TriggerKey.triggerKey("daytimeCleanupTrigger", QuartzSchedulerConfig.GROUP_PAYMENTS));
        SimpleTrigger nightTrigger = (SimpleTrigger) scheduler.getTrigger(
                TriggerKey.triggerKey("nighttimeCleanupTrigger", QuartzSchedulerConfig.GROUP_PAYMENTS));
        CronTrigger reportTrigger = (CronTrigger) scheduler.getTrigger(
                TriggerKey.triggerKey("dailySettlementReportTrigger", QuartzSchedulerConfig.GROUP_REPORTS));

        assertThat(dayTrigger.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        assertThat(nightTrigger.getMisfireInstruction()).isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
        assertThat(reportTrigger.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);

        assertThat(dayTrigger.getTimeZone()).isEqualTo(TimeZone.getTimeZone("Europe/Kyiv"));
        assertThat(reportTrigger.getTimeZone().getID()).isEqualTo("Europe/Kyiv");
    }

    @Test
    @DisplayName("8. Повторний запуск завдання є ідемпотентним (другий запуск нічого не змінює)")
    void shouldBeIdempotentOnRepeatedExecution() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        paymentRepository.save(new Payment("TX-OLD", new BigDecimal("100.00"), "UAH", PaymentStatus.PENDING, now.minusMinutes(40)));
        paymentRepository.save(new Payment("TX-FRESH", new BigDecimal("200.00"), "UAH", PaymentStatus.PENDING, now.minusMinutes(2)));

        int firstRunExpired = maintenanceService.expireStalePendingPayments(15);
        int secondRunExpired = maintenanceService.expireStalePendingPayments(15);

        assertThat(firstRunExpired).isEqualTo(1);
        assertThat(secondRunExpired).isZero();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        paymentRepository.save(new Payment("TX-DONE", new BigDecimal("500.00"), "UAH", PaymentStatus.COMPLETED, now));

        SettlementReport firstReport = maintenanceService.generateDailySettlementReport(today, "UAH");
        SettlementReport secondReport = maintenanceService.generateDailySettlementReport(today, "UAH");

        assertThat(secondReport.getId()).isEqualTo(firstReport.getId());
        assertThat(reportRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("9. Невдале виконання повторюється до maxRetries і зупиняється без нескінченного циклу")
    void shouldRetryUpToMaxRetriesAndStop() {
        AtomicInteger calls = new AtomicInteger(0);
        PaymentMaintenanceService failingService = new PaymentMaintenanceService(paymentRepository, reportRepository) {
            @Override
            public int expireStalePendingPayments(int timeoutMinutes) {
                calls.incrementAndGet();
                throw new RuntimeException("Temporary DB timeout");
            }
        };

        PendingPaymentCleanupJob job = new PendingPaymentCleanupJob(failingService);
        JobDetail detail = JobBuilder.newJob(PendingPaymentCleanupJob.class)
                .withIdentity("retryTestJob", "test")
                .usingJobData(PendingPaymentCleanupJob.KEY_TIMEOUT_MINUTES, 10)
                .usingJobData(PendingPaymentCleanupJob.KEY_MAX_RETRIES, 2)
                .build();

        AtomicInteger refireCount = new AtomicInteger(0);
        for (int i = 0; i < 5; i++) {
            JobDataMap merged = new JobDataMap(detail.getJobDataMap());
            JobExecutionContext ctx = createContext(detail, merged);

            try {
                job.execute(ctx);
            } catch (JobExecutionException e) {
                if (e.refireImmediately()) {
                    refireCount.incrementAndGet();
                } else {
                    break;
                }
            }
        }

        assertThat(refireCount.get()).isEqualTo(2);
        assertThat(calls.get()).isEqualTo(3);
    }
}
