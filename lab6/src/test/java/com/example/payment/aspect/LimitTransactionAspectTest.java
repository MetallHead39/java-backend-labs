package com.example.payment.aspect;

import com.example.payment.annotation.LimitTransactionAmount;
import com.example.payment.exception.TransactionLimitExceededException;
import com.example.payment.service.PaymentService;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitTransactionAspectTest {

    private final LimitTransactionAspect aspect = new LimitTransactionAspect();

    @Mock
    private JoinPoint joinPoint;

    private LimitTransactionAmount limitAnnotation;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        Method method = PaymentService.class.getMethod("transferMoney", String.class, double.class);
        limitAnnotation = method.getAnnotation(LimitTransactionAmount.class);
    }

    @Test
    void shouldAllowTransactionWithinLimit() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"4111222233334444", 4999.99});

        assertDoesNotThrow(() -> aspect.checkLimit(joinPoint, limitAnnotation));
    }

    @Test
    void shouldThrowExceptionWhenAmountExceedsLimit() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"4111222233334444", 5000.01});

        assertThrows(TransactionLimitExceededException.class,
                () -> aspect.checkLimit(joinPoint, limitAnnotation));
    }
}