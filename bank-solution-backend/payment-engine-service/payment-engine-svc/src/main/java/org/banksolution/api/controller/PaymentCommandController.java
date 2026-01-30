package org.banksolution.api.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.api.dto.ApproveManualReviewRequest;
import org.banksolution.api.dto.InitiatePaymentRequest;
import org.banksolution.api.dto.InitiatePaymentResponse;
import org.banksolution.api.dto.ManualReviewResponse;
import org.banksolution.api.dto.RejectManualReviewRequest;
import org.banksolution.domain.payment.command.ApproveManualReviewCommand;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.banksolution.domain.payment.command.RejectManualReviewCommand;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandController {

    private final CommandGateway commandGateway;

    @PostMapping
    public ResponseEntity<@NonNull InitiatePaymentResponse> initiatePayment(@RequestBody InitiatePaymentRequest request) {
        log.info("Received payment initiation request for customer: {}", request.getCustomerId());

        PaymentId paymentId = new PaymentId(request.getPaymentId());
        InitiatePaymentCommand command = new InitiatePaymentCommand(
                paymentId,
                request.getCustomerId(),
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentType(),
                request.isCrossBorderPayment(),
                request.getDescription()
        );
        commandGateway.sendAndWait(command);
        log.info("Payment initiated successfully: {}", paymentId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new InitiatePaymentResponse(paymentId.toString(), "Payment initiated successfully"));
    }

    @PostMapping("/{paymentId}/manual-review/approve")
    public ResponseEntity<@NonNull ManualReviewResponse> approveManualReview(
            @PathVariable String paymentId,
            @RequestBody ApproveManualReviewRequest request) {
        log.info("Received manual review approval request for payment: {} by: {}", paymentId, request.getApprovedBy());

        PaymentId id = new PaymentId(request.getPaymentId());
        ApproveManualReviewCommand command = new ApproveManualReviewCommand(
                id,
                request.getApprovedBy(),
                request.getApprovalNotes()
        );

        commandGateway.sendAndWait(command);

        log.info("Manual review approved successfully for payment: {} by: {}", paymentId, request.getApprovedBy());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ManualReviewResponse(
                        paymentId,
                        "Manual review approved successfully. Payment will proceed to account charging.",
                        request.getApprovedBy()
                ));
    }

    @PostMapping("/{paymentId}/manual-review/reject")
    public ResponseEntity<@NonNull ManualReviewResponse> rejectManualReview(
            @PathVariable String paymentId,
            @RequestBody RejectManualReviewRequest request) {
        log.info("Received manual review rejection request for payment: {} by: {}", paymentId, request.getRejectedBy());

        PaymentId id = new PaymentId(request.getPaymentId());
        RejectManualReviewCommand command = new RejectManualReviewCommand(
                id,
                request.getRejectedBy(),
                request.getRejectionReason()
        );
        commandGateway.sendAndWait(command);

        log.info("Manual review rejected successfully for payment: {} by: {}", paymentId, request.getRejectedBy());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ManualReviewResponse(
                        paymentId,
                        "Manual review rejected. Payment has been blocked: " + request.getRejectionReason(),
                        request.getRejectedBy()
                ));
    }

}
