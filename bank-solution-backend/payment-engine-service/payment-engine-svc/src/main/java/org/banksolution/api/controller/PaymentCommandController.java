package org.banksolution.api.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.banksolution.api.dto.ApproveManualReviewRequest;
import org.banksolution.api.dto.InitiatePaymentRequest;
import org.banksolution.api.dto.InitiatePaymentResponse;
import org.banksolution.api.dto.ManualReviewResponse;
import org.banksolution.api.dto.OverrideDecisionRequest;
import org.banksolution.api.dto.OverrideDecisionResponse;
import org.banksolution.api.dto.RejectManualReviewRequest;
import org.banksolution.api.service.PaymentCommandService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment-engine/payments")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final PaymentCommandService paymentCommandService;

    @PostMapping
    public ResponseEntity<@NonNull InitiatePaymentResponse> initiatePayment(@RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentCommandService.initiatePayment(request));
    }

    @PostMapping("/{paymentId}/manual-review/approve")
    public ResponseEntity<@NonNull ManualReviewResponse> approveManualReview(
            @PathVariable String paymentId,
            @RequestBody ApproveManualReviewRequest request) {
        return ResponseEntity.ok(paymentCommandService.approveManualReview(paymentId, request));
    }

    @PostMapping("/{paymentId}/manual-review/reject")
    public ResponseEntity<@NonNull ManualReviewResponse> rejectManualReview(
            @PathVariable String paymentId,
            @RequestBody RejectManualReviewRequest request) {
        return ResponseEntity.ok(paymentCommandService.rejectManualReview(paymentId, request));
    }

    @PostMapping("/{paymentId}/decision/override")
    public ResponseEntity<@NonNull OverrideDecisionResponse> overrideDecision(
            @PathVariable String paymentId,
            @RequestBody OverrideDecisionRequest request) {
        return ResponseEntity.ok(paymentCommandService.overrideDecision(paymentId, request));
    }
}
