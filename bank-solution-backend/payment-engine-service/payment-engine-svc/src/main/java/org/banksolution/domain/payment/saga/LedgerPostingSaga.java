package org.banksolution.domain.payment.saga;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.banksolution.config.LedgerPostingTimeoutProperties;
import org.banksolution.domain.payment.event.*;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.infrastructure.messaging.kafka.producer.LedgerPostingRequestedEventProducer;

import java.time.Duration;

@Saga(sagaStore = "sagaStore")
@Slf4j
public class LedgerPostingSaga {

    private static final String PAYMENT_ID_ASSOCIATION = "paymentId";
    private static final String LEDGER_POSTING_TIMEOUT_DEADLINE = "ledger-posting-timeout";
    private static final int MAX_BACKOFF_DOUBLINGS = 20;

    public enum AwaitedLedgerPosting {
        AUTHORISATION,
        SETTLEMENT,
        RELEASE
    }

    private PaymentId paymentId;
    private String deadlineId;
    private AwaitedLedgerPosting awaitedLedgerPosting;
    private LedgerAuthorisationInitiatedEvent ledgerAuthorisationRequest;
    private int ledgerPostingResends;

    @StartSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent,
                   DeadlineManager deadlineManager,
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer,
                   LedgerPostingTimeoutProperties ledgerPostingTimeoutProperties) {
        log.info("Requesting ledger authorisation for payment: {}", ledgerAuthorisationInitiatedEvent.paymentId());

        this.paymentId = ledgerAuthorisationInitiatedEvent.paymentId();
        this.ledgerAuthorisationRequest = ledgerAuthorisationInitiatedEvent;

        awaitLedgerPosting(AwaitedLedgerPosting.AUTHORISATION, deadlineManager, ledgerPostingTimeoutProperties);
        ledgerPostingRequestedEventProducer.publishAuthorisation(ledgerAuthorisationInitiatedEvent);
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerAuthorisedEvent ledgerAuthorisedEvent, DeadlineManager deadlineManager) {
        log.info("Ledger authorised payment: {}, awaiting the compliance decision", ledgerAuthorisedEvent.paymentId());
        cancelTimeout(deadlineManager);
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerSettlementInitiatedEvent ledgerSettlementInitiatedEvent,
                   DeadlineManager deadlineManager,
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer,
                   LedgerPostingTimeoutProperties ledgerPostingTimeoutProperties) {
        log.info("Requesting ledger settlement for payment: {}", ledgerSettlementInitiatedEvent.paymentId());

        awaitLedgerPosting(AwaitedLedgerPosting.SETTLEMENT, deadlineManager, ledgerPostingTimeoutProperties);
        ledgerPostingRequestedEventProducer.publishSettlement(ledgerSettlementInitiatedEvent.paymentId());
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerReleaseInitiatedEvent ledgerReleaseInitiatedEvent,
                   DeadlineManager deadlineManager,
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer,
                   LedgerPostingTimeoutProperties ledgerPostingTimeoutProperties) {
        log.info("Requesting ledger release for payment: {}", ledgerReleaseInitiatedEvent.paymentId());

        awaitLedgerPosting(AwaitedLedgerPosting.RELEASE, deadlineManager, ledgerPostingTimeoutProperties);
        ledgerPostingRequestedEventProducer.publishRelease(ledgerReleaseInitiatedEvent.paymentId());
    }

    @DeadlineHandler(deadlineName = LEDGER_POSTING_TIMEOUT_DEADLINE)
    public void onLedgerPostingTimeout(
            PaymentId timedOutPaymentId,
            DeadlineManager deadlineManager,
            LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer,
            LedgerPostingTimeoutProperties ledgerPostingTimeoutProperties) {

        this.deadlineId = null;

        if (this.ledgerPostingResends >= ledgerPostingTimeoutProperties.getMaxResends()) {
            log.error("Ledger outcome for payment: {} is still unknown after {} resends of the {} request, awaiting manual reconciliation",
                    timedOutPaymentId,
                    this.ledgerPostingResends,
                    this.awaitedLedgerPosting);
            return;
        }

        this.ledgerPostingResends++;
        log.warn("No ledger outcome for payment: {} while awaiting {}, resending the request ({} of {})",
                timedOutPaymentId,
                this.awaitedLedgerPosting,
                this.ledgerPostingResends,
                ledgerPostingTimeoutProperties.getMaxResends());

        scheduleTimeout(computeTimeoutBeforeNextResend(ledgerPostingTimeoutProperties), deadlineManager);
        resendAwaitedLedgerPosting(ledgerPostingRequestedEventProducer);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerAuthorisationDeclinedEvent ledgerAuthorisationDeclinedEvent, DeadlineManager deadlineManager) {
        log.warn("Ledger declined authorisation for payment: {}, ending saga", ledgerAuthorisationDeclinedEvent.paymentId());
        cancelTimeout(deadlineManager);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerSettledEvent ledgerSettledEvent, DeadlineManager deadlineManager) {
        log.info("Ledger settled payment: {}, ending saga", ledgerSettledEvent.paymentId());
        cancelTimeout(deadlineManager);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerSettlementFailedEvent ledgerSettlementFailedEvent, DeadlineManager deadlineManager) {
        log.error("Ledger settlement failed for payment: {}, ending saga", ledgerSettlementFailedEvent.paymentId());
        cancelTimeout(deadlineManager);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerReleasedEvent ledgerReleasedEvent, DeadlineManager deadlineManager) {
        log.info("Ledger released the authorisation for payment: {}, ending saga", ledgerReleasedEvent.paymentId());
        cancelTimeout(deadlineManager);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerReleaseFailedEvent ledgerReleaseFailedEvent, DeadlineManager deadlineManager) {
        log.error("Ledger release failed for payment: {}, ending saga", ledgerReleaseFailedEvent.paymentId());
        cancelTimeout(deadlineManager);
    }

    private void awaitLedgerPosting(
            AwaitedLedgerPosting awaitedLedgerPosting,
            DeadlineManager deadlineManager,
            LedgerPostingTimeoutProperties ledgerPostingTimeoutProperties) {

        this.awaitedLedgerPosting = awaitedLedgerPosting;
        this.ledgerPostingResends = 0;
        scheduleTimeout(ledgerPostingTimeoutProperties.getInitialTimeout(), deadlineManager);
    }

    private void scheduleTimeout(Duration timeout, DeadlineManager deadlineManager) {
        this.deadlineId = deadlineManager.schedule(timeout, LEDGER_POSTING_TIMEOUT_DEADLINE, this.paymentId);
    }

    private Duration computeTimeoutBeforeNextResend(LedgerPostingTimeoutProperties ledgerPostingTimeoutProperties) {
        Duration backedOffTimeout = ledgerPostingTimeoutProperties.getInitialTimeout()
                .multipliedBy(1L << Math.min(this.ledgerPostingResends, MAX_BACKOFF_DOUBLINGS));
        return backedOffTimeout.compareTo(ledgerPostingTimeoutProperties.getMaxTimeout()) > 0
                ? ledgerPostingTimeoutProperties.getMaxTimeout()
                : backedOffTimeout;
    }

    private void resendAwaitedLedgerPosting(LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer) {
        try {
            switch (this.awaitedLedgerPosting) {
                case AUTHORISATION -> resendAuthorisation(ledgerPostingRequestedEventProducer);
                case SETTLEMENT -> ledgerPostingRequestedEventProducer.publishSettlement(this.paymentId);
                case RELEASE -> ledgerPostingRequestedEventProducer.publishRelease(this.paymentId);
            }
        } catch (RuntimeException resendFailure) {
            log.error("Could not resend the {} request for payment: {}, retrying at the next timeout",
                    this.awaitedLedgerPosting,
                    this.paymentId,
                    resendFailure);
        }
    }

    private void resendAuthorisation(LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer) {
        if (this.ledgerAuthorisationRequest == null) {
            log.warn("Authorisation request for payment: {} was not kept by this saga, waiting for the ledger without resending",
                    this.paymentId);
            return;
        }

        ledgerPostingRequestedEventProducer.publishAuthorisation(this.ledgerAuthorisationRequest);
    }

    private void cancelTimeout(DeadlineManager deadlineManager) {
        if (this.deadlineId != null) {
            deadlineManager.cancelSchedule(LEDGER_POSTING_TIMEOUT_DEADLINE, this.deadlineId);
            this.deadlineId = null;
        }
    }
}
