package org.banksolution.domain.payment.aggregate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.banksolution.domain.payment.command.*;
import org.banksolution.domain.payment.event.*;
import org.banksolution.domain.payment.valueobject.MarlAssessment;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.exception.InvalidPaymentStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * Every lifecycle timestamp is taken from the event's own {@code @Timestamp}, never from the
 * wall clock: event-sourcing handlers rerun on every load, so a {@code now()} there would
 * rewrite history each time the aggregate is reconstructed.
 */
@Getter
@Aggregate(snapshotTriggerDefinition = "snapshotTriggerDefinition")
@NoArgsConstructor
@Slf4j
public class PaymentAggregate {

    private static final String RISK_ASSESSMENT_TIMED_OUT_REASON = "Risk assessment timed out";
    private static final String LEDGER_SETTLED_REASON = "Payment successfully settled on the ledger";

    @AggregateIdentifier
    private PaymentId paymentId;

    private String referenceNumber;

    private UUID customerId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal convertedAmount;
    private BigDecimal appliedExchangeRate;
    private String paymentType;
    private String paymentScheme;
    private String fixedSide;
    private String description;
    private boolean isCrossBorderPayment;

    private PaymentStatus status;
    private FraudAnalysisStatus fraudStatus;

    private RiskAssessment riskAssessment;

    private Instant initiatedAt;
    private Instant riskAssessmentRequestedAt;
    private Instant riskAssessmentCompletedAt;
    private Instant fraudCheckApprovedAt;
    private Instant manualReviewRequestedAt;
    private Instant manualReviewApprovedAt;
    private Instant manualReviewRejectedAt;
    private String manualReviewedBy;
    private String manualReviewNotes;
    private Instant ledgerAuthorisationInitiatedAt;
    private Instant ledgerAuthorisedAt;
    private Instant ledgerSettlementInitiatedAt;
    private Instant ledgerSettledAt;
    private Instant ledgerReleaseInitiatedAt;
    private Instant ledgerReleasedAt;
    private Instant failedAt;
    private UUID authorisationTransferId;
    private UUID settlementTransferId;
    private Instant completedAt;
    private Instant blockedAt;
    private String blockReason;
    private String failureReason;

    private Instant decisionOverriddenAt;
    private String decisionOverriddenBy;
    private String decisionOverrideReason;

    private PaymentStatus releaseCompletionStatus;
    private String releaseCompletionReason;

    @CommandHandler
    public PaymentAggregate(InitiatePaymentCommand initiatePaymentCommand) {
        log.info("Handling InitiatePaymentCommand for payment: {}", initiatePaymentCommand.paymentId());

        apply(new PaymentInitiatedEvent(
                initiatePaymentCommand.paymentId(),
                initiatePaymentCommand.customerId(),
                initiatePaymentCommand.sourceAccountId(),
                initiatePaymentCommand.destinationAccountId(),
                initiatePaymentCommand.amount(),
                initiatePaymentCommand.fromCurrency(),
                initiatePaymentCommand.toCurrency(),
                initiatePaymentCommand.convertedAmount(),
                initiatePaymentCommand.appliedExchangeRate(),
                initiatePaymentCommand.paymentType(),
                initiatePaymentCommand.paymentScheme(),
                initiatePaymentCommand.fixedSide(),
                initiatePaymentCommand.isCrossBorderPayment(),
                initiatePaymentCommand.description()
        ));

        apply(new LedgerAuthorisationInitiatedEvent(
                initiatePaymentCommand.paymentId(),
                initiatePaymentCommand.customerId(),
                initiatePaymentCommand.sourceAccountId(),
                initiatePaymentCommand.destinationAccountId(),
                initiatePaymentCommand.amount(),
                initiatePaymentCommand.fromCurrency(),
                initiatePaymentCommand.convertedAmount(),
                initiatePaymentCommand.toCurrency(),
                initiatePaymentCommand.paymentType(),
                initiatePaymentCommand.paymentScheme(),
                initiatePaymentCommand.description()
        ));
    }

    @CommandHandler
    public void handle(ConfirmLedgerAuthorisationCommand confirmLedgerAuthorisationCommand) {
        log.info("Handling ConfirmLedgerAuthorisationCommand for payment: {}", confirmLedgerAuthorisationCommand.paymentId());

        if (this.status != PaymentStatus.AUTHORISATION_PENDING) {
            logStaleLedgerOutcome(confirmLedgerAuthorisationCommand);
            return;
        }

        apply(new LedgerAuthorisedEvent(
                confirmLedgerAuthorisationCommand.paymentId(),
                confirmLedgerAuthorisationCommand.transferId()));
    }

