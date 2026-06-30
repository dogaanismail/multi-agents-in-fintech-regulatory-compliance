package org.banksolution.domain.payment.aggregate;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.banksolution.domain.payment.event.DecisionOverriddenEvent;
import org.banksolution.domain.payment.event.ManualReviewApprovedEvent;
import org.banksolution.domain.payment.event.ManualReviewRejectedEvent;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.banksolution.fixtures.PaymentFixtures.createAccountChargeFailedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createAccountChargeInitiatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createAccountChargedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createApproveFraudCheckCommand;
import static org.banksolution.fixtures.PaymentFixtures.createApproveManualReviewCommand;
import static org.banksolution.fixtures.PaymentFixtures.createBlockAssessment;
import static org.banksolution.fixtures.PaymentFixtures.createBlockPaymentCommand;
import static org.banksolution.fixtures.PaymentFixtures.createChargeAccountCommand;
import static org.banksolution.fixtures.PaymentFixtures.createConfirmAccountChargedCommand;
import static org.banksolution.fixtures.PaymentFixtures.createEscalateAssessment;
import static org.banksolution.fixtures.PaymentFixtures.createFailAccountChargeCommand;
import static org.banksolution.fixtures.PaymentFixtures.createFraudCheckApprovedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createInitiatePaymentCommand;
import static org.banksolution.fixtures.PaymentFixtures.createManualReviewRequestedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createOverrideDecisionCommand;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentBlockedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentCompletedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentId;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentInitiatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createProceedAssessment;
import static org.banksolution.fixtures.PaymentFixtures.createRejectManualReviewCommand;
import static org.banksolution.fixtures.PaymentFixtures.createRequestManualReviewCommand;
import static org.banksolution.fixtures.PaymentFixtures.createRiskAssessmentInitiatedEvent;

class PaymentAggregateTest {

