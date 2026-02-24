package org.banksolution.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.api.dto.ApproveManualReviewRequest;
import org.banksolution.api.dto.InitiatePaymentRequest;
import org.banksolution.api.dto.InitiatePaymentResponse;
import org.banksolution.api.dto.ManualReviewResponse;
import org.banksolution.api.dto.OverrideDecisionRequest;
import org.banksolution.api.dto.OverrideDecisionResponse;
import org.banksolution.api.dto.RejectManualReviewRequest;
import org.banksolution.domain.payment.command.ApproveManualReviewCommand;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.banksolution.domain.payment.command.OverrideDecisionCommand;
import org.banksolution.domain.payment.command.RejectManualReviewCommand;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.enums.PaymentStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandService {

    private final CommandGateway commandGateway;

    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request) {
        log.info("Initiating payment for customer: {}", request.getCustomerId());

        PaymentId paymentId = new PaymentId(request.getPaymentId());
        commandGateway.sendAndWait(new InitiatePaymentCommand(
                paymentId,
                request.getCustomerId(),
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentType(),
                request.isCrossBorderPayment(),
                request.getDescription()
        ));

        log.info("Payment initiated successfully: {}", paymentId);
        return new InitiatePaymentResponse(paymentId.toString(), "Payment initiated successfully");
    }

    public ManualReviewResponse approveManualReview(String paymentId, ApproveManualReviewRequest request) {
        log.info("Approving manual review for payment: {} by: {}", paymentId, request.getApprovedBy());

        commandGateway.sendAndWait(new ApproveManualReviewCommand(
                new PaymentId(request.getPaymentId()),
                request.getApprovedBy(),
                request.getApprovalNotes()
        ));

        log.info("Manual review approved for payment: {} by: {}", paymentId, request.getApprovedBy());
        return new ManualReviewResponse(
                paymentId,
                "Manual review approved successfully. Payment will proceed to account charging.",
                request.getApprovedBy()
        );
    }

    public ManualReviewResponse rejectManualReview(String paymentId, RejectManualReviewRequest request) {
        log.info("Rejecting manual review for payment: {} by: {}", paymentId, request.getRejectedBy());

        commandGateway.sendAndWait(new RejectManualReviewCommand(
                new PaymentId(request.getPaymentId()),
                request.getRejectedBy(),
                request.getRejectionReason()
        ));

        log.info("Manual review rejected for payment: {} by: {}", paymentId, request.getRejectedBy());
        return new ManualReviewResponse(
                paymentId,
                "Manual review rejected. Payment has been blocked: " + request.getRejectionReason(),
                request.getRejectedBy()
        );
    }

    public OverrideDecisionResponse overrideDecision(String paymentId, OverrideDecisionRequest request) {
        log.info("Overriding decision for payment: {} by: {}", paymentId, request.getOverriddenBy());

        commandGateway.sendAndWait(new OverrideDecisionCommand(
                new PaymentId(UUID.fromString(paymentId)),
                request.getOverriddenBy(),
                request.getOverrideReason(),
                request.isApprovePayment()
        ));

        String newStatus = request.isApprovePayment()
                ? PaymentStatus.OVERRIDE_APPROVED.name()
                : PaymentStatus.OVERRIDE_REJECTED.name();

        log.info("Decision override applied for payment: {} by: {} — newStatus: {}", paymentId, request.getOverriddenBy(), newStatus);
        return new OverrideDecisionResponse(
                paymentId,
                "Decision override applied successfully",
                request.getOverriddenBy(),
                newStatus
        );
    }
}
