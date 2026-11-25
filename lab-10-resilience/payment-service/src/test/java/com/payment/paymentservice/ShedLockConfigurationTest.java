package com.payment.paymentservice;

import com.payment.paymentservice.scheduler.PaymentReconciliationScheduler;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ShedLockConfigurationTest {

    @Autowired
    private LockProvider lockProvider;

    @Autowired
    private PaymentReconciliationScheduler reconciliationScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldHaveLockProviderConfigured() {
        assertNotNull(lockProvider, "LockProvider bean should be present");
        assertNotNull(reconciliationScheduler, "PaymentReconciliationScheduler bean should be present");
    }

    @Test
    void shouldHaveShedLockTableCreated() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shedlock", Integer.class
        );
        assertNotNull(count);
        assertTrue(count >= 0);
    }

    @Test
    void shouldExecuteScheduledJobWithoutException() {
        assertDoesNotThrow(() -> reconciliationScheduler.reconcilePayments());
    }
}
