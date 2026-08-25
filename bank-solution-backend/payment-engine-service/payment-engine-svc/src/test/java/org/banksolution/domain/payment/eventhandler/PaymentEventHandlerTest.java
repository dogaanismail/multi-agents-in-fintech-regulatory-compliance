package org.banksolution.domain.payment.eventhandler;

import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.banksolution.enums.PaymentEventTrigger;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.infrastructure.messaging.kafka.producer.PaymentCompletedEventProducer;
import org.banksolution.infrastructure.messaging.kafka.producer.PaymentSnapshotEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.banksolution.enums.PaymentEventTrigger.*;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentEventHandlerTest {

    private static final EventMessage<?> EVENT_MESSAGE = GenericEventMessage.asEventMessage("ignored");

    @Mock
    private PaymentSnapshotEventProducer paymentSnapshotEventProducer;

    @Mock
    private PaymentCompletedEventProducer paymentCompletedEventProducer;

    @InjectMocks
    private PaymentEventHandler paymentEventHandler;

    @Test
    void shouldPublishOneSnapshotPerLifecycleEventWithItsOwnTrigger() {
        paymentEventHandler.on(createPaymentInitiatedEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createRiskAssessmentInitiatedEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createRiskAssessmentCompletedEvent(createProceedAssessment()), EVENT_MESSAGE);
        paymentEventHandler.on(createRiskAssessmentTimedOutEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createFraudCheckApprovedEvent(createProceedAssessment()), EVENT_MESSAGE);
        paymentEventHandler.on(createPaymentBlockedEvent(createBlockAssessment()), EVENT_MESSAGE);
        paymentEventHandler.on(createManualReviewRequestedEvent(createEscalateAssessment()), EVENT_MESSAGE);
        paymentEventHandler.on(createManualReviewApprovedEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createManualReviewRejectedEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createLedgerAuthorisationInitiatedEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createLedgerAuthorisedEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createLedgerAuthorisationDeclinedEvent("declined"), EVENT_MESSAGE);
        paymentEventHandler.on(createLedgerSettlementInitiatedEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createLedgerSettledEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createLedgerSettlementFailedEvent("failed"), EVENT_MESSAGE);
        paymentEventHandler.on(createLedgerReleaseInitiatedEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createLedgerReleasedEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createLedgerReleaseFailedEvent("failed"), EVENT_MESSAGE);

        for (PaymentEventTrigger paymentEventTrigger : new PaymentEventTrigger[]{
                PAYMENT_INITIATED, RISK_ASSESSMENT_INITIATED, RISK_ASSESSMENT_COMPLETED, RISK_ASSESSMENT_TIMED_OUT,
                FRAUD_CHECK_APPROVED, PAYMENT_BLOCKED, MANUAL_REVIEW_REQUESTED, MANUAL_REVIEW_APPROVED,
                MANUAL_REVIEW_REJECTED, LEDGER_AUTHORISATION_INITIATED, LEDGER_AUTHORISED,
                LEDGER_AUTHORISATION_DECLINED, LEDGER_SETTLEMENT_INITIATED, LEDGER_SETTLED, LEDGER_SETTLEMENT_FAILED,
                LEDGER_RELEASE_INITIATED, LEDGER_RELEASED, LEDGER_RELEASE_FAILED}) {
            verify(paymentSnapshotEventProducer).publish(createPaymentId(), paymentEventTrigger);
        }
        verifyNoInteractions(paymentCompletedEventProducer);
    }

    @Test
    void shouldDistinguishApprovedFromRejectedOverridesInTheSnapshotTrigger() {
        paymentEventHandler.on(createDecisionOverriddenEvent(true, "BLOCKED"), EVENT_MESSAGE);
        paymentEventHandler.on(createDecisionOverriddenEvent(false, "BLOCKED"), EVENT_MESSAGE);

        verify(paymentSnapshotEventProducer).publish(createPaymentId(), DECISION_OVERRIDE_APPROVED);
        verify(paymentSnapshotEventProducer).publish(createPaymentId(), DECISION_OVERRIDE_REJECTED);
    }

    @Test
    void shouldPublishTheCompletionSnapshotAndTheCompletedEvent() {
        paymentEventHandler.on(createPaymentCompletedEvent(PaymentStatus.COMPLETED, "done"), EVENT_MESSAGE);

        verify(paymentSnapshotEventProducer).publish(createPaymentId(), PAYMENT_COMPLETED);
        verify(paymentCompletedEventProducer).publish(createPaymentId());
    }

    @Test
    void shouldDeferPublishingUntilTheUnitOfWorkCommits() {
        UnitOfWork<?> unitOfWork = DefaultUnitOfWork.startAndGet(EVENT_MESSAGE);

        paymentEventHandler.on(createPaymentInitiatedEvent(), EVENT_MESSAGE);
        paymentEventHandler.on(createPaymentCompletedEvent(PaymentStatus.COMPLETED, "done"), EVENT_MESSAGE);
        verifyNoInteractions(paymentSnapshotEventProducer, paymentCompletedEventProducer);

        unitOfWork.commit();

        verify(paymentSnapshotEventProducer).publish(createPaymentId(), PAYMENT_INITIATED);
        verify(paymentSnapshotEventProducer).publish(createPaymentId(), PAYMENT_COMPLETED);
        verify(paymentCompletedEventProducer).publish(createPaymentId());
    }
}
