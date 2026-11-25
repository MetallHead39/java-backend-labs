package com.payment.paymentservice.scheduler;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationScheduler.class);

    private final String instanceName;

    public PaymentReconciliationScheduler(
            @Value("${instance.name:${INSTANCE_NAME:${HOSTNAME:${spring.application.name:payment-service}}}}") String instanceName) {
        this.instanceName = instanceName;
    }

    @Scheduled(fixedDelayString = "${scheduler.reconciliation.delay-ms:5000}")
    @SchedulerLock(
            name = "paymentReconciliationTask",
            lockAtMostFor = "${scheduler.reconciliation.lock-at-most:PT10S}",
            lockAtLeastFor = "${scheduler.reconciliation.lock-at-least:PT4S}"
    )
    public void reconcilePayments() {
        log.info("[ShedLock-Reconciliation] Lock acquired and reconciliation job executed by instance: {}", instanceName);
    }
}
