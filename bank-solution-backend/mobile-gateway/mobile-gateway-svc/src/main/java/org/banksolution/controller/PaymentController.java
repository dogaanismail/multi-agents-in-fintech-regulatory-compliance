package org.banksolution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.banksolution.http.IdempotencyHeaders;
import org.banksolution.model.request.MobilePaymentRequest;
import org.banksolution.model.response.MobilePaymentResponse;
import org.banksolution.service.PaymentService;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/payments")
@Tag(name = "Payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Pay from one of my accounts", description = "Requires an Idempotency-Key; a retry with the same key returns the same payment")
    public ResponseEntity<@NonNull MobilePaymentResponse> submitPayment(
            @AuthenticationPrincipal Jwt caller,
            @RequestHeader(IdempotencyHeaders.IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody MobilePaymentRequest mobilePaymentRequest) {

        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.submitPayment(caller, idempotencyKey, mobilePaymentRequest));
    }

    @GetMapping
    @Operation(summary = "List my payments")
    public ResponseEntity<@NonNull List<MobilePaymentResponse>> getPayments(
            @AuthenticationPrincipal Jwt caller,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(paymentService.getPayments(caller, page, size));
    }
}