    @CommandHandler
    public void handle(DeclineLedgerAuthorisationCommand declineLedgerAuthorisationCommand) {
        log.warn("Handling DeclineLedgerAuthorisationCommand for payment: {}, reason: {}",
                declineLedgerAuthorisationCommand.paymentId(),
                declineLedgerAuthorisationCommand.reason());

        if (this.status != PaymentStatus.AUTHORISATION_PENDING) {
            logStaleLedgerOutcome(declineLedgerAuthorisationCommand);
            return;
        }

        apply(new LedgerAuthorisationDeclinedEvent(
                declineLedgerAuthorisationCommand.paymentId(),
                declineLedgerAuthorisationCommand.reason()));
    }

    @CommandHandler
    public void handle(ConfirmLedgerSettlementCommand confirmLedgerSettlementCommand) {
        log.info("Handling ConfirmLedgerSettlementCommand for payment: {}", confirmLedgerSettlementCommand.paymentId());

        if (this.status != PaymentStatus.SETTLEMENT_PENDING) {
            logStaleLedgerOutcome(confirmLedgerSettlementCommand);
            return;
        }

        apply(new LedgerSettledEvent(
                confirmLedgerSettlementCommand.paymentId(),
                confirmLedgerSettlementCommand.transferId()));
    }

    @CommandHandler
    public void handle(FailLedgerSettlementCommand failLedgerSettlementCommand) {
        log.error("Handling FailLedgerSettlementCommand for payment: {}, reason: {}",
                failLedgerSettlementCommand.paymentId(),
                failLedgerSettlementCommand.reason());

        if (this.status != PaymentStatus.SETTLEMENT_PENDING) {
            logStaleLedgerOutcome(failLedgerSettlementCommand);
            return;
        }

        apply(new LedgerSettlementFailedEvent(
                failLedgerSettlementCommand.paymentId(),
                failLedgerSettlementCommand.reason()));
    }

    @CommandHandler
    public void handle(ConfirmLedgerReleaseCommand confirmLedgerReleaseCommand) {
        log.info("Handling ConfirmLedgerReleaseCommand for payment: {}", confirmLedgerReleaseCommand.paymentId());

        if (this.status != PaymentStatus.RELEASE_PENDING) {
            logStaleLedgerOutcome(confirmLedgerReleaseCommand);
            return;
        }

        apply(new LedgerReleasedEvent(confirmLedgerReleaseCommand.paymentId()));
    }

    @CommandHandler
    public void handle(FailLedgerReleaseCommand failLedgerReleaseCommand) {
        log.error("Handling FailLedgerReleaseCommand for payment: {}, reason: {}",
                failLedgerReleaseCommand.paymentId(),
                failLedgerReleaseCommand.reason());

        if (this.status != PaymentStatus.RELEASE_PENDING) {
            logStaleLedgerOutcome(failLedgerReleaseCommand);
            return;
        }

        apply(new LedgerReleaseFailedEvent(
                failLedgerReleaseCommand.paymentId(),
                failLedgerReleaseCommand.reason()));
    }

    @CommandHandler
    public void handle(ExpireRiskAssessmentCommand expireRiskAssessmentCommand) {
        log.error("Handling ExpireRiskAssessmentCommand for payment: {}", expireRiskAssessmentCommand.paymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            log.info("Ignoring risk assessment expiry for payment: {}, the assessment already completed, status: {}",
                    expireRiskAssessmentCommand.paymentId(),
                    this.status);
            return;
        }

        apply(new RiskAssessmentTimedOutEvent(expireRiskAssessmentCommand.paymentId()));
    }

    @CommandHandler
    public void handle(CompleteRiskAssessmentCommand completeRiskAssessmentCommand) {
        log.info("Handling CompleteRiskAssessmentCommand for payment: {}", completeRiskAssessmentCommand.paymentId());

        // The risk engine's completion is delivered at-least-once and may arrive after the
        // timeout already decided the payment; both are ignored rather than rejected so the
        // Kafka record is acknowledged instead of parked on the DLT.
        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            log.info("Ignoring risk assessment completion for payment: {}, no assessment is pending, status: {}",
                    completeRiskAssessmentCommand.paymentId(),
                    this.status);
            return;
        }

        if (this.riskAssessmentCompletedAt != null) {
            log.info("Ignoring redelivered risk assessment completion for payment: {}",
                    completeRiskAssessmentCommand.paymentId());
            return;
        }

        apply(new RiskAssessmentCompletedEvent(
                completeRiskAssessmentCommand.paymentId(),
                completeRiskAssessmentCommand.riskAssessment()));
    }

    @CommandHandler
    public void handle(ApproveFraudCheckCommand approveFraudCheckCommand) {
        log.info("Handling ApproveFraudCheckCommand for payment: {}", approveFraudCheckCommand.paymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        apply(new FraudCheckApprovedEvent(
                approveFraudCheckCommand.paymentId(),
                approveFraudCheckCommand.riskAssessment()));
    }

    @CommandHandler
    public void handle(BlockPaymentCommand blockPaymentCommand) {
        log.info("Handling BlockPaymentCommand for payment: {}", blockPaymentCommand.paymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new InvalidPaymentStateException("Payment cannot be blocked from current status: " + this.status);
        }

        RiskAssessment blockingRiskAssessment = blockPaymentCommand.riskAssessment();
        String blockReason = String.format("Risk level: %s, Risk score: %s",
                blockingRiskAssessment.riskLevel(),
                blockingRiskAssessment.riskScore());

        apply(new PaymentBlockedEvent(
                blockPaymentCommand.paymentId(),
                blockReason,
                blockingRiskAssessment.riskScore(),
                toMaddpgQValue(blockingRiskAssessment.marlAssessment()),
                blockingRiskAssessment
        ));
    }

    @CommandHandler
    public void handle(RequestManualReviewCommand requestManualReviewCommand) {
        log.info("Handling RequestManualReviewCommand for payment: {}", requestManualReviewCommand.paymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        RiskAssessment escalatedRiskAssessment = requestManualReviewCommand.riskAssessment();

        apply(new ManualReviewRequestedEvent(
                requestManualReviewCommand.paymentId(),
                escalatedRiskAssessment.riskScore(),
                toMaddpgQValue(escalatedRiskAssessment.marlAssessment()),
                escalatedRiskAssessment
        ));
    }

    @CommandHandler
    public void handle(ApproveManualReviewCommand approveManualReviewCommand) {
        log.info("Handling ApproveManualReviewCommand for payment: {}", approveManualReviewCommand.paymentId());

        if (this.status != PaymentStatus.MANUAL_REVIEW_REQUIRED) {
            throw new InvalidPaymentStateException("Payment is not in MANUAL_REVIEW_REQUIRED status");
        }

        apply(new ManualReviewApprovedEvent(
                approveManualReviewCommand.paymentId(),
                approveManualReviewCommand.approvedBy(),
                approveManualReviewCommand.approvalNotes()
        ));
    }

    @CommandHandler
    public void handle(RejectManualReviewCommand rejectManualReviewCommand) {
        log.info("Handling RejectManualReviewCommand for payment: {}", rejectManualReviewCommand.paymentId());

        if (this.status != PaymentStatus.MANUAL_REVIEW_REQUIRED) {
            throw new InvalidPaymentStateException("Payment is not in MANUAL_REVIEW_REQUIRED status");
        }

        apply(new ManualReviewRejectedEvent(
                rejectManualReviewCommand.paymentId(),
                rejectManualReviewCommand.rejectedBy(),
                rejectManualReviewCommand.rejectionReason()
        ));
    }

    @CommandHandler
    public void handle(OverrideDecisionCommand overrideDecisionCommand) {
        log.info("Handling OverrideDecisionCommand for payment: {}", overrideDecisionCommand.paymentId());

        if (this.status != PaymentStatus.BLOCKED) {
            throw new InvalidPaymentStateException(
                    "Decision override is only allowed for BLOCKED payments, current status: " + this.status);
        }

        apply(new DecisionOverriddenEvent(
                overrideDecisionCommand.paymentId(),
                overrideDecisionCommand.overriddenBy(),
                overrideDecisionCommand.overrideReason(),
                overrideDecisionCommand.approvePayment(),
                this.status.name()
        ));
    }

    @EventSourcingHandler
    public void on(PaymentInitiatedEvent paymentInitiatedEvent, @Timestamp Instant occurredAt) {
        this.paymentId = paymentInitiatedEvent.paymentId();
        this.referenceNumber = toReferenceNumber(paymentInitiatedEvent.paymentId());
        this.customerId = paymentInitiatedEvent.customerId();
        this.sourceAccountId = paymentInitiatedEvent.sourceAccountId();
        this.destinationAccountId = paymentInitiatedEvent.destinationAccountId();
        this.amount = paymentInitiatedEvent.amount();
        this.fromCurrency = paymentInitiatedEvent.fromCurrency();
        this.toCurrency = paymentInitiatedEvent.toCurrency();
        this.convertedAmount = paymentInitiatedEvent.convertedAmount();
        this.appliedExchangeRate = paymentInitiatedEvent.appliedExchangeRate();
        this.paymentType = paymentInitiatedEvent.paymentType();
        this.paymentScheme = paymentInitiatedEvent.paymentScheme();
        this.fixedSide = paymentInitiatedEvent.fixedSide();
        this.isCrossBorderPayment = paymentInitiatedEvent.isCrossBorderPayment();
        this.description = paymentInitiatedEvent.description();
        this.status = PaymentStatus.INITIATED;
        this.fraudStatus = FraudAnalysisStatus.PENDING;
        this.initiatedAt = occurredAt;
        log.info("Payment initiated: {}", this.paymentId);
    }

    @EventSourcingHandler
    public void on(RiskAssessmentInitiatedEvent riskAssessmentInitiatedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.FRAUD_CHECK_PENDING;
        this.riskAssessmentRequestedAt = occurredAt;
        log.info("Risk assessment initiated event for payment: {}", riskAssessmentInitiatedEvent.paymentId());
    }

    @EventSourcingHandler
    public void on(RiskAssessmentCompletedEvent riskAssessmentCompletedEvent, @Timestamp Instant occurredAt) {
        this.riskAssessment = riskAssessmentCompletedEvent.riskAssessment();
        this.riskAssessmentCompletedAt = occurredAt;
        log.info("Risk assessment completed for payment: {}", riskAssessmentCompletedEvent.paymentId());
    }

    @EventSourcingHandler
    public void on(FraudCheckApprovedEvent fraudCheckApprovedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.FRAUD_CHECK_APPROVED;
        this.fraudStatus = FraudAnalysisStatus.APPROVED;
        this.fraudCheckApprovedAt = occurredAt;
        this.riskAssessment = fraudCheckApprovedEvent.riskAssessment();
        this.riskAssessmentCompletedAt = occurredAt;
        log.info("Fraud check approved for payment: {}", fraudCheckApprovedEvent.paymentId());

        apply(new LedgerSettlementInitiatedEvent(this.paymentId));
    }

    @EventSourcingHandler
    public void on(DecisionOverriddenEvent decisionOverriddenEvent, @Timestamp Instant occurredAt) {
        this.status = decisionOverriddenEvent.approvePayment()
                ? PaymentStatus.OVERRIDE_APPROVED
                : PaymentStatus.OVERRIDE_REJECTED;
        this.decisionOverriddenAt = occurredAt;
        this.decisionOverriddenBy = decisionOverriddenEvent.overriddenBy();
        this.decisionOverrideReason = decisionOverriddenEvent.overrideReason();

        log.info("Decision overridden for payment: {} by: {}, approve={}",
                decisionOverriddenEvent.paymentId(),
                decisionOverriddenEvent.overriddenBy(),
                decisionOverriddenEvent.approvePayment());
    }

    @EventSourcingHandler
    public void on(LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.AUTHORISATION_PENDING;
        this.ledgerAuthorisationInitiatedAt = occurredAt;
        log.info("Ledger authorisation initiated for payment: {}", ledgerAuthorisationInitiatedEvent.paymentId());
    }

    @EventSourcingHandler
    public void on(LedgerAuthorisedEvent ledgerAuthorisedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.AUTHORISED;
        this.ledgerAuthorisedAt = occurredAt;
        this.authorisationTransferId = ledgerAuthorisedEvent.transferId();
        log.info("Ledger authorised payment: {}, transferId: {}",
                ledgerAuthorisedEvent.paymentId(),
                ledgerAuthorisedEvent.transferId());

        apply(new RiskAssessmentInitiatedEvent(
                this.paymentId,
                this.customerId,
                this.sourceAccountId,
                this.destinationAccountId,
                this.amount,
                this.fromCurrency,
                this.toCurrency,
                this.paymentType,
                this.description
        ));
    }

    @EventSourcingHandler
    public void on(LedgerAuthorisationDeclinedEvent ledgerAuthorisationDeclinedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.AUTHORISATION_DECLINED;
        this.failedAt = occurredAt;
        this.failureReason = ledgerAuthorisationDeclinedEvent.reason();
        log.warn("Ledger declined authorisation for payment: {}, reason: {}",
                ledgerAuthorisationDeclinedEvent.paymentId(),
                ledgerAuthorisationDeclinedEvent.reason());

        apply(new PaymentCompletedEvent(
                this.paymentId,
                PaymentStatus.AUTHORISATION_DECLINED,
                ledgerAuthorisationDeclinedEvent.reason()
        ));
    }

    @EventSourcingHandler
    public void on(LedgerSettlementInitiatedEvent ledgerSettlementInitiatedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.SETTLEMENT_PENDING;
        this.ledgerSettlementInitiatedAt = occurredAt;
        log.info("Ledger settlement initiated for payment: {}", ledgerSettlementInitiatedEvent.paymentId());
    }

    @EventSourcingHandler
    public void on(LedgerSettledEvent ledgerSettledEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.SETTLED;
        this.ledgerSettledAt = occurredAt;
        this.settlementTransferId = ledgerSettledEvent.transferId();
        log.info("Ledger settled payment: {}, transferId: {}",
                ledgerSettledEvent.paymentId(),
                ledgerSettledEvent.transferId());

        apply(new PaymentCompletedEvent(this.paymentId, PaymentStatus.COMPLETED, LEDGER_SETTLED_REASON));
    }

    @EventSourcingHandler
    public void on(LedgerSettlementFailedEvent ledgerSettlementFailedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.FAILED;
        this.failedAt = occurredAt;
        this.failureReason = ledgerSettlementFailedEvent.reason();
        log.error("Ledger settlement failed for payment: {}, reason: {}",
                ledgerSettlementFailedEvent.paymentId(),
                ledgerSettlementFailedEvent.reason());

        apply(new PaymentCompletedEvent(this.paymentId, PaymentStatus.FAILED, ledgerSettlementFailedEvent.reason()));
    }

    @EventSourcingHandler
    public void on(LedgerReleaseInitiatedEvent ledgerReleaseInitiatedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.RELEASE_PENDING;
        this.ledgerReleaseInitiatedAt = occurredAt;
        log.info("Ledger release initiated for payment: {}", ledgerReleaseInitiatedEvent.paymentId());
    }

    @EventSourcingHandler
    public void on(LedgerReleasedEvent ledgerReleasedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.RELEASED;
        this.ledgerReleasedAt = occurredAt;
        log.info("Ledger released the authorisation for payment: {}", ledgerReleasedEvent.paymentId());

        apply(new PaymentCompletedEvent(this.paymentId, this.releaseCompletionStatus, this.releaseCompletionReason));
    }

    @EventSourcingHandler
    public void on(LedgerReleaseFailedEvent ledgerReleaseFailedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.FAILED;
        this.failedAt = occurredAt;
        this.failureReason = ledgerReleaseFailedEvent.reason();
        log.error("Ledger release failed for payment: {}, reason: {}",
                ledgerReleaseFailedEvent.paymentId(),
                ledgerReleaseFailedEvent.reason());

        apply(new PaymentCompletedEvent(this.paymentId, PaymentStatus.FAILED, ledgerReleaseFailedEvent.reason()));
    }

    @EventSourcingHandler
    public void on(RiskAssessmentTimedOutEvent riskAssessmentTimedOutEvent, @Timestamp Instant occurredAt) {
        this.failedAt = occurredAt;
        this.failureReason = RISK_ASSESSMENT_TIMED_OUT_REASON;
        this.releaseCompletionStatus = PaymentStatus.FAILED;
        this.releaseCompletionReason = RISK_ASSESSMENT_TIMED_OUT_REASON;
        log.error("Risk assessment timed out for payment: {}, releasing the held funds",
                riskAssessmentTimedOutEvent.paymentId());

        apply(new LedgerReleaseInitiatedEvent(this.paymentId));
    }

    @EventSourcingHandler
    public void on(PaymentBlockedEvent paymentBlockedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.BLOCKED;
        this.fraudStatus = FraudAnalysisStatus.BLOCKED;
        this.blockedAt = occurredAt;
        this.blockReason = paymentBlockedEvent.reason();
        this.riskAssessmentCompletedAt = occurredAt;
        this.riskAssessment = paymentBlockedEvent.riskAssessment();
        this.releaseCompletionStatus = PaymentStatus.BLOCKED;
        this.releaseCompletionReason = paymentBlockedEvent.reason();
        log.info("Payment blocked: {} - Reason: {}",
                paymentBlockedEvent.paymentId(),
                paymentBlockedEvent.reason());

        apply(new LedgerReleaseInitiatedEvent(this.paymentId));
    }

    @EventSourcingHandler
    public void on(ManualReviewRequestedEvent manualReviewRequestedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.MANUAL_REVIEW_REQUIRED;
        this.fraudStatus = FraudAnalysisStatus.REVIEW_REQUIRED;
        this.riskAssessment = manualReviewRequestedEvent.riskAssessment();
        this.riskAssessmentCompletedAt = occurredAt;
        this.manualReviewRequestedAt = occurredAt;
        log.info("Manual review requested for payment: {}", manualReviewRequestedEvent.paymentId());
    }

    @EventSourcingHandler
    public void on(ManualReviewApprovedEvent manualReviewApprovedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.FRAUD_CHECK_APPROVED;
        this.fraudStatus = FraudAnalysisStatus.APPROVED;
        this.manualReviewApprovedAt = occurredAt;
        this.manualReviewedBy = manualReviewApprovedEvent.approvedBy();
        this.manualReviewNotes = manualReviewApprovedEvent.approvalNotes();
        this.riskAssessmentCompletedAt = occurredAt;
        log.info("Manual review approved for payment: {} by {}",
                manualReviewApprovedEvent.paymentId(),
                manualReviewApprovedEvent.approvedBy());

        apply(new LedgerSettlementInitiatedEvent(this.paymentId));
    }

    @EventSourcingHandler
    public void on(ManualReviewRejectedEvent manualReviewRejectedEvent, @Timestamp Instant occurredAt) {
        this.status = PaymentStatus.BLOCKED;
        this.fraudStatus = FraudAnalysisStatus.BLOCKED;
        this.blockedAt = occurredAt;
        this.manualReviewRejectedAt = occurredAt;
        this.manualReviewedBy = manualReviewRejectedEvent.rejectedBy();
        this.manualReviewNotes = manualReviewRejectedEvent.rejectionReason();
        this.blockReason = "Manual review rejected: " + manualReviewRejectedEvent.rejectionReason();
        this.releaseCompletionStatus = PaymentStatus.BLOCKED;
        this.releaseCompletionReason = this.blockReason;
        log.info("Manual review rejected for payment: {} by: {}, reason: {}",
                manualReviewRejectedEvent.paymentId(),
                manualReviewRejectedEvent.rejectedBy(),
                manualReviewRejectedEvent.rejectionReason());

        apply(new LedgerReleaseInitiatedEvent(this.paymentId));
    }

    @EventSourcingHandler
    public void on(PaymentCompletedEvent paymentCompletedEvent, @Timestamp Instant occurredAt) {
        this.status = paymentCompletedEvent.finalStatus();
        this.completedAt = occurredAt;
        log.info("Payment completed with status: {} - {}",
                paymentCompletedEvent.finalStatus(),
                paymentCompletedEvent.reason());
    }

    private static String toReferenceNumber(PaymentId paymentId) {
        return "PAY-" + paymentId.toString().substring(0, 8).toUpperCase();
    }

    private static Double toMaddpgQValue(MarlAssessment marlAssessment) {
        return marlAssessment != null ? marlAssessment.maddpgQValue() : null;
    }

    /**
     * Ledger outcomes arrive over Kafka at-least-once, so an outcome can reach the aggregate
     * after the payment has already moved past the status that awaited it — typically a
     * redelivery. That is not an error: throwing would park a healthy message on the DLT.
     */
    private void logStaleLedgerOutcome(Object staleLedgerCommand) {
        log.info("Ignoring stale {} for payment: {}, status is already {}",
                staleLedgerCommand.getClass().getSimpleName(),
                this.paymentId,
                this.status);
    }
}
