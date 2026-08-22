package org.banksolution.domain.payment.aggregate;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.banksolution.fixtures.PaymentFixtures.*;

class PaymentAggregateTest {

    private FixtureConfiguration<PaymentAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
        fixture.setReportIllegalStateChange(false);
    }

    @Test
    void shouldAuthoriseOnTheLedgerBeforeRiskAssessment() {
        fixture.givenNoPriorActivity()
                .when(createInitiatePaymentCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent());
    }

    @Test
    void shouldStartRiskAssessmentOnlyOnceFundsAreAuthorised() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .when(createConfirmLedgerAuthorisationCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(createLedgerAuthorisedEvent(), createRiskAssessmentInitiatedEvent());
    }

    @Test
    void shouldFailThePaymentWhenTheLedgerDeclinesAuthorisation() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .when(createDeclineLedgerAuthorisationCommand("Insufficient funds"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createLedgerAuthorisationDeclinedEvent("Insufficient funds"),
                        createPaymentCompletedEvent(PaymentStatus.AUTHORISATION_DECLINED, "Insufficient funds"));
    }

    @Test
    void shouldSettleTheAuthorisationWhenFraudCheckApproves() {
        RiskAssessment riskAssessment = createProceedAssessment();

        fixture.given(authorisedPayment())
                .when(createApproveFraudCheckCommand(riskAssessment))
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createFraudCheckApprovedEvent(riskAssessment),
                        createLedgerSettlementInitiatedEvent());
    }

    @Test
    void shouldCompleteThePaymentOnceTheLedgerSettles() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent(),
                        createLedgerAuthorisedEvent(), createRiskAssessmentInitiatedEvent(),
                        createFraudCheckApprovedEvent(createProceedAssessment()),
                        createLedgerSettlementInitiatedEvent())
                .when(createConfirmLedgerSettlementCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createLedgerSettledEvent(),
                        createPaymentCompletedEvent(PaymentStatus.COMPLETED,
                                "Payment successfully settled on the ledger"));
    }

    @Test
    void shouldFailThePaymentWhenSettlementIsRejected() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent(),
                        createLedgerAuthorisedEvent(), createRiskAssessmentInitiatedEvent(),
                        createFraudCheckApprovedEvent(createProceedAssessment()),
                        createLedgerSettlementInitiatedEvent())
                .when(createFailLedgerSettlementCommand("Pending transfer expired"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createLedgerSettlementFailedEvent("Pending transfer expired"),
                        createPaymentCompletedEvent(PaymentStatus.FAILED, "Pending transfer expired"));
    }

    @Test
    void shouldReleaseTheAuthorisationWhenThePaymentIsBlocked() {
        RiskAssessment riskAssessment = createBlockAssessment();

        fixture.given(authorisedPayment())
                .when(createBlockPaymentCommand(riskAssessment))
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createPaymentBlockedEvent(riskAssessment),
                        createLedgerReleaseInitiatedEvent());
    }

    @Test
    void shouldSettleWhenManualReviewApproves() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent(),
                        createLedgerAuthorisedEvent(), createRiskAssessmentInitiatedEvent(),
                        createManualReviewRequestedEvent(createEscalateAssessment()))
                .when(createApproveManualReviewCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(createManualReviewApprovedEvent(), createLedgerSettlementInitiatedEvent());
    }

    @Test
    void shouldReleaseWhenManualReviewRejects() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent(),
                        createLedgerAuthorisedEvent(), createRiskAssessmentInitiatedEvent(),
                        createManualReviewRequestedEvent(createEscalateAssessment()))
                .when(createRejectManualReviewCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(createManualReviewRejectedEvent(), createLedgerReleaseInitiatedEvent());
    }

    @Test
    void shouldRequestManualReviewWhenRiskEscalates() {
        RiskAssessment riskAssessment = createEscalateAssessment();

        fixture.given(authorisedPayment())
                .when(createRequestManualReviewCommand(riskAssessment))
                .expectSuccessfulHandlerExecution()
                .expectEvents(createManualReviewRequestedEvent(riskAssessment));
    }

    @Test
    void shouldRejectSettlementConfirmationBeforeSettlementWasInitiated() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .when(createConfirmLedgerSettlementCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRejectAuthorisationConfirmationWhenNotAwaitingAuthorisation() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent(),
                        createLedgerAuthorisedEvent())
                .when(createConfirmLedgerAuthorisationCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRejectReleaseConfirmationBeforeReleaseWasInitiated() {
        fixture.given(authorisedPayment())
                .when(createConfirmLedgerReleaseCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    private static Object[] authorisedPayment() {
        return new Object[]{
                createPaymentInitiatedEvent(),
                createLedgerAuthorisationInitiatedEvent(),
                createLedgerAuthorisedEvent(),
                createRiskAssessmentInitiatedEvent()
        };
    }
}
