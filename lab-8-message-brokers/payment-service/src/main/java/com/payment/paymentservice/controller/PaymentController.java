package com.payment.paymentservice.controller;

import com.payment.common.PaymentDto;
import com.payment.common.event.SettlementRequestedEvent;
import com.payment.paymentservice.service.PaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for completing payments, issuing refunds, and requesting daily settlements.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentDto> completePayment(@RequestBody CreatePaymentRequest request) {
        String paymentId = (request.id() == null || request.id().isBlank())
                ? UUID.randomUUID().toString()
                : request.id();
        PaymentDto created = paymentService.completePayment(paymentId, request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentDto> refundPayment(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }

    @PostMapping("/settlements/close")
    public ResponseEntity<Map<String, Object>> closeSettlement(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        SettlementRequestedEvent event = paymentService.requestSettlement(targetDate);
        return ResponseEntity.accepted().body(Map.of(
                "status", "SETTLEMENT_REQUESTED",
                "eventId", event.eventId().toString(),
                "date", event.date().toString()
        ));
    }

    public record CreatePaymentRequest(String id, BigDecimal amount) {
    }
}