    private FixtureConfiguration<PaymentAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
        fixture.setReportIllegalStateChange(false);
    }

    @Test
    void shouldEmitPaymentInitiatedAndRiskAssessmentInitiatedOnInitiate() {
        fixture.givenNoPriorActivity()
                .when(createInitiatePaymentCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent());
    }

    @Test
    void shouldApproveFraudCheckAndInitiateAccountCharge() {
        RiskAssessment riskAssessment = createProceedAssessment();

        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent())
                .when(createApproveFraudCheckCommand(riskAssessment))
                .expectEvents(createFraudCheckApprovedEvent(riskAssessment), createAccountChargeInitiatedEvent());
    }

    @Test
    void shouldEmitAccountChargeInitiatedWhenChargingApprovedPayment() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(), createFraudCheckApprovedEvent(createProceedAssessment()))
                .when(createChargeAccountCommand())
                .expectEvents(createAccountChargeInitiatedEvent());
    }

    @Test
    void shouldEmitBlockedAndCompletedWhenBlockingFromFraudPending() {
        RiskAssessment riskAssessment = createBlockAssessment();
        String reason = String.format("Risk level: %s, Risk score: %s",
                riskAssessment.riskLevel(), riskAssessment.riskScore());

        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent())
                .when(createBlockPaymentCommand(riskAssessment))
                .expectEvents(createPaymentBlockedEvent(riskAssessment),
                        createPaymentCompletedEvent(PaymentStatus.BLOCKED, reason));
    }

    @Test
    void shouldEmitBlockedAndCompletedWhenBlockingFromAccountChargePending() {
        RiskAssessment riskAssessment = createBlockAssessment();
        String reason = String.format("Risk level: %s, Risk score: %s",
                riskAssessment.riskLevel(), riskAssessment.riskScore());

        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(),
                        createFraudCheckApprovedEvent(createProceedAssessment()), createAccountChargeInitiatedEvent())
                .when(createBlockPaymentCommand(riskAssessment))
                .expectEvents(createPaymentBlockedEvent(riskAssessment),
                        createPaymentCompletedEvent(PaymentStatus.BLOCKED, reason));
    }

    @Test
    void shouldEmitManualReviewRequestedWhenRequestingManualReview() {
        RiskAssessment riskAssessment = createEscalateAssessment();

        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent())
                .when(createRequestManualReviewCommand(riskAssessment))
                .expectEvents(createManualReviewRequestedEvent(riskAssessment));
    }

    @Test
    void shouldResumeIntoAccountChargeWhenApprovingManualReview() {
        RiskAssessment riskAssessment = createEscalateAssessment();

        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(), createManualReviewRequestedEvent(riskAssessment))
                .when(createApproveManualReviewCommand())
                .expectEvents(new ManualReviewApprovedEvent(createPaymentId(), "officer-1", "Looks legitimate"),
                        createAccountChargeInitiatedEvent());
    }

    @Test
    void shouldBlockAndCompletePaymentWhenRejectingManualReview() {
        RiskAssessment riskAssessment = createEscalateAssessment();

        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(), createManualReviewRequestedEvent(riskAssessment))
                .when(createRejectManualReviewCommand())
                .expectEvents(new ManualReviewRejectedEvent(createPaymentId(), "officer-1", "Confirmed fraud"),
                        createPaymentCompletedEvent(PaymentStatus.BLOCKED, "Manual review rejected: Confirmed fraud"));
    }

    @Test
    void shouldCompletePaymentWhenConfirmingAccountCharged() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(),
                        createFraudCheckApprovedEvent(createProceedAssessment()), createAccountChargeInitiatedEvent())
                .when(createConfirmAccountChargedCommand())
                .expectEvents(createAccountChargedEvent(),
                        createPaymentCompletedEvent(PaymentStatus.COMPLETED, "Payment successfully processed and account charged"));
    }

    @Test
    void shouldFailAndCompletePaymentWhenAccountChargeFails() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(),
                        createFraudCheckApprovedEvent(createProceedAssessment()), createAccountChargeInitiatedEvent())
                .when(createFailAccountChargeCommand("Insufficient funds"))
                .expectEvents(createAccountChargeFailedEvent("Insufficient funds"),
                        createPaymentCompletedEvent(PaymentStatus.FAILED, "Account charge failed: Insufficient funds"));
    }

    @Test
    void shouldEmitDecisionOverriddenWhenApprovingOverrideFromBlocked() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(), createPaymentBlockedEvent(createBlockAssessment()))
                .when(createOverrideDecisionCommand(true))
                .expectEvents(new DecisionOverriddenEvent(createPaymentId(), "officer-1", "False positive", true,
                        PaymentStatus.BLOCKED.name()));
    }

    @Test
    void shouldEmitDecisionOverriddenWhenRejectingOverrideFromBlocked() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(), createPaymentBlockedEvent(createBlockAssessment()))
                .when(createOverrideDecisionCommand(false))
                .expectEvents(new DecisionOverriddenEvent(createPaymentId(), "officer-1", "False positive", false,
                        PaymentStatus.BLOCKED.name()));
    }

    @Test
    void shouldRejectApproveFraudCheckWhenNotFraudPending() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(), createFraudCheckApprovedEvent(createProceedAssessment()))
                .when(createApproveFraudCheckCommand(createProceedAssessment()))
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRejectBlockPaymentWhenNotPendingOrCharging() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(), createFraudCheckApprovedEvent(createProceedAssessment()))
                .when(createBlockPaymentCommand(createBlockAssessment()))
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRejectRequestManualReviewWhenNotFraudPending() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent(), createFraudCheckApprovedEvent(createProceedAssessment()))
                .when(createRequestManualReviewCommand(createEscalateAssessment()))
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRejectChargeAccountWhenNotApproved() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent())
                .when(createChargeAccountCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRejectApproveManualReviewWhenNotInReview() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent())
                .when(createApproveManualReviewCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRejectRejectManualReviewWhenNotInReview() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent())
                .when(createRejectManualReviewCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRejectConfirmAccountChargedWhenNotCharging() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent())
                .when(createConfirmAccountChargedCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRejectFailAccountChargeWhenNotCharging() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent())
                .when(createFailAccountChargeCommand("Insufficient funds"))
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void shouldRejectOverrideDecisionWhenNotBlocked() {
        fixture.given(createPaymentInitiatedEvent(), createRiskAssessmentInitiatedEvent())
                .when(createOverrideDecisionCommand(true))
                .expectException(InvalidPaymentStateException.class);
    }
}
