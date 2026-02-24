package org.banksolution.domain.payment.eventhandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.AllowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.unitofwork.CurrentUnitOfWork;
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
public class PaymentEventHandler {

    private final PaymentSnapshotEventProducer paymentSnapshotEventProducer;
    private final PaymentCompletedEventProducer paymentCompletedEventProducer;

    @EventHandler
    @AllowReplay
    public void on(PaymentInitiatedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), PAYMENT_INITIATED);
    }

    @EventHandler
    @AllowReplay
    public void on(RiskAssessmentInitiatedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), RISK_ASSESSMENT_INITIATED);
    }

    @EventHandler
    @AllowReplay
    public void on(RiskAssessmentCompletedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), RISK_ASSESSMENT_COMPLETED);
    }

    @EventHandler
    @AllowReplay
    public void on(FraudCheckApprovedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), FRAUD_CHECK_APPROVED);
    }

    @EventHandler
    @AllowReplay
    public void on(PaymentBlockedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), PAYMENT_BLOCKED);
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewRequestedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), MANUAL_REVIEW_REQUESTED);
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewApprovedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), MANUAL_REVIEW_APPROVED);
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewRejectedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), MANUAL_REVIEW_REJECTED);
    }

    @EventHandler
    @AllowReplay
    public void on(DecisionOverriddenEvent event, EventMessage<?> eventMessage) {
        PaymentEventTrigger trigger = event.approvePayment()
                ? DECISION_OVERRIDE_APPROVED
                : DECISION_OVERRIDE_REJECTED;
        publishSnapshotAfterCommit(event.paymentId(), trigger);
    }

    @EventHandler
    @AllowReplay
    public void on(AccountChargeInitiatedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), ACCOUNT_CHARGE_INITIATED);
    }

    @EventHandler
    @AllowReplay
    public void on(AccountChargedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), ACCOUNT_CHARGED);
    }

    @EventHandler
    @AllowReplay
    public void on(AccountChargeFailedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), ACCOUNT_CHARGE_FAILED);
    }

    @EventHandler
    @AllowReplay
    public void on(PaymentCompletedEvent event, EventMessage<?> eventMessage) {
        log.info("Payment has been completed for paymentId: {}", event.paymentId());

        if (CurrentUnitOfWork.isStarted()) {
            CurrentUnitOfWork.get().afterCommit(uow -> {
                paymentSnapshotEventProducer.publish(event.paymentId(), PaymentEventTrigger.PAYMENT_COMPLETED);
                paymentCompletedEventProducer.publish(event.paymentId());
            });
        } else {
            paymentSnapshotEventProducer.publish(event.paymentId(), PaymentEventTrigger.PAYMENT_COMPLETED);
            paymentCompletedEventProducer.publish(event.paymentId());
        }
    }

    private void publishSnapshotAfterCommit(PaymentId paymentId, PaymentEventTrigger eventTrigger) {
        if (CurrentUnitOfWork.isStarted()) {
            CurrentUnitOfWork.get().afterCommit(uow -> paymentSnapshotEventProducer.publish(paymentId, eventTrigger));
        } else {
            paymentSnapshotEventProducer.publish(paymentId, eventTrigger);
        }
    }
}
