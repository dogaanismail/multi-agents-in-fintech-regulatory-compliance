package org.banksolution.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.banksolution.servicesecurity.AuthenticatedCaller;
import org.banksolution.servicesecurity.Permissions;
import org.banksolution.servicesecurity.RequiresPermission;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment Commands")
@RestController
@RequestMapping("/api/v1/payment-engine/payments")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final PaymentCommandService paymentCommandService;

    @Operation(summary = "Initiate a payment")
    @PostMapping
    @RequiresPermission(Permissions.PAYMENT_ENGINE_COMMAND)
    public ResponseEntity<@NonNull InitiatePaymentResponse> initiatePayment(@RequestBody InitiatePaymentRequest initiatePaymentRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentCommandService.initiatePayment(initiatePaymentRequest));
    }

    @Operation(summary = "Approve a manual review")
    @PostMapping("/{paymentId}/manual-review/approve")
    @RequiresPermission(Permissions.PAYMENT_REVIEW)
    public ResponseEntity<@NonNull ManualReviewResponse> approveManualReview(
            @PathVariable String paymentId,
            @RequestBody ApproveManualReviewRequest approveManualReviewRequest,
            @AuthenticationPrincipal Jwt caller) {

        return ResponseEntity.ok(paymentCommandService.approveManualReview(
                paymentId,
                approveManualReviewRequest,
                AuthenticatedCaller.username(caller)));
    }

    @Operation(summary = "Reject a manual review")
    @PostMapping("/{paymentId}/manual-review/reject")
    @RequiresPermission(Permissions.PAYMENT_REVIEW)
    public ResponseEntity<@NonNull ManualReviewResponse> rejectManualReview(
            @PathVariable String paymentId,
            @RequestBody RejectManualReviewRequest rejectManualReviewRequest,
            @AuthenticationPrincipal Jwt caller) {

        return ResponseEntity.ok(paymentCommandService.rejectManualReview(
                paymentId,
                rejectManualReviewRequest,
                AuthenticatedCaller.username(caller)));
    }

    @Operation(summary = "Override a compliance decision")
    @PostMapping("/{paymentId}/decision/override")
    @RequiresPermission(Permissions.PAYMENT_OVERRIDE)
    public ResponseEntity<@NonNull OverrideDecisionResponse> overrideDecision(
            @PathVariable String paymentId,
            @RequestBody OverrideDecisionRequest overrideDecisionRequest,
            @AuthenticationPrincipal Jwt caller) {

        return ResponseEntity.ok(paymentCommandService.overrideDecision(
                paymentId,
                overrideDecisionRequest,
                AuthenticatedCaller.username(caller)));
    }
}
