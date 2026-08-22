package org.banksolution.domain.payment.saga;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.banksolution.domain.payment.event.*;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.infrastructure.messaging.kafka.producer.LedgerPostingRequestedEventProducer;

import java.time.Duration;

@Saga(sagaStore = "sagaStore")
@Slf4j
public class LedgerPostingSaga {

    private static final String PAYMENT_ID_ASSOCIATION = "paymentId";
    private static final String LEDGER_POSTING_TIMEOUT_DEADLINE = "ledger-posting-timeout";
    private static final Duration LEDGER_POSTING_TIMEOUT = Duration.ofMinutes(2);

    private PaymentId paymentId;
    private String deadlineId;

    @StartSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerAuthorisationInitiatedEvent event,
                   DeadlineManager deadlineManager,
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer) {
        log.info("Requesting ledger authorisation for payment: {}", event.paymentId());

        this.paymentId = event.paymentId();

        scheduleTimeout(deadlineManager);
        ledgerPostingRequestedEventProducer.publishAuthorisation(event);
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerAuthorisedEvent event, DeadlineManager deadlineManager) {
        log.info("Ledger authorised payment: {}, awaiting the compliance decision", event.paymentId());
        cancelTimeout(deadlineManager);
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerSettlementInitiatedEvent event,
                   DeadlineManager deadlineManager,
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer) {
        log.info("Requesting ledger settlement for payment: {}", event.paymentId());

        scheduleTimeout(deadlineManager);
        ledgerPostingRequestedEventProducer.publishSettlement(event.paymentId());
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerReleaseInitiatedEvent event,
                   DeadlineManager deadlineManager,
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer) {
        log.info("Requesting ledger release for payment: {}", event.paymentId());

        scheduleTimeout(deadlineManager);
        ledgerPostingRequestedEventProducer.publishRelease(event.paymentId());
    }

    @DeadlineHandler(deadlineName = LEDGER_POSTING_TIMEOUT_DEADLINE)
    public void onLedgerPostingTimeout() {
        log.error("Ledger posting timed out for payment: {}, ending saga", this.paymentId);
        SagaLifecycle.end();
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerAuthorisationDeclinedEvent event, DeadlineManager deadlineManager) {
        log.warn("Ledger declined authorisation for payment: {}, ending saga", event.paymentId());
        cancelTimeout(deadlineManager);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerSettledEvent event, DeadlineManager deadlineManager) {
        log.info("Ledger settled payment: {}, ending saga", event.paymentId());
        cancelTimeout(deadlineManager);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerSettlementFailedEvent event, DeadlineManager deadlineManager) {
        log.error("Ledger settlement failed for payment: {}, ending saga", event.paymentId());
        cancelTimeout(deadlineManager);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerReleasedEvent event, DeadlineManager deadlineManager) {
        log.info("Ledger released the authorisation for payment: {}, ending saga", event.paymentId());
        cancelTimeout(deadlineManager);
    }

    private void scheduleTimeout(DeadlineManager deadlineManager) {
        this.deadlineId = deadlineManager.schedule(
                LEDGER_POSTING_TIMEOUT,
                LEDGER_POSTING_TIMEOUT_DEADLINE,
                this.paymentId
        );
    }

    private void cancelTimeout(DeadlineManager deadlineManager) {
        if (this.deadlineId != null) {
            deadlineManager.cancelSchedule(LEDGER_POSTING_TIMEOUT_DEADLINE, this.deadlineId);
            this.deadlineId = null;
        }
    }
}
