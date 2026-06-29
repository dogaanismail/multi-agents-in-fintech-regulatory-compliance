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

import static org.banksolution.fixtures.PaymentFixtures.accountChargeFailedEvent;
import static org.banksolution.fixtures.PaymentFixtures.accountChargeInitiatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.accountChargedEvent;
import static org.banksolution.fixtures.PaymentFixtures.approveFraudCheckCommand;
import static org.banksolution.fixtures.PaymentFixtures.approveManualReviewCommand;
import static org.banksolution.fixtures.PaymentFixtures.blockAssessment;
import static org.banksolution.fixtures.PaymentFixtures.blockPaymentCommand;
import static org.banksolution.fixtures.PaymentFixtures.chargeAccountCommand;
import static org.banksolution.fixtures.PaymentFixtures.confirmAccountChargedCommand;
import static org.banksolution.fixtures.PaymentFixtures.escalateAssessment;
import static org.banksolution.fixtures.PaymentFixtures.failAccountChargeCommand;
import static org.banksolution.fixtures.PaymentFixtures.fraudCheckApprovedEvent;
import static org.banksolution.fixtures.PaymentFixtures.initiatePaymentCommand;
import static org.banksolution.fixtures.PaymentFixtures.manualReviewRequestedEvent;
import static org.banksolution.fixtures.PaymentFixtures.overrideDecisionCommand;
import static org.banksolution.fixtures.PaymentFixtures.paymentBlockedEvent;
import static org.banksolution.fixtures.PaymentFixtures.paymentCompletedEvent;
import static org.banksolution.fixtures.PaymentFixtures.paymentId;
import static org.banksolution.fixtures.PaymentFixtures.paymentInitiatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.proceedAssessment;
import static org.banksolution.fixtures.PaymentFixtures.rejectManualReviewCommand;
import static org.banksolution.fixtures.PaymentFixtures.requestManualReviewCommand;
import static org.banksolution.fixtures.PaymentFixtures.riskAssessmentInitiatedEvent;

class PaymentAggregateTest {

