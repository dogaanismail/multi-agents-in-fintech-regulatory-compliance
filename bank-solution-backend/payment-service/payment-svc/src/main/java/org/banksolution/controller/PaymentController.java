package org.banksolution.controller;

import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;
import org.banksolution.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/request")
    public ResponseEntity<@NonNull PaymentRequestResponse> requestPayment(
            @Valid @RequestBody PaymentRequest request) {

        log.info("POST /api/v1/payments/request - customer: {}, type: {}, amount: {} {}",
                request.getCustomerId(),
                request.getPaymentType(),
                request.getAmount(),
                request.getCurrency());

        PaymentRequestResponse response = paymentService.requestPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<@NonNull List<PaymentRequestResponse>> getPaymentsByCustomerId(
            @PathVariable UUID customerId) {
        log.info("GET /api/v1/payments/customer/{}", customerId);

        List<PaymentRequestResponse> responses = paymentService.getPaymentsByCustomerId(customerId);
        return ResponseEntity.ok(responses);
    }
}

