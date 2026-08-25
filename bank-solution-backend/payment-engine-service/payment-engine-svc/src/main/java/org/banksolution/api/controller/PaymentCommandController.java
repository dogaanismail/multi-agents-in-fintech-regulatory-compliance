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
    public ResponseEntity<@NonNull InitiatePaymentResponse> initiatePayment(@RequestBody InitiatePaymentRequest initiatePaymentRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentCommandService.initiatePayment(initiatePaymentRequest));
    }

    @PostMapping("/{paymentId}/manual-review/approve")
    public ResponseEntity<@NonNull ManualReviewResponse> approveManualReview(
            @PathVariable String paymentId,
            @RequestBody ApproveManualReviewRequest approveManualReviewRequest) {
        return ResponseEntity.ok(paymentCommandService.approveManualReview(paymentId, approveManualReviewRequest));
    }

    @PostMapping("/{paymentId}/manual-review/reject")
    public ResponseEntity<@NonNull ManualReviewResponse> rejectManualReview(
            @PathVariable String paymentId,
            @RequestBody RejectManualReviewRequest rejectManualReviewRequest) {
        return ResponseEntity.ok(paymentCommandService.rejectManualReview(paymentId, rejectManualReviewRequest));
    }

    @PostMapping("/{paymentId}/decision/override")
    public ResponseEntity<@NonNull OverrideDecisionResponse> overrideDecision(
            @PathVariable String paymentId,
            @RequestBody OverrideDecisionRequest overrideDecisionRequest) {
        return ResponseEntity.ok(paymentCommandService.overrideDecision(paymentId, overrideDecisionRequest));
    }
}
