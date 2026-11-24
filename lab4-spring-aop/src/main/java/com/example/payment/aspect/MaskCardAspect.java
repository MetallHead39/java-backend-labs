package com.example.payment.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MaskCardAspect {

    @Around("@annotation(com.example.payment.annotation.MaskCardNumber)")
    public Object maskCard(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof String text) {
            return text.replaceAll("\\b(\\d{4})\\d{8}(\\d{4})\\b", "$1********$2");
        }
        return result;
    }
}