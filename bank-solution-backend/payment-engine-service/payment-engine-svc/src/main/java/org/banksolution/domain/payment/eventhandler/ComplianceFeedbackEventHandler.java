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

    private final ComplianceAgentManualFeedbackEventProducer complianceAgentManualFeedbackEventProducer;
    private final PaymentQueryService paymentQueryService;

    @EventHandler
    @AllowReplay
    public void on(ManualReviewApprovedEvent event, EventMessage<?> eventMessage) {
        if (CurrentUnitOfWork.isStarted()) {
            CurrentUnitOfWork.get().afterCommit(uow ->
                    publishManualReviewFeedback(event.paymentId().toString(), event.approvedBy(), "APPROVE", event.approvalNotes()));
        } else {
            publishManualReviewFeedback(event.paymentId().toString(), event.approvedBy(), "APPROVE", event.approvalNotes());
        }
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewRejectedEvent event, EventMessage<?> eventMessage) {
        if (CurrentUnitOfWork.isStarted()) {
            CurrentUnitOfWork.get().afterCommit(uow ->
                    publishManualReviewFeedback(event.paymentId().toString(), event.rejectedBy(), "REJECT", event.rejectionReason()));
        } else {
            publishManualReviewFeedback(event.paymentId().toString(), event.rejectedBy(), "REJECT", event.rejectionReason());
        }
    }

    @EventHandler
    @AllowReplay
    public void on(DecisionOverriddenEvent event, EventMessage<?> eventMessage) {
        if (CurrentUnitOfWork.isStarted()) {
            CurrentUnitOfWork.get().afterCommit(uow -> publishOverrideFeedback(event));
        } else {
            publishOverrideFeedback(event);
        }
    }

    private void publishManualReviewFeedback(String paymentId, String reviewedBy, String officerDecision, String notes) {
        try {
            PaymentResponse payment = paymentQueryService.findPaymentById(new PaymentId(UUID.fromString(paymentId)));
            String originalMarlAction = payment.riskAssessment() != null
                    && payment.riskAssessment().marlAssessment() != null
                    ? payment.riskAssessment().marlAssessment().action()
                    : "UNKNOWN";
            complianceAgentManualFeedbackEventProducer.publish(
                    paymentId,
                    "MANUAL_REVIEW",
                    originalMarlAction,
                    officerDecision,
                    reviewedBy,
                    notes
            );
        } catch (Exception e) {
            log.error("Failed to publish manual review feedback for paymentId: {}", paymentId, e);
        }
    }

    private void publishOverrideFeedback(DecisionOverriddenEvent event) {
        try {
            String officerDecision = event.approvePayment() ? "APPROVE" : "REJECT";
            String originalMarlAction = switch (event.originalStatus()) {
                case "COMPLETED" -> "ALLOW";
                case "BLOCKED" -> "BLOCK";
                default -> "UNKNOWN";
            };
            complianceAgentManualFeedbackEventProducer.publish(
                    event.paymentId().toString(),
                    "DECISION_OVERRIDE",
                    originalMarlAction,
                    officerDecision,
                    event.overriddenBy(),
                    event.overrideReason()
            );
        } catch (Exception e) {
            log.error("Failed to publish override feedback for paymentId: {}", event.paymentId(), e);
        }
    }
}
