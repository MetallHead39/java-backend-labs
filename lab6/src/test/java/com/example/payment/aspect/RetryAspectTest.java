package com.example.payment.aspect;

import com.example.payment.annotation.RetryOnTimeout;
import com.example.payment.exception.PaymentGatewayTimeoutException;
import com.example.payment.service.PaymentService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryAspectTest {

    private final RetryAspect aspect = new RetryAspect();

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private RetryOnTimeout retryAnnotation;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        Method method = PaymentService.class.getMethod("chargeExternalGateway", double.class);
        retryAnnotation = method.getAnnotation(RetryOnTimeout.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("chargeExternalGateway");
    }

    @Test
    void shouldRetryAndSucceedOnThirdAttempt() throws Throwable {
        when(joinPoint.proceed())
                .thenThrow(new PaymentGatewayTimeoutException("Timeout 1"))
                .thenThrow(new PaymentGatewayTimeoutException("Timeout 2"))
                .thenReturn("OK");

        Object result = aspect.retryOperation(joinPoint, retryAnnotation);

        assertEquals("OK", result);
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void shouldThrowExceptionWhenAllAttemptsExhausted() throws Throwable {
        when(joinPoint.proceed())
                .thenThrow(new PaymentGatewayTimeoutException("Timeout 1"))
                .thenThrow(new PaymentGatewayTimeoutException("Timeout 2"))
                .thenThrow(new PaymentGatewayTimeoutException("Timeout 3"));

        assertThrows(PaymentGatewayTimeoutException.class,
                () -> aspect.retryOperation(joinPoint, retryAnnotation));

        verify(joinPoint, times(3)).proceed();
    }
}