package org.banksolution.domain.payment.saga;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.banksolution.domain.payment.command.DeclineLedgerAuthorisationCommand;
import org.banksolution.domain.payment.command.FailLedgerReleaseCommand;
import org.banksolution.domain.payment.command.FailLedgerSettlementCommand;
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
    private static final String TIMEOUT_REASON = "Ledger did not respond within the posting timeout";

    public enum AwaitedLedgerPosting {
        AUTHORISATION,
        SETTLEMENT,
        RELEASE
    }

    private PaymentId paymentId;
    private String deadlineId;
    private AwaitedLedgerPosting awaitedLedgerPosting;

    @StartSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent,
                   DeadlineManager deadlineManager,
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer) {
        log.info("Requesting ledger authorisation for payment: {}", ledgerAuthorisationInitiatedEvent.paymentId());

        this.paymentId = ledgerAuthorisationInitiatedEvent.paymentId();

        awaitLedgerPosting(AwaitedLedgerPosting.AUTHORISATION, deadlineManager);
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
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer) {
        log.info("Requesting ledger settlement for payment: {}", ledgerSettlementInitiatedEvent.paymentId());

        awaitLedgerPosting(AwaitedLedgerPosting.SETTLEMENT, deadlineManager);
        ledgerPostingRequestedEventProducer.publishSettlement(ledgerSettlementInitiatedEvent.paymentId());
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerReleaseInitiatedEvent ledgerReleaseInitiatedEvent,
                   DeadlineManager deadlineManager,
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer) {
        log.info("Requesting ledger release for payment: {}", ledgerReleaseInitiatedEvent.paymentId());

        awaitLedgerPosting(AwaitedLedgerPosting.RELEASE, deadlineManager);
        ledgerPostingRequestedEventProducer.publishRelease(ledgerReleaseInitiatedEvent.paymentId());
    }

    @DeadlineHandler(deadlineName = LEDGER_POSTING_TIMEOUT_DEADLINE)
    public void onLedgerPostingTimeout(PaymentId timedOutPaymentId, CommandGateway commandGateway) {
        log.error("Ledger posting timed out for payment: {} while awaiting {}, failing the payment",
                timedOutPaymentId,
                this.awaitedLedgerPosting);

        try {
            commandGateway.sendAndWait(switch (this.awaitedLedgerPosting) {
                case AUTHORISATION -> new DeclineLedgerAuthorisationCommand(this.paymentId, TIMEOUT_REASON);
                case SETTLEMENT -> new FailLedgerSettlementCommand(this.paymentId, TIMEOUT_REASON);
                case RELEASE -> new FailLedgerReleaseCommand(this.paymentId, TIMEOUT_REASON);
            });
        } catch (Exception exception) {
            log.error("Failed to fail the timed-out ledger posting for payment: {}", this.paymentId, exception);
        }

        SagaLifecycle.end();
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
            DeadlineManager deadlineManager) {

        this.awaitedLedgerPosting = awaitedLedgerPosting;
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
