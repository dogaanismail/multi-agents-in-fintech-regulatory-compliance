package org.banksolution.domain.payment.eventhandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.AllowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.unitofwork.CurrentUnitOfWork;
import org.banksolution.domain.payment.event.DecisionOverriddenEvent;
import org.banksolution.domain.payment.event.ManualReviewApprovedEvent;
import org.banksolution.domain.payment.event.ManualReviewRejectedEvent;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.service.PaymentQueryService;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.infrastructure.messaging.kafka.producer.ComplianceAgentManualFeedbackEventProducer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceFeedbackEventHandler {

    private static final String APPROVE_DECISION = "APPROVE";
    private static final String REJECT_DECISION = "REJECT";
    private static final String UNKNOWN_MARL_ACTION = "UNKNOWN";

    private final ComplianceAgentManualFeedbackEventProducer complianceAgentManualFeedbackEventProducer;
    private final PaymentQueryService paymentQueryService;

    @EventHandler
    @AllowReplay
    public void on(ManualReviewApprovedEvent manualReviewApprovedEvent, EventMessage<?> eventMessage) {
        runAfterCommit(() -> publishManualReviewFeedback(
                manualReviewApprovedEvent.paymentId().toString(),
                manualReviewApprovedEvent.approvedBy(),
                APPROVE_DECISION,
                manualReviewApprovedEvent.approvalNotes()));
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewRejectedEvent manualReviewRejectedEvent, EventMessage<?> eventMessage) {
        runAfterCommit(() -> publishManualReviewFeedback(
                manualReviewRejectedEvent.paymentId().toString(),
                manualReviewRejectedEvent.rejectedBy(),
                REJECT_DECISION,
                manualReviewRejectedEvent.rejectionReason()));
    }

    @EventHandler
    @AllowReplay
    public void on(DecisionOverriddenEvent decisionOverriddenEvent, EventMessage<?> eventMessage) {
        runAfterCommit(() -> publishOverrideFeedback(decisionOverriddenEvent));
    }

    private static void runAfterCommit(Runnable publication) {
        if (CurrentUnitOfWork.isStarted()) {
            CurrentUnitOfWork.get().afterCommit(_ -> publication.run());
        } else {
            publication.run();
        }
    }

    private void publishManualReviewFeedback(String paymentId, String reviewedBy, String officerDecision, String notes) {
        try {
            PaymentResponse paymentResponse = paymentQueryService.findPaymentById(new PaymentId(UUID.fromString(paymentId)));
            String originalMarlAction = paymentResponse.riskAssessment() != null
                    && paymentResponse.riskAssessment().marlAssessment() != null
                    ? paymentResponse.riskAssessment().marlAssessment().action()
                    : UNKNOWN_MARL_ACTION;
            complianceAgentManualFeedbackEventProducer.publish(
                    paymentId,
                    "MANUAL_REVIEW",
                    originalMarlAction,
                    officerDecision,
                    reviewedBy,
                    notes
            );
        } catch (Exception exception) {
            log.error("Failed to publish manual review feedback for paymentId: {}", paymentId, exception);
        }
    }

    private void publishOverrideFeedback(DecisionOverriddenEvent decisionOverriddenEvent) {
        try {
            String officerDecision = decisionOverriddenEvent.approvePayment() ? APPROVE_DECISION : REJECT_DECISION;
            String originalMarlAction = switch (decisionOverriddenEvent.originalStatus()) {
                case "COMPLETED" -> "ALLOW";
                case "BLOCKED" -> "BLOCK";
                default -> UNKNOWN_MARL_ACTION;
            };
            complianceAgentManualFeedbackEventProducer.publish(
                    decisionOverriddenEvent.paymentId().toString(),
                    "DECISION_OVERRIDE",
                    originalMarlAction,
                    officerDecision,
                    decisionOverriddenEvent.overriddenBy(),
                    decisionOverriddenEvent.overrideReason()
            );
        } catch (Exception exception) {
            log.error("Failed to publish override feedback for paymentId: {}", decisionOverriddenEvent.paymentId(), exception);
        }
    }
}
