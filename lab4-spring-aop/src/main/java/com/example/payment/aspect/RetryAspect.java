package com.example.payment.aspect;

import com.example.payment.annotation.RetryOnTimeout;
import com.example.payment.exception.PaymentGatewayTimeoutException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RetryAspect {

    private static final Logger log = LoggerFactory.getLogger(RetryAspect.class);

    @Around("@annotation(retryAnnotation)")
    public Object retryOperation(ProceedingJoinPoint joinPoint, RetryOnTimeout retryAnnotation) throws Throwable {
        int maxAttempts = retryAnnotation.maxAttempts();
        int attempt = 0;

        while (true) {
            attempt++;
            try {
                return joinPoint.proceed();
            } catch (PaymentGatewayTimeoutException ex) {
                log.warn("Спроба {}/{} не вдалася для методу {}: {}",
                        attempt, maxAttempts, joinPoint.getSignature().getName(), ex.getMessage());
                if (attempt >= maxAttempts) {
                    throw ex;
                }
            }
        }
    }
}