package org.banksolution.domain.payment.aggregate;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;

class PaymentAggregateTest {

    private static final Instant DECISION_TIME = Instant.parse("2026-08-26T12:00:00Z");

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
                .expectEvents(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.AUTHORISATION_PENDING);
                    assertThat(paymentAggregate.getFraudStatus()).isEqualTo(FraudAnalysisStatus.PENDING);
                    assertThat(paymentAggregate.getReferenceNumber()).isEqualTo("PAY-11111111");
                    assertThat(paymentAggregate.getAmount()).isEqualByComparingTo(AMOUNT);
                    assertThat(paymentAggregate.getInitiatedAt()).isNotNull();
                    assertThat(paymentAggregate.getLedgerAuthorisationInitiatedAt()).isNotNull();
                });
    }

    @Test
    void shouldStartRiskAssessmentOnlyOnceFundsAreAuthorised() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .when(createConfirmLedgerAuthorisationCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(createLedgerAuthorisedEvent(), createRiskAssessmentInitiatedEvent())
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.FRAUD_CHECK_PENDING);
                    assertThat(paymentAggregate.getAuthorisationTransferId()).isEqualTo(AUTHORISATION_TRANSFER_ID);
                    assertThat(paymentAggregate.getLedgerAuthorisedAt()).isNotNull();
                    assertThat(paymentAggregate.getRiskAssessmentRequestedAt()).isNotNull();
                });
    }

    @Test
    void shouldFailThePaymentWhenTheLedgerDeclinesAuthorisation() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .when(createDeclineLedgerAuthorisationCommand("Insufficient funds"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createLedgerAuthorisationDeclinedEvent("Insufficient funds"),
                        createPaymentCompletedEvent(PaymentStatus.AUTHORISATION_DECLINED, "Insufficient funds"))
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.AUTHORISATION_DECLINED);
                    assertThat(paymentAggregate.getFailureReason()).isEqualTo("Insufficient funds");
                    assertThat(paymentAggregate.getFailedAt()).isNotNull();
                    assertThat(paymentAggregate.getCompletedAt()).isNotNull();
                });
    }

    @Test
    void shouldSettleTheAuthorisationWhenFraudCheckApproves() {
        RiskAssessment riskAssessment = createProceedAssessment();

        fixture.given(authorisedPayment())
                .when(createApproveFraudCheckCommand(riskAssessment))
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createFraudCheckApprovedEvent(riskAssessment),
                        createLedgerSettlementInitiatedEvent())
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.SETTLEMENT_PENDING);
                    assertThat(paymentAggregate.getFraudStatus()).isEqualTo(FraudAnalysisStatus.APPROVED);
                    assertThat(paymentAggregate.getRiskAssessment()).isEqualTo(riskAssessment);
                    assertThat(paymentAggregate.getFraudCheckApprovedAt()).isNotNull();
                    assertThat(paymentAggregate.getRiskAssessmentCompletedAt()).isNotNull();
                });
    }

    @Test
    void shouldCompleteThePaymentOnceTheLedgerSettles() {
        fixture.given(settlementPendingPayment())
                .when(createConfirmLedgerSettlementCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createLedgerSettledEvent(),
                        createPaymentCompletedEvent(PaymentStatus.COMPLETED,
                                "Payment successfully settled on the ledger"))
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
                    assertThat(paymentAggregate.getSettlementTransferId()).isEqualTo(SETTLEMENT_TRANSFER_ID);
                    assertThat(paymentAggregate.getLedgerSettledAt()).isNotNull();
                    assertThat(paymentAggregate.getCompletedAt()).isNotNull();
                });
    }

    @Test
    void shouldFailThePaymentWhenSettlementIsRejected() {
        fixture.given(settlementPendingPayment())
                .when(createFailLedgerSettlementCommand("Pending transfer expired"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createLedgerSettlementFailedEvent("Pending transfer expired"),
                        createPaymentCompletedEvent(PaymentStatus.FAILED, "Pending transfer expired"))
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.FAILED);
                    assertThat(paymentAggregate.getFailureReason()).isEqualTo("Pending transfer expired");
                });
    }

    @Test
    void shouldReleaseTheAuthorisationWhenThePaymentIsBlocked() {
        RiskAssessment riskAssessment = createBlockAssessment();

        fixture.given(authorisedPayment())
                .when(createBlockPaymentCommand(riskAssessment))
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createPaymentBlockedEvent(riskAssessment),
                        createLedgerReleaseInitiatedEvent())
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.RELEASE_PENDING);
                    assertThat(paymentAggregate.getFraudStatus()).isEqualTo(FraudAnalysisStatus.BLOCKED);
                    assertThat(paymentAggregate.getBlockReason()).isEqualTo("Risk level: HIGH, Risk score: 0.95");
                    assertThat(paymentAggregate.getBlockedAt()).isNotNull();
                    assertThat(paymentAggregate.getReleaseCompletionStatus()).isEqualTo(PaymentStatus.BLOCKED);
                    assertThat(paymentAggregate.getReleaseCompletionReason()).isEqualTo("Risk level: HIGH, Risk score: 0.95");
                });
    }

    @Test
    void shouldCarryTheMaddpgQValueIntoTheBlockAndReviewEvents() {
        RiskAssessment blockAssessment = createRiskAssessmentWithMarl("BLOCK", "HIGH", 0.95);
        RiskAssessment escalateAssessment = createRiskAssessmentWithMarl("ESCALATE", "MEDIUM", 0.60);

        fixture.given(authorisedPayment())
                .when(createBlockPaymentCommand(blockAssessment))
                .expectEvents(createPaymentBlockedEvent(blockAssessment), createLedgerReleaseInitiatedEvent());

        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
        fixture.given(authorisedPayment())
                .when(createRequestManualReviewCommand(escalateAssessment))
                .expectEvents(createManualReviewRequestedEvent(escalateAssessment));
    }

    @Test
    void shouldNeverLeaveThePaymentInTheTransientAuthorisedStatus() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .andGivenCommands(createConfirmLedgerAuthorisationCommand())
                .when(createBlockPaymentCommand(createBlockAssessment()))
                .expectSuccessfulHandlerExecution()
                .expectState(paymentAggregate ->
                        assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.RELEASE_PENDING));
    }

    @Test
    void shouldCompleteAsBlockedWhenTheReleaseFollowsABlock() {
        fixture.given(authorisedPayment())
                .andGiven(createPaymentBlockedEvent(createBlockAssessment()), createLedgerReleaseInitiatedEvent())
                .when(createConfirmLedgerReleaseCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createLedgerReleasedEvent(),
                        createPaymentCompletedEvent(PaymentStatus.BLOCKED, "Risk level: HIGH, Risk score: 0.95"))
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.BLOCKED);
                    assertThat(paymentAggregate.getLedgerReleasedAt()).isNotNull();
                });
    }

    @Test
    void shouldRequestManualReviewWhenRiskEscalates() {
        RiskAssessment riskAssessment = createEscalateAssessment();

        fixture.given(authorisedPayment())
                .when(createRequestManualReviewCommand(riskAssessment))
                .expectSuccessfulHandlerExecution()
                .expectEvents(createManualReviewRequestedEvent(riskAssessment))
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.MANUAL_REVIEW_REQUIRED);
                    assertThat(paymentAggregate.getFraudStatus()).isEqualTo(FraudAnalysisStatus.REVIEW_REQUIRED);
                    assertThat(paymentAggregate.getManualReviewRequestedAt()).isNotNull();
                });
    }

    @Test
    void shouldSettleWhenManualReviewApproves() {
        fixture.given(manualReviewRequiredPayment())
                .when(createApproveManualReviewCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(createManualReviewApprovedEvent(), createLedgerSettlementInitiatedEvent())
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.SETTLEMENT_PENDING);
                    assertThat(paymentAggregate.getFraudStatus()).isEqualTo(FraudAnalysisStatus.APPROVED);
                    assertThat(paymentAggregate.getManualReviewedBy()).isEqualTo(OFFICER);
                    assertThat(paymentAggregate.getManualReviewNotes()).isEqualTo(APPROVAL_NOTES);
                    assertThat(paymentAggregate.getManualReviewApprovedAt()).isNotNull();
                });
    }

    @Test
    void shouldReleaseWhenManualReviewRejects() {
        fixture.given(manualReviewRequiredPayment())
                .when(createRejectManualReviewCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(createManualReviewRejectedEvent(), createLedgerReleaseInitiatedEvent())
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.RELEASE_PENDING);
                    assertThat(paymentAggregate.getFraudStatus()).isEqualTo(FraudAnalysisStatus.BLOCKED);
                    assertThat(paymentAggregate.getBlockReason()).isEqualTo("Manual review rejected: " + REJECTION_REASON);
                    assertThat(paymentAggregate.getReleaseCompletionStatus()).isEqualTo(PaymentStatus.BLOCKED);
                    assertThat(paymentAggregate.getManualReviewRejectedAt()).isNotNull();
                    assertThat(paymentAggregate.getManualReviewedBy()).isEqualTo(OFFICER);
                });
    }

    @Test
    void shouldCompleteAsBlockedWhenTheReleaseFollowsAManualRejection() {
        fixture.given(manualReviewRequiredPayment())
                .andGiven(createManualReviewRejectedEvent(), createLedgerReleaseInitiatedEvent())
                .when(createConfirmLedgerReleaseCommand())
                .expectEvents(
                        createLedgerReleasedEvent(),
                        createPaymentCompletedEvent(PaymentStatus.BLOCKED, "Manual review rejected: " + REJECTION_REASON));
    }

    @Test
    void shouldOverrideABlockedDecisionInEitherDirection() {
        fixture.given(blockedPayment())
                .when(createOverrideDecisionCommand(true))
                .expectSuccessfulHandlerExecution()
                .expectEvents(createDecisionOverriddenEvent(true, "BLOCKED"))
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.OVERRIDE_APPROVED);
                    assertThat(paymentAggregate.getDecisionOverriddenBy()).isEqualTo(OFFICER);
                    assertThat(paymentAggregate.getDecisionOverrideReason()).isEqualTo(OVERRIDE_REASON);
                    assertThat(paymentAggregate.getDecisionOverriddenAt()).isNotNull();
                });

        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
        fixture.given(blockedPayment())
                .when(createOverrideDecisionCommand(false))
                .expectEvents(createDecisionOverriddenEvent(false, "BLOCKED"))
                .expectState(paymentAggregate ->
                        assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.OVERRIDE_REJECTED));
    }

    @Test
    void shouldRejectAnOverrideUnlessThePaymentIsBlocked() {
        fixture.given(authorisedPayment())
                .when(createOverrideDecisionCommand(true))
                .expectException(InvalidPaymentStateException.class)
                .expectExceptionMessage("Decision override is only allowed for BLOCKED payments, current status: FRAUD_CHECK_PENDING");
    }

    @Test
    void shouldRecordTheRiskAssessmentInTheStreamWhenTheEngineCompletesIt() {
        RiskAssessment riskAssessment = createRiskAssessmentWithMarl("BLOCK", "HIGH", 0.95);

        fixture.given(authorisedPayment())
                .when(createCompleteRiskAssessmentCommand(riskAssessment))
                .expectSuccessfulHandlerExecution()
                .expectEvents(createRiskAssessmentCompletedEvent(riskAssessment))
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.FRAUD_CHECK_PENDING);
                    assertThat(paymentAggregate.getFraudStatus()).isEqualTo(FraudAnalysisStatus.PENDING);
                    assertThat(paymentAggregate.getRiskAssessment()).isEqualTo(riskAssessment);
                    assertThat(paymentAggregate.getRiskAssessmentCompletedAt()).isNotNull();
                });
    }

    @Test
    void shouldLeaveTheDecisionToTheSagaOnceTheCompletionIsRecorded() {
        fixture.given(authorisedPayment())
                .andGiven(createRiskAssessmentCompletedEvent(createProceedAssessment()))
                .when(createApproveFraudCheckCommand(createProceedAssessment()))
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createFraudCheckApprovedEvent(createProceedAssessment()),
                        createLedgerSettlementInitiatedEvent());
    }

    @Test
    void shouldIgnoreARedeliveredRiskAssessmentCompletion() {
        fixture.given(authorisedPayment())
                .andGiven(createRiskAssessmentCompletedEvent(createProceedAssessment()))
                .when(createCompleteRiskAssessmentCommand(createBlockAssessment()))
                .expectSuccessfulHandlerExecution()
                .expectNoEvents()
                .expectState(paymentAggregate ->
                        assertThat(paymentAggregate.getRiskAssessment()).isEqualTo(createProceedAssessment()));
    }

    @Test
    void shouldIgnoreACompletionThatArrivesAfterTheAssessmentTimedOut() {
        fixture.given(authorisedPayment())
                .andGiven(createRiskAssessmentTimedOutEvent(), createLedgerReleaseInitiatedEvent())
                .when(createCompleteRiskAssessmentCommand(createProceedAssessment()))
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();
    }

    @Test
    void shouldIgnoreACompletionWhenNoAssessmentIsPending() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .when(createCompleteRiskAssessmentCommand(createProceedAssessment()))
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();

        fixture.given(settlementPendingPayment())
                .when(createCompleteRiskAssessmentCommand(createProceedAssessment()))
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();
    }

    @Test
    void shouldRejectAFraudApprovalUnlessTheAssessmentIsPending() {
        fixture.given(settlementPendingPayment())
                .when(createApproveFraudCheckCommand(createProceedAssessment()))
                .expectException(InvalidPaymentStateException.class)
                .expectExceptionMessage("Payment is not in FRAUD_CHECK_PENDING status");
    }

    @Test
    void shouldRejectAManualReviewRequestUnlessTheAssessmentIsPending() {
        fixture.given(settlementPendingPayment())
                .when(createRequestManualReviewCommand(createEscalateAssessment()))
                .expectException(InvalidPaymentStateException.class)
                .expectExceptionMessage("Payment is not in FRAUD_CHECK_PENDING status");
    }

    @Test
    void shouldRejectABlockFromAStatusThatHoldsNoAuthorisation() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .when(createBlockPaymentCommand(createBlockAssessment()))
                .expectException(InvalidPaymentStateException.class)
                .expectExceptionMessage("Payment cannot be blocked from current status: AUTHORISATION_PENDING");
    }

    @Test
    void shouldRejectManualReviewDecisionsUnlessAReviewWasRequested() {
        fixture.given(authorisedPayment())
                .when(createApproveManualReviewCommand())
                .expectException(InvalidPaymentStateException.class)
                .expectExceptionMessage("Payment is not in MANUAL_REVIEW_REQUIRED status");

        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
        fixture.given(authorisedPayment())
                .when(createRejectManualReviewCommand())
                .expectException(InvalidPaymentStateException.class)
                .expectExceptionMessage("Payment is not in MANUAL_REVIEW_REQUIRED status");
    }

    @Test
    void shouldIgnoreARedeliveredSettlementOutcomeWhenNotAwaitingSettlement() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .when(createConfirmLedgerSettlementCommand())
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();

        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent())
                .when(createFailLedgerSettlementCommand("late"))
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();
    }

    @Test
    void shouldIgnoreARedeliveredAuthorisationOutcomeWhenAlreadyAuthorised() {
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent(),
                        createLedgerAuthorisedEvent())
                .when(createConfirmLedgerAuthorisationCommand())
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();

        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
        fixture.given(createPaymentInitiatedEvent(), createLedgerAuthorisationInitiatedEvent(),
                        createLedgerAuthorisedEvent())
                .when(createDeclineLedgerAuthorisationCommand("late"))
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();
    }

    @Test
    void shouldIgnoreARedeliveredReleaseOutcomeWhenNotAwaitingRelease() {
        fixture.given(authorisedPayment())
                .when(createConfirmLedgerReleaseCommand())
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();

        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
        fixture.given(authorisedPayment())
                .when(createFailLedgerReleaseCommand("late"))
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();
    }

    @Test
    void shouldReleaseHeldFundsWhenTheRiskAssessmentTimesOut() {
        fixture.given(authorisedPayment())
                .when(createExpireRiskAssessmentCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(createRiskAssessmentTimedOutEvent(), createLedgerReleaseInitiatedEvent())
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.RELEASE_PENDING);
                    assertThat(paymentAggregate.getFailureReason()).isEqualTo("Risk assessment timed out");
                    assertThat(paymentAggregate.getReleaseCompletionStatus()).isEqualTo(PaymentStatus.FAILED);
                });
    }

    @Test
    void shouldIgnoreRiskAssessmentExpiryWhenTheAssessmentAlreadyCompleted() {
        fixture.given(settlementPendingPayment())
                .when(createExpireRiskAssessmentCommand())
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();
    }

    @Test
    void shouldCompleteAsFailedWhenReleaseFollowsARiskAssessmentTimeout() {
        fixture.given(authorisedPayment())
                .andGiven(createRiskAssessmentTimedOutEvent(), createLedgerReleaseInitiatedEvent())
                .when(createConfirmLedgerReleaseCommand())
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createLedgerReleasedEvent(),
                        createPaymentCompletedEvent(PaymentStatus.FAILED, "Risk assessment timed out"));
    }

    @Test
    void shouldFailThePaymentWhenTheLedgerReleaseFails() {
        fixture.given(authorisedPayment())
                .andGiven(createPaymentBlockedEvent(createBlockAssessment()), createLedgerReleaseInitiatedEvent())
                .when(createFailLedgerReleaseCommand("Pending authorisation not found"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(
                        createLedgerReleaseFailedEvent("Pending authorisation not found"),
                        createPaymentCompletedEvent(PaymentStatus.FAILED, "Pending authorisation not found"))
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.FAILED);
                    assertThat(paymentAggregate.getFailureReason()).isEqualTo("Pending authorisation not found");
                });
    }

    @Test
    void shouldStampLifecycleTimestampsFromTheEventClockNotTheWallClock() {
        fixture.givenCurrentTime(DECISION_TIME)
                .andGiven(authorisedPayment())
                .when(createApproveFraudCheckCommand(createProceedAssessment()))
                .expectState(paymentAggregate -> {
                    assertThat(paymentAggregate.getInitiatedAt()).isEqualTo(DECISION_TIME);
                    assertThat(paymentAggregate.getLedgerAuthorisedAt()).isEqualTo(DECISION_TIME);
                    assertThat(paymentAggregate.getRiskAssessmentRequestedAt()).isEqualTo(DECISION_TIME);
                    assertThat(paymentAggregate.getFraudCheckApprovedAt()).isEqualTo(DECISION_TIME);
                    assertThat(paymentAggregate.getLedgerSettlementInitiatedAt()).isEqualTo(DECISION_TIME);
                });
    }

    private static Object[] authorisedPayment() {
        return new Object[]{
                createPaymentInitiatedEvent(),
                createLedgerAuthorisationInitiatedEvent(),
                createLedgerAuthorisedEvent(),
                createRiskAssessmentInitiatedEvent()
        };
    }

    private static Object[] settlementPendingPayment() {
        return new Object[]{
                createPaymentInitiatedEvent(),
                createLedgerAuthorisationInitiatedEvent(),
                createLedgerAuthorisedEvent(),
                createRiskAssessmentInitiatedEvent(),
                createFraudCheckApprovedEvent(createProceedAssessment()),
                createLedgerSettlementInitiatedEvent()
        };
    }

    private static Object[] manualReviewRequiredPayment() {
        return new Object[]{
                createPaymentInitiatedEvent(),
                createLedgerAuthorisationInitiatedEvent(),
                createLedgerAuthorisedEvent(),
                createRiskAssessmentInitiatedEvent(),
                createManualReviewRequestedEvent(createEscalateAssessment())
        };
    }

    private static Object[] blockedPayment() {
        return new Object[]{
                createPaymentInitiatedEvent(),
                createLedgerAuthorisationInitiatedEvent(),
                createLedgerAuthorisedEvent(),
                createRiskAssessmentInitiatedEvent(),
                createPaymentBlockedEvent(createBlockAssessment()),
                createLedgerReleaseInitiatedEvent(),
                createLedgerReleasedEvent(),
                createPaymentCompletedEvent(PaymentStatus.BLOCKED, "Risk level: HIGH, Risk score: 0.95")
        };
    }
}
