package org.banksolution.domain.payment.eventhandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.AllowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.config.ProcessingGroup;
import org.banksolution.domain.payment.PaymentEventProcessingGroups;
import org.banksolution.domain.payment.event.*;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.enums.PaymentEventTrigger;
import org.banksolution.infrastructure.messaging.kafka.producer.PaymentCompletedEventProducer;
import org.banksolution.infrastructure.messaging.kafka.producer.PaymentSnapshotEventProducer;
import org.springframework.stereotype.Component;

import static org.banksolution.enums.PaymentEventTrigger.*;

@Slf4j
@Component
@RequiredArgsConstructor
@ProcessingGroup(PaymentEventProcessingGroups.PAYMENT_SNAPSHOT_PUBLISHER)
public class PaymentEventHandler {

    private final PaymentSnapshotEventProducer paymentSnapshotEventProducer;
    private final PaymentCompletedEventProducer paymentCompletedEventProducer;

    @EventHandler
    @AllowReplay
    public void on(PaymentInitiatedEvent paymentInitiatedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(paymentInitiatedEvent.paymentId(), PAYMENT_INITIATED);
    }

    @EventHandler
    @AllowReplay
    public void on(RiskAssessmentInitiatedEvent riskAssessmentInitiatedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(riskAssessmentInitiatedEvent.paymentId(), RISK_ASSESSMENT_INITIATED);
    }

    @EventHandler
    @AllowReplay
    public void on(RiskAssessmentCompletedEvent riskAssessmentCompletedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(riskAssessmentCompletedEvent.paymentId(), RISK_ASSESSMENT_COMPLETED);
    }

    @EventHandler
    @AllowReplay
    public void on(RiskAssessmentTimedOutEvent riskAssessmentTimedOutEvent, EventMessage<?> eventMessage) {
        publishSnapshot(riskAssessmentTimedOutEvent.paymentId(), RISK_ASSESSMENT_TIMED_OUT);
    }

    @EventHandler
    @AllowReplay
    public void on(FraudCheckApprovedEvent fraudCheckApprovedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(fraudCheckApprovedEvent.paymentId(), FRAUD_CHECK_APPROVED);
    }

    @EventHandler
    @AllowReplay
    public void on(PaymentBlockedEvent paymentBlockedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(paymentBlockedEvent.paymentId(), PAYMENT_BLOCKED);
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewRequestedEvent manualReviewRequestedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(manualReviewRequestedEvent.paymentId(), MANUAL_REVIEW_REQUESTED);
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewApprovedEvent manualReviewApprovedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(manualReviewApprovedEvent.paymentId(), MANUAL_REVIEW_APPROVED);
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewRejectedEvent manualReviewRejectedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(manualReviewRejectedEvent.paymentId(), MANUAL_REVIEW_REJECTED);
    }

    @EventHandler
    @AllowReplay
    public void on(DecisionOverriddenEvent decisionOverriddenEvent, EventMessage<?> eventMessage) {
        PaymentEventTrigger paymentEventTrigger = decisionOverriddenEvent.approvePayment()
                ? DECISION_OVERRIDE_APPROVED
                : DECISION_OVERRIDE_REJECTED;
        publishSnapshot(decisionOverriddenEvent.paymentId(), paymentEventTrigger);
    }

    @EventHandler
    @AllowReplay
    public void on(LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(ledgerAuthorisationInitiatedEvent.paymentId(), LEDGER_AUTHORISATION_INITIATED);
    }

    @EventHandler
    @AllowReplay
    public void on(LedgerAuthorisedEvent ledgerAuthorisedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(ledgerAuthorisedEvent.paymentId(), LEDGER_AUTHORISED);
    }

    @EventHandler
    @AllowReplay
    public void on(LedgerAuthorisationDeclinedEvent ledgerAuthorisationDeclinedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(ledgerAuthorisationDeclinedEvent.paymentId(), LEDGER_AUTHORISATION_DECLINED);
    }

    @EventHandler
    @AllowReplay
    public void on(LedgerSettlementInitiatedEvent ledgerSettlementInitiatedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(ledgerSettlementInitiatedEvent.paymentId(), LEDGER_SETTLEMENT_INITIATED);
    }

    @EventHandler
    @AllowReplay
    public void on(LedgerSettledEvent ledgerSettledEvent, EventMessage<?> eventMessage) {
        publishSnapshot(ledgerSettledEvent.paymentId(), LEDGER_SETTLED);
    }

    @EventHandler
    @AllowReplay
    public void on(LedgerSettlementFailedEvent ledgerSettlementFailedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(ledgerSettlementFailedEvent.paymentId(), LEDGER_SETTLEMENT_FAILED);
    }

    @EventHandler
    @AllowReplay
    public void on(LedgerReleaseInitiatedEvent ledgerReleaseInitiatedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(ledgerReleaseInitiatedEvent.paymentId(), LEDGER_RELEASE_INITIATED);
    }

    @EventHandler
    @AllowReplay
    public void on(LedgerReleasedEvent ledgerReleasedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(ledgerReleasedEvent.paymentId(), LEDGER_RELEASED);
    }

    @EventHandler
    @AllowReplay
    public void on(LedgerReleaseFailedEvent ledgerReleaseFailedEvent, EventMessage<?> eventMessage) {
        publishSnapshot(ledgerReleaseFailedEvent.paymentId(), LEDGER_RELEASE_FAILED);
    }

    @EventHandler
    @AllowReplay
    public void on(PaymentCompletedEvent paymentCompletedEvent, EventMessage<?> eventMessage) {
        log.info("Payment has been completed for paymentId: {}", paymentCompletedEvent.paymentId());

        paymentSnapshotEventProducer.publish(paymentCompletedEvent.paymentId(), PAYMENT_COMPLETED);
        paymentCompletedEventProducer.publish(paymentCompletedEvent.paymentId());
    }

    private void publishSnapshot(PaymentId paymentId, PaymentEventTrigger paymentEventTrigger) {
        paymentSnapshotEventProducer.publish(paymentId, paymentEventTrigger);
    }
}
