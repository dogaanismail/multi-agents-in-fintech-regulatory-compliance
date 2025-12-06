package org.banksolution.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.model.response.PaymentStatusResponse;
import org.banksolution.service.PaymentQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-engine/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentQueryController {

    private final PaymentQueryService paymentQueryService;

    @GetMapping("/{paymentId}")
    public ResponseEntity<@NonNull PaymentStatusResponse> getPaymentById(@PathVariable UUID paymentId) {
        log.info("GET /api/v1/payment-engine/payments/{}", paymentId);
        PaymentStatusResponse response = paymentQueryService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reference/{referenceNumber}")
    public ResponseEntity<@NonNull PaymentStatusResponse> getPaymentByReferenceNumber(@PathVariable String referenceNumber) {
        log.info("GET /api/v1/payment-engine/payments/reference/{}", referenceNumber);
        PaymentStatusResponse response = paymentQueryService.getPaymentByReferenceNumber(referenceNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/external/{externalPaymentId}")
    public ResponseEntity<@NonNull PaymentStatusResponse> getPaymentByExternalId(@PathVariable UUID externalPaymentId) {
        log.info("GET /api/v1/payment-engine/payments/external/{}", externalPaymentId);
        PaymentStatusResponse response = paymentQueryService.getPaymentByExternalId(externalPaymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<@NonNull List<PaymentStatusResponse>> getPaymentsByCustomerId(@PathVariable UUID customerId) {
        log.info("GET /api/v1/payment-engine/payments/customer/{}", customerId);
        List<PaymentStatusResponse> responses = paymentQueryService.getPaymentsByCustomerId(customerId);
        return ResponseEntity.ok(responses);
    }
}
