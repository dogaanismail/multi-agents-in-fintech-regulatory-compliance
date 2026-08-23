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

    enum AwaitedLedgerPosting {
        AUTHORISATION,
        SETTLEMENT,
        RELEASE
    }

    private PaymentId paymentId;
    private String deadlineId;
    private AwaitedLedgerPosting awaitedLedgerPosting;

    @StartSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerAuthorisationInitiatedEvent event,
                   DeadlineManager deadlineManager,
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer) {
        log.info("Requesting ledger authorisation for payment: {}", event.paymentId());

        this.paymentId = event.paymentId();

        awaitLedgerPosting(AwaitedLedgerPosting.AUTHORISATION, deadlineManager);
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

        awaitLedgerPosting(AwaitedLedgerPosting.SETTLEMENT, deadlineManager);
        ledgerPostingRequestedEventProducer.publishSettlement(event.paymentId());
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerReleaseInitiatedEvent event,
                   DeadlineManager deadlineManager,
                   LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer) {
        log.info("Requesting ledger release for payment: {}", event.paymentId());

        awaitLedgerPosting(AwaitedLedgerPosting.RELEASE, deadlineManager);
        ledgerPostingRequestedEventProducer.publishRelease(event.paymentId());
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
        } catch (Exception e) {
            log.error("Failed to fail the timed-out ledger posting for payment: {}", this.paymentId, e);
        }

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

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(LedgerReleaseFailedEvent event, DeadlineManager deadlineManager) {
        log.error("Ledger release failed for payment: {}, ending saga", event.paymentId());
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
