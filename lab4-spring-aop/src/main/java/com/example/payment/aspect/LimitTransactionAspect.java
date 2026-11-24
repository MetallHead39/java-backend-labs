package com.example.payment.aspect;

import com.example.payment.annotation.LimitTransactionAmount;
import com.example.payment.exception.TransactionLimitExceededException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LimitTransactionAspect {

    @Before("@annotation(limitAnnotation)")
    public void checkLimit(JoinPoint joinPoint, LimitTransactionAmount limitAnnotation) {
        double maxAllowed = limitAnnotation.max();

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Double amount) {
                if (amount <= 0) {
                    throw new IllegalArgumentException("Сума транзакції має бути більшою за 0");
                }
                if (amount > maxAllowed) {
                    throw new TransactionLimitExceededException(
                            String.format("Сума %.2f перевищує дозволений ліміт %.2f", amount, maxAllowed)
                    );
                }
            }
        }
    }
}