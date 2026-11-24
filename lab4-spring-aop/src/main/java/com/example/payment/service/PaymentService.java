package com.example.payment.service;

import com.example.payment.annotation.LimitTransactionAmount;
import com.example.payment.annotation.MaskCardNumber;
import com.example.payment.annotation.RetryOnTimeout;
import com.example.payment.exception.PaymentGatewayTimeoutException;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private int simulatedFailures = 0;

    public void setSimulatedFailures(int simulatedFailures) {
        this.simulatedFailures = simulatedFailures;
    }

    @LimitTransactionAmount(max = 5000.0)
    public String transferMoney(String toCard, double amount) {
        return String.format("Переказано %.2f грн на картку %s", amount, toCard);
    }

    @RetryOnTimeout(maxAttempts = 3)
    public String chargeExternalGateway(double amount) {
        if (simulatedFailures > 0) {
            simulatedFailures--;
            throw new PaymentGatewayTimeoutException("Таймаут з'єднання з банком-еквайром");
        }
        return "Оплата успішна: " + amount + " грн";
    }

    @MaskCardNumber
    public String getReceipt(String cardNumber, double amount) {
        return String.format("Квитанція: картка %s, сума %.2f грн", cardNumber, amount);
    }

    // Демонстрація проблеми self-invocation: виклик іде в обхід Spring Proxy, аспект не спрацьовує
    public String processAndGetUnmaskedReceiptViaSelfInvocation(String cardNumber, double amount) {
        return this.getReceipt(cardNumber, amount);
    }
}