package com.example.payment;

import com.example.payment.service.PaymentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(PaymentService paymentService) {
        return args -> {
            System.out.println("=== 1. Зовнішній виклик @MaskCardNumber (аспект ПРАЦЮЄ) ===");
            System.out.println(paymentService.getReceipt("4111222233334444", 1500.0));

            System.out.println("\n=== 2. Внутрішній self-invocation виклик (аспект НЕ ПРАЦЮЄ) ===");
            System.out.println(paymentService.processAndGetUnmaskedReceiptViaSelfInvocation("4111222233334444", 1500.0));

            System.out.println("\n=== 3. Робота @RetryOnTimeout (2 невдачі, 3-тя успішна) ===");
            paymentService.setSimulatedFailures(2);
            System.out.println(paymentService.chargeExternalGateway(999.0));
        };
    }
}