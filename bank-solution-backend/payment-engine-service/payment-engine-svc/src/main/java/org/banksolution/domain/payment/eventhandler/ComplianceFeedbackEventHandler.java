package org.banksolution.domain.payment.eventhandler;

import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.AllowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.config.ProcessingGroup;
import org.banksolution.domain.payment.PaymentEventProcessingGroups;
import org.banksolution.domain.payment.event.DecisionOverriddenEvent;
import org.banksolution.domain.payment.event.ManualReviewApprovedEvent;
import org.banksolution.domain.payment.event.ManualReviewRejectedEvent;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.service.PaymentQueryService;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.infrastructure.messaging.kafka.producer.ComplianceAgentManualFeedbackEventProducer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@ProcessingGroup(PaymentEventProcessingGroups.COMPLIANCE_FEEDBACK_PUBLISHER)
public class ComplianceFeedbackEventHandler {

    private static final String APPROVE_DECISION = "APPROVE";
    private static final String REJECT_DECISION = "REJECT";
    private static final String UNKNOWN_MARL_ACTION = "UNKNOWN";

    private final ComplianceAgentManualFeedbackEventProducer complianceAgentManualFeedbackEventProducer;
    private final PaymentQueryService paymentQueryService;

    @EventHandler
    @AllowReplay
    public void on(ManualReviewApprovedEvent manualReviewApprovedEvent, EventMessage<?> eventMessage) {
        publishManualReviewFeedback(
                manualReviewApprovedEvent.paymentId().toString(),
                manualReviewApprovedEvent.approvedBy(),
                APPROVE_DECISION,
                manualReviewApprovedEvent.approvalNotes());
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewRejectedEvent manualReviewRejectedEvent, EventMessage<?> eventMessage) {
        publishManualReviewFeedback(
                manualReviewRejectedEvent.paymentId().toString(),
                manualReviewRejectedEvent.rejectedBy(),
                REJECT_DECISION,
                manualReviewRejectedEvent.rejectionReason());
    }

    @EventHandler
    @AllowReplay
    public void on(DecisionOverriddenEvent decisionOverriddenEvent, EventMessage<?> eventMessage) {
        publishOverrideFeedback(decisionOverriddenEvent);
    }

    private void publishManualReviewFeedback(String paymentId, String reviewedBy, String officerDecision, String notes) {
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
    }

    private void publishOverrideFeedback(DecisionOverriddenEvent decisionOverriddenEvent) {
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
    }
}