    private FixtureConfiguration<PaymentAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
        fixture.setReportIllegalStateChange(false);
    }

    @Test
    void initiatePaymentEmitsPaymentInitiatedAndRiskAssessmentInitiated() {
        fixture.givenNoPriorActivity()
                .when(initiatePaymentCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(paymentInitiatedEvent(), riskAssessmentInitiatedEvent());
    }

    @Test
    void approveFraudCheckApprovesAndInitiatesAccountCharge() {
        RiskAssessment riskAssessment = proceedAssessment();

        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
                .when(approveFraudCheckCommand(riskAssessment))
                .expectEvents(fraudCheckApprovedEvent(riskAssessment), accountChargeInitiatedEvent());
    }

    @Test
    void chargeAccountFromApprovedEmitsAccountChargeInitiated() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(), fraudCheckApprovedEvent(proceedAssessment()))
                .when(chargeAccountCommand())
                .expectEvents(accountChargeInitiatedEvent());
    }

    @Test
    void blockPaymentFromFraudPendingEmitsBlockedAndCompleted() {
        RiskAssessment riskAssessment = blockAssessment();
        String reason = String.format("Risk level: %s, Risk score: %s",
                riskAssessment.riskLevel(), riskAssessment.riskScore());

        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
                .when(blockPaymentCommand(riskAssessment))
                .expectEvents(paymentBlockedEvent(riskAssessment),
                        paymentCompletedEvent(PaymentStatus.BLOCKED, reason));
    }

    @Test
    void blockPaymentFromAccountChargePendingEmitsBlockedAndCompleted() {
        RiskAssessment riskAssessment = blockAssessment();
        String reason = String.format("Risk level: %s, Risk score: %s",
                riskAssessment.riskLevel(), riskAssessment.riskScore());

        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(),
                        fraudCheckApprovedEvent(proceedAssessment()), accountChargeInitiatedEvent())
                .when(blockPaymentCommand(riskAssessment))
                .expectEvents(paymentBlockedEvent(riskAssessment),
                        paymentCompletedEvent(PaymentStatus.BLOCKED, reason));
    }

    @Test
    void requestManualReviewEmitsManualReviewRequested() {
        RiskAssessment riskAssessment = escalateAssessment();

        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
                .when(requestManualReviewCommand(riskAssessment))
                .expectEvents(manualReviewRequestedEvent(riskAssessment));
    }

    @Test
    void approveManualReviewResumesIntoAccountCharge() {
        RiskAssessment riskAssessment = escalateAssessment();

        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(), manualReviewRequestedEvent(riskAssessment))
                .when(approveManualReviewCommand())
                .expectEvents(new ManualReviewApprovedEvent(paymentId(), "officer-1", "Looks legitimate"),
                        accountChargeInitiatedEvent());
    }

    @Test
    void rejectManualReviewBlocksAndCompletesPayment() {
        RiskAssessment riskAssessment = escalateAssessment();

        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(), manualReviewRequestedEvent(riskAssessment))
                .when(rejectManualReviewCommand())
                .expectEvents(new ManualReviewRejectedEvent(paymentId(), "officer-1", "Confirmed fraud"),
                        paymentCompletedEvent(PaymentStatus.BLOCKED, "Manual review rejected: Confirmed fraud"));
    }

    @Test
    void confirmAccountChargedCompletesPayment() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(),
                        fraudCheckApprovedEvent(proceedAssessment()), accountChargeInitiatedEvent())
                .when(confirmAccountChargedCommand())
                .expectEvents(accountChargedEvent(),
                        paymentCompletedEvent(PaymentStatus.COMPLETED, "Payment successfully processed and account charged"));
    }

    @Test
    void failAccountChargeFailsAndCompletesPayment() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(),
                        fraudCheckApprovedEvent(proceedAssessment()), accountChargeInitiatedEvent())
                .when(failAccountChargeCommand("Insufficient funds"))
                .expectEvents(accountChargeFailedEvent("Insufficient funds"),
                        paymentCompletedEvent(PaymentStatus.FAILED, "Account charge failed: Insufficient funds"));
    }

    @Test
    void overrideDecisionApproveFromBlockedEmitsDecisionOverridden() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(), paymentBlockedEvent(blockAssessment()))
                .when(overrideDecisionCommand(true))
                .expectEvents(new DecisionOverriddenEvent(paymentId(), "officer-1", "False positive", true,
                        PaymentStatus.BLOCKED.name()));
    }

    @Test
    void overrideDecisionRejectFromBlockedEmitsDecisionOverridden() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(), paymentBlockedEvent(blockAssessment()))
                .when(overrideDecisionCommand(false))
                .expectEvents(new DecisionOverriddenEvent(paymentId(), "officer-1", "False positive", false,
                        PaymentStatus.BLOCKED.name()));
    }

    @Test
    void approveFraudCheckRejectedWhenNotFraudPending() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(), fraudCheckApprovedEvent(proceedAssessment()))
                .when(approveFraudCheckCommand(proceedAssessment()))
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void blockPaymentRejectedWhenNotPendingOrCharging() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(), fraudCheckApprovedEvent(proceedAssessment()))
                .when(blockPaymentCommand(blockAssessment()))
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void requestManualReviewRejectedWhenNotFraudPending() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(), fraudCheckApprovedEvent(proceedAssessment()))
                .when(requestManualReviewCommand(escalateAssessment()))
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void chargeAccountRejectedWhenNotApproved() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
                .when(chargeAccountCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void approveManualReviewRejectedWhenNotInReview() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
                .when(approveManualReviewCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void rejectManualReviewRejectedWhenNotInReview() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
                .when(rejectManualReviewCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void confirmAccountChargedRejectedWhenNotCharging() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
                .when(confirmAccountChargedCommand())
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void failAccountChargeRejectedWhenNotCharging() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
                .when(failAccountChargeCommand("Insufficient funds"))
                .expectException(InvalidPaymentStateException.class);
    }

    @Test
    void overrideDecisionRejectedWhenNotBlocked() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
                .when(overrideDecisionCommand(true))
                .expectException(InvalidPaymentStateException.class);
    }
}
