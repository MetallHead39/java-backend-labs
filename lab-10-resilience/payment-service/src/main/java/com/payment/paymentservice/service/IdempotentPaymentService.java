package com.payment.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.paymentservice.dto.PaymentCalculationRequest;
import com.payment.paymentservice.dto.PaymentCalculationResponse;
import com.payment.paymentservice.entity.IdempotencyRecordEntity;
import com.payment.paymentservice.entity.PaymentRecordEntity;
import com.payment.paymentservice.repository.IdempotencyRecordRepository;
import com.payment.paymentservice.repository.PaymentRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class IdempotentPaymentService {

    private final PaymentQuoteService paymentQuoteService;
    private final PaymentRecordRepository paymentRecordRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public IdempotentPaymentService(PaymentQuoteService paymentQuoteService,
                                    PaymentRecordRepository paymentRecordRepository,
                                    IdempotencyRecordRepository idempotencyRecordRepository,
                                    ObjectMapper objectMapper) {
        this.paymentQuoteService = paymentQuoteService;
        this.paymentRecordRepository = paymentRecordRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ResponseEntity<?> calculateWithIdempotency(PaymentCalculationRequest request,
                                                      String idempotencyKey,
                                                      String userId) {
        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId.trim() : "default-user";

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String trimmedKey = idempotencyKey.trim();
            Optional<IdempotencyRecordEntity> cachedRecord =
                    idempotencyRecordRepository.findByUserIdAndIdempotencyKey(effectiveUserId, trimmedKey);
            if (cachedRecord.isPresent()) {
                IdempotencyRecordEntity record = cachedRecord.get();
                try {
                    PaymentCalculationResponse cachedResponse =
                            objectMapper.readValue(record.getResponseBody(), PaymentCalculationResponse.class);
                    return ResponseEntity.status(record.getResponseStatus()).body(cachedResponse);
                } catch (JsonProcessingException e) {
                    return ResponseEntity.status(record.getResponseStatus()).body(record.getResponseBody());
                }
            }
        }

        // Execute payment calculation (calls settlement service ports)
        PaymentCalculationResponse response = paymentQuoteService.calculate(request);

        // Persist payment record
        PaymentRecordEntity paymentRecord = new PaymentRecordEntity(
                effectiveUserId,
                response.merchantId(),
                response.date(),
                response.amount(),
                response.feePercent(),
                response.feeAmount(),
                response.netAmount(),
                "COMPLETED",
                Instant.now()
        );
        paymentRecordRepository.save(paymentRecord);

        // If Idempotency-Key present, persist idempotency record
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String trimmedKey = idempotencyKey.trim();
            try {
                String jsonBody = objectMapper.writeValueAsString(response);
                IdempotencyRecordEntity idempotencyRecord = new IdempotencyRecordEntity(
                        effectiveUserId,
                        trimmedKey,
                        200,
                        jsonBody,
                        Instant.now()
                );
                idempotencyRecordRepository.save(idempotencyRecord);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize payment response", e);
            }
        }

        return ResponseEntity.ok(response);
    }
}
