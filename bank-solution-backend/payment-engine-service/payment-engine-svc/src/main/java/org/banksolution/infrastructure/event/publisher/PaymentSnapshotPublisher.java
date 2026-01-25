package org.banksolution.infrastructure.event.publisher;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.AllowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.unitofwork.CurrentUnitOfWork;
import org.axonframework.queryhandling.QueryGateway;
import org.banksolution.domain.payment.event.*;
import org.banksolution.domain.payment.query.FindPaymentQuery;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.infrastructure.event.mapper.PaymentAggregateSnapshotMapper;
import org.banksolution.infrastructure.messaging.kafka.producer.PaymentSnapshotEventProducer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSnapshotPublisher {

    private final PaymentSnapshotEventProducer snapshotProducer;
    private final QueryGateway queryGateway;

    @EventHandler
    @AllowReplay
    public void on(PaymentInitiatedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "PAYMENT_INITIATED");
    }

    @EventHandler
    @AllowReplay
    public void on(RiskAssessmentInitiatedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "RISK_ASSESSMENT_INITIATED");
    }

    @EventHandler
    @AllowReplay
    public void on(RiskAssessmentCompletedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "RISK_ASSESSMENT_COMPLETED");
    }

    @EventHandler
    @AllowReplay
    public void on(FraudCheckApprovedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "FRAUD_CHECK_APPROVED");
    }

    @EventHandler
    @AllowReplay
    public void on(PaymentBlockedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "PAYMENT_BLOCKED");
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewRequestedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "MANUAL_REVIEW_REQUESTED");
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewApprovedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "MANUAL_REVIEW_APPROVED");
    }

    @EventHandler
    @AllowReplay
    public void on(ManualReviewRejectedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "MANUAL_REVIEW_REJECTED");
    }

    @EventHandler
    @AllowReplay
    public void on(AccountChargeInitiatedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "ACCOUNT_CHARGE_INITIATED");
    }

    @EventHandler
    @AllowReplay
    public void on(AccountChargedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "ACCOUNT_CHARGED");
    }

    @EventHandler
    @AllowReplay
    public void on(AccountChargeFailedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "ACCOUNT_CHARGE_FAILED");
    }

    @EventHandler
    @AllowReplay
    public void on(PaymentCompletedEvent event, EventMessage<?> eventMessage) {
        publishSnapshotAfterCommit(event.paymentId(), "PAYMENT_COMPLETED");
    }

    private void publishSnapshotAfterCommit(PaymentId paymentId, String eventTrigger) {
        if (CurrentUnitOfWork.isStarted()) {
            CurrentUnitOfWork.get().afterCommit(uow -> publishSnapshot(paymentId, eventTrigger));
        } else {
            // Fallback: publish immediately if no unit of work (shouldn't happen in normal flow)
            publishSnapshot(paymentId, eventTrigger);
        }
    }

    private void publishSnapshot(PaymentId paymentId, String eventTrigger) {
        try {
            log.debug("Publishing payment snapshot for paymentId: {}, trigger: {}", paymentId, eventTrigger);

            PaymentResponse paymentResponse = queryGateway
                    .query(new FindPaymentQuery(paymentId.toString()), PaymentResponse.class).join();

            PaymentSnapshotEvent snapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(paymentResponse, eventTrigger);
            snapshotProducer.publishPaymentSnapshot(snapshotEvent);

            log.info("Payment snapshot published for paymentId: {}, status: {}, trigger: {}",
                    paymentId,
                    paymentResponse.status(),
                    eventTrigger);
        } catch (Exception e) {
            log.error("Failed to publish payment snapshot for paymentId: {}, trigger: {}",
                    paymentId,
                    eventTrigger,
                    e);
        }
    }
}
