package org.banksolution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.PaymentRequestResult;
import org.banksolution.http.IdempotencyHeaders;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;
import org.banksolution.service.IdempotentPaymentService;
import org.banksolution.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Payments")
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotentPaymentService idempotentPaymentService;

    @Operation(summary = "Request a payment")
    @PostMapping("/request")
    public ResponseEntity<@NonNull PaymentRequestResponse> requestPayment(
            @Parameter(description = "Client-generated key; a retry with the same key returns the original payment")
            @RequestHeader(IdempotencyHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody PaymentRequest paymentRequest) {

        log.info("POST /api/v1/payments/request - customer: {}, type: {}, amount: {} {}",
                paymentRequest.getCustomerId(),
                paymentRequest.getPaymentType(),
                paymentRequest.getAmount(),
                paymentRequest.getFromCurrency());

        PaymentRequestResult paymentRequestResult = idempotentPaymentService.requestPayment(idempotencyKey, paymentRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(IdempotencyHeaders.IDEMPOTENT_REPLAYED, String.valueOf(paymentRequestResult.replayed()))
                .body(paymentRequestResult.paymentRequestResponse());
    }

    @Operation(summary = "List payments by customer")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<@NonNull List<PaymentRequestResponse>> getPaymentsByCustomerId(
            @PathVariable UUID customerId) {
        log.info("GET /api/v1/payments/customer/{}", customerId);

        List<PaymentRequestResponse> paymentRequestResponses = paymentService.getPaymentsByCustomerId(customerId);
        return ResponseEntity.ok(paymentRequestResponses);
    }
}

