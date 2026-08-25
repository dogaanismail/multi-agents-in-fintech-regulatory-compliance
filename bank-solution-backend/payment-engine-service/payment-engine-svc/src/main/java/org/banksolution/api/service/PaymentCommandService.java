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
import org.banksolution.enums.FixedSide;
import org.banksolution.enums.PaymentScheme;
import org.banksolution.enums.PaymentStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandService {

    private final CommandGateway commandGateway;

    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest initiatePaymentRequest) {
        log.info("Initiating payment for customer: {}", initiatePaymentRequest.getCustomerId());

        PaymentId paymentId = initiatePaymentRequest.getPaymentId() != null
                ? new PaymentId(initiatePaymentRequest.getPaymentId())
                : new PaymentId();
        String toCurrency = initiatePaymentRequest.getToCurrency() != null
                ? initiatePaymentRequest.getToCurrency()
                : initiatePaymentRequest.getFromCurrency();

        // No FX is applied on this path, so the converted amount equals the amount; a null
        // here would break every snapshot published for the payment.
        commandGateway.sendAndWait(new InitiatePaymentCommand(
                paymentId,
                initiatePaymentRequest.getCustomerId(),
                initiatePaymentRequest.getSourceAccountId(),
                initiatePaymentRequest.getDestinationAccountId(),
                initiatePaymentRequest.getAmount(),
                initiatePaymentRequest.getFromCurrency(),
                toCurrency,
                initiatePaymentRequest.getAmount(),
                null,
                initiatePaymentRequest.getPaymentType(),
                PaymentScheme.EXTERNAL_OUTBOUND.name(),
                FixedSide.SELL.name(),
                initiatePaymentRequest.isCrossBorderPayment(),
                initiatePaymentRequest.getDescription()
        ));

        log.info("Payment initiated successfully: {}", paymentId);
        return new InitiatePaymentResponse(paymentId.toString(), "Payment initiated successfully");
    }

    public ManualReviewResponse approveManualReview(String paymentId, ApproveManualReviewRequest approveManualReviewRequest) {
        log.info("Approving manual review for payment: {} by: {}", paymentId, approveManualReviewRequest.getApprovedBy());

        commandGateway.sendAndWait(new ApproveManualReviewCommand(
                new PaymentId(UUID.fromString(paymentId)),
                approveManualReviewRequest.getApprovedBy(),
                approveManualReviewRequest.getApprovalNotes()
        ));

        log.info("Manual review approved for payment: {} by: {}", paymentId, approveManualReviewRequest.getApprovedBy());
        return new ManualReviewResponse(
                paymentId,
                "Manual review approved successfully. Payment will proceed to account charging.",
                approveManualReviewRequest.getApprovedBy()
        );
    }

    public ManualReviewResponse rejectManualReview(String paymentId, RejectManualReviewRequest rejectManualReviewRequest) {
        log.info("Rejecting manual review for payment: {} by: {}", paymentId, rejectManualReviewRequest.getRejectedBy());

        commandGateway.sendAndWait(new RejectManualReviewCommand(
                new PaymentId(UUID.fromString(paymentId)),
                rejectManualReviewRequest.getRejectedBy(),
                rejectManualReviewRequest.getRejectionReason()
        ));

        log.info("Manual review rejected for payment: {} by: {}", paymentId, rejectManualReviewRequest.getRejectedBy());
        return new ManualReviewResponse(
                paymentId,
                "Manual review rejected. Payment has been blocked: " + rejectManualReviewRequest.getRejectionReason(),
                rejectManualReviewRequest.getRejectedBy()
        );
    }

    public OverrideDecisionResponse overrideDecision(String paymentId, OverrideDecisionRequest overrideDecisionRequest) {
        log.info("Overriding decision for payment: {} by: {}", paymentId, overrideDecisionRequest.getOverriddenBy());

        commandGateway.sendAndWait(new OverrideDecisionCommand(
                new PaymentId(UUID.fromString(paymentId)),
                overrideDecisionRequest.getOverriddenBy(),
                overrideDecisionRequest.getOverrideReason(),
                overrideDecisionRequest.isApprovePayment()
        ));

        String newStatus = overrideDecisionRequest.isApprovePayment()
                ? PaymentStatus.OVERRIDE_APPROVED.name()
                : PaymentStatus.OVERRIDE_REJECTED.name();

        log.info("Decision override applied for payment: {} by: {} — newStatus: {}",
                paymentId, overrideDecisionRequest.getOverriddenBy(), newStatus);
        return new OverrideDecisionResponse(
                paymentId,
                "Decision override applied successfully",
                overrideDecisionRequest.getOverriddenBy(),
                newStatus
        );
    }
}
