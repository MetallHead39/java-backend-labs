package com.example.payment.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaskCardAspectTest {

    private final MaskCardAspect aspect = new MaskCardAspect();

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Test
    void shouldMask16DigitCardNumberInReturnedString() throws Throwable {
        when(joinPoint.proceed()).thenReturn("Оплата з картки 5375414100001234 виконана");

        Object result = aspect.maskCard(joinPoint);

        assertEquals("Оплата з картки 5375********1234 виконана", result);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void shouldLeaveNonStringResultUnchanged() throws Throwable {
        when(joinPoint.proceed()).thenReturn(42);

        Object result = aspect.maskCard(joinPoint);

        assertEquals(42, result);
        verify(joinPoint, times(1)).proceed();
    }
}