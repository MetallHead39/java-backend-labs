package com.example.payment.service;

import com.example.payment.exception.TransactionLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SelfInvocationDemoTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    void externalCallShouldTriggerAspectAndMaskCard() {
        String receipt = paymentService.getReceipt("4111222233334444", 250.0);
        assertTrue(receipt.contains("4111********4444"),
                "Зовнішній виклик через Spring Proxy повинен замаскувати картку");
    }

    @Test
    void selfInvocationCallShouldBypassProxyAndNotMaskCard() {
        String receipt = paymentService.processAndGetUnmaskedReceiptViaSelfInvocation("4111222233334444", 250.0);
        assertTrue(receipt.contains("4111222233334444"),
                "Внутрішній виклик this.getReceipt() обходить проксі, тому картка лишається відкритою");
    }

    @Test
    void limitAspectShouldBlockExcessiveTransfer() {
        assertThrows(TransactionLimitExceededException.class,
                () -> paymentService.transferMoney("4111222233334444", 15000.0));
    }
}