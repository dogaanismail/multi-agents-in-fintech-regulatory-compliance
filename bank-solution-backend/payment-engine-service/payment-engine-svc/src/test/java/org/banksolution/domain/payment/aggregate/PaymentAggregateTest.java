package org.banksolution.domain.payment.aggregate;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.banksolution.domain.payment.event.AccountChargeFailedEvent;
import org.banksolution.domain.payment.event.DecisionOverriddenEvent;
import org.banksolution.domain.payment.event.ManualReviewApprovedEvent;
import org.banksolution.domain.payment.event.ManualReviewRejectedEvent;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.banksolution.fixtures.PaymentFixtures.accountChargeInitiatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.accountChargedEvent;
import static org.banksolution.fixtures.PaymentFixtures.approveFraudCheckCommand;
import static org.banksolution.fixtures.PaymentFixtures.approveManualReviewCommand;
import static org.banksolution.fixtures.PaymentFixtures.blockAssessment;
import static org.banksolution.fixtures.PaymentFixtures.blockPaymentCommand;
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
    void blockPaymentEmitsBlockedAndCompletedEvents() {
        RiskAssessment riskAssessment = blockAssessment();
        String reason = String.format("Risk level: %s, Risk score: %s",
                riskAssessment.riskLevel(), riskAssessment.riskScore());

        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
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
        RiskAssessment riskAssessment = proceedAssessment();

        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(),
                        fraudCheckApprovedEvent(riskAssessment), accountChargeInitiatedEvent())
                .when(confirmAccountChargedCommand())
                .expectEvents(accountChargedEvent(),
                        paymentCompletedEvent(PaymentStatus.COMPLETED, "Payment successfully processed and account charged"));
    }

    @Test
    void failAccountChargeFailsAndCompletesPayment() {
        RiskAssessment riskAssessment = proceedAssessment();

        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(),
                        fraudCheckApprovedEvent(riskAssessment), accountChargeInitiatedEvent())
                .when(failAccountChargeCommand("Insufficient funds"))
                .expectEvents(new AccountChargeFailedEvent(paymentId(), "Insufficient funds"),
                        paymentCompletedEvent(PaymentStatus.FAILED, "Account charge failed: Insufficient funds"));
    }

    @Test
    void overrideDecisionFromBlockedEmitsDecisionOverridden() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent(), paymentBlockedEvent(blockAssessment()))
                .when(overrideDecisionCommand())
                .expectEvents(new DecisionOverriddenEvent(paymentId(), "officer-1", "False positive", true,
                        PaymentStatus.BLOCKED.name()));
    }

    @Test
    void approveManualReviewFromFraudPendingIsRejected() {
        fixture.given(paymentInitiatedEvent(), riskAssessmentInitiatedEvent())
                .when(approveManualReviewCommand())
                .expectException(InvalidPaymentStateException.class);
    }
}
