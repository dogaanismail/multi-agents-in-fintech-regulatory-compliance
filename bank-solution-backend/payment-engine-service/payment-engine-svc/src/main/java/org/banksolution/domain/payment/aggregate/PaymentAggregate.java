package org.banksolution.domain.payment.aggregate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateVersion;
import org.axonframework.spring.stereotype.Aggregate;
import org.banksolution.domain.payment.command.*;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.domain.payment.event.*;
import org.banksolution.exception.InvalidPaymentStateException;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Getter
@Aggregate(
        snapshotTriggerDefinition = "snapshotTriggerDefinition",
        cache = "paymentCache"
)
@NoArgsConstructor
@Slf4j
public class PaymentAggregate {

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

    // Lifecycle Timestamps
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

    @AggregateVersion
    private Long version;

    @CommandHandler
    public PaymentAggregate(InitiatePaymentCommand command) {
        log.info("Handling InitiatePaymentCommand for payment: {}", command.paymentId());

        apply(new PaymentInitiatedEvent(
                command.paymentId(),
                command.customerId(),
                command.sourceAccountId(),
                command.destinationAccountId(),
                command.amount(),
                command.fromCurrency(),
                command.toCurrency(),
                command.convertedAmount(),
                command.appliedExchangeRate(),
                command.paymentType(),
                command.paymentScheme(),
                command.fixedSide(),
                command.isCrossBorderPayment(),
                command.description()
        ));

        apply(new LedgerAuthorisationInitiatedEvent(
                command.paymentId(),
                command.customerId(),
                command.sourceAccountId(),
                command.destinationAccountId(),
                command.amount(),
                command.fromCurrency(),
                command.convertedAmount(),
                command.toCurrency(),
                command.paymentType(),
                command.paymentScheme(),
                command.description()
        ));
    }

    @CommandHandler
    public void handle(ConfirmLedgerAuthorisationCommand command) {
        log.info("Handling ConfirmLedgerAuthorisationCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.AUTHORISATION_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in AUTHORISATION_PENDING status");
        }

        apply(new LedgerAuthorisedEvent(command.paymentId(), command.transferId()));
    }

    @CommandHandler
    public void handle(DeclineLedgerAuthorisationCommand command) {
        log.warn("Handling DeclineLedgerAuthorisationCommand for payment: {}, reason: {}",
                command.paymentId(), command.reason());

        if (this.status != PaymentStatus.AUTHORISATION_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in AUTHORISATION_PENDING status");
        }

        apply(new LedgerAuthorisationDeclinedEvent(command.paymentId(), command.reason()));
    }

    @CommandHandler
    public void handle(ConfirmLedgerSettlementCommand command) {
        log.info("Handling ConfirmLedgerSettlementCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.SETTLEMENT_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in SETTLEMENT_PENDING status");
        }

        apply(new LedgerSettledEvent(command.paymentId(), command.transferId()));
    }

    @CommandHandler
    public void handle(FailLedgerSettlementCommand command) {
        log.error("Handling FailLedgerSettlementCommand for payment: {}, reason: {}",
                command.paymentId(), command.reason());

        if (this.status != PaymentStatus.SETTLEMENT_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in SETTLEMENT_PENDING status");
        }

        apply(new LedgerSettlementFailedEvent(command.paymentId(), command.reason()));
    }

    @CommandHandler
    public void handle(ConfirmLedgerReleaseCommand command) {
        log.info("Handling ConfirmLedgerReleaseCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.RELEASE_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in RELEASE_PENDING status");
        }

        apply(new LedgerReleasedEvent(command.paymentId()));
    }

    @CommandHandler
    public void handle(ApproveFraudCheckCommand command) {
        log.info("Handling ApproveFraudCheckCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        apply(new FraudCheckApprovedEvent(command.paymentId(), command.riskAssessment()));
    }

    @CommandHandler
    public void handle(BlockPaymentCommand command) {
        log.info("Handling BlockPaymentCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING && this.status != PaymentStatus.AUTHORISED) {
            throw new InvalidPaymentStateException("Payment cannot be blocked from current status: " + this.status);
        }

        String riskLevel = command.riskAssessment().riskLevel();
        Double riskScore = command.riskAssessment().riskScore();
        String reason = String.format("Risk level: %s, Risk score: %s",
                riskLevel,
                riskScore);

        apply(new PaymentBlockedEvent(
                command.paymentId(),
                reason,
                command.riskAssessment().riskScore(),
                command.riskAssessment().marlAssessment() != null ?
                        command.riskAssessment().marlAssessment().maddpgQValue() : null,
                command.riskAssessment()
        ));
    }

    @CommandHandler
    public void handle(RequestManualReviewCommand command) {
        log.info("Handling RequestManualReviewCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        apply(new ManualReviewRequestedEvent(
                command.paymentId(),
                command.riskAssessment().riskScore(),
                command.riskAssessment().marlAssessment() != null ?
                        command.riskAssessment().marlAssessment().maddpgQValue() : null,
                command.riskAssessment()
        ));
    }

    @CommandHandler
    public void handle(ApproveManualReviewCommand command) {
        log.info("Handling ApproveManualReviewCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.MANUAL_REVIEW_REQUIRED) {
            throw new InvalidPaymentStateException("Payment is not in MANUAL_REVIEW_REQUIRED status");
        }

        apply(new ManualReviewApprovedEvent(
                command.paymentId(),
                command.approvedBy(),
                command.approvalNotes()
        ));
    }

    @CommandHandler
    public void handle(RejectManualReviewCommand command) {
        log.info("Handling RejectManualReviewCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.MANUAL_REVIEW_REQUIRED) {
            throw new InvalidPaymentStateException("Payment is not in MANUAL_REVIEW_REQUIRED status");
        }

        apply(new ManualReviewRejectedEvent(
                command.paymentId(),
                command.rejectedBy(),
                command.rejectionReason()
        ));
    }

    @CommandHandler
    public void handle(OverrideDecisionCommand command) {
        log.info("Handling OverrideDecisionCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.BLOCKED) {
            throw new InvalidPaymentStateException("Decision override is only allowed for BLOCKED payments, current status: " + this.status);
        }

        apply(new DecisionOverriddenEvent(
                command.paymentId(),
                command.overriddenBy(),
                command.overrideReason(),
                command.approvePayment(),
                this.status.name()
        ));
    }

    @EventSourcingHandler
    public void on(PaymentInitiatedEvent event) {
        this.paymentId = event.paymentId();
        this.referenceNumber = "PAY-" + event.paymentId().toString().substring(0, 8).toUpperCase(); //TODO: Handle payment reference
        this.customerId = event.customerId();
        this.sourceAccountId = event.sourceAccountId();
        this.destinationAccountId = event.destinationAccountId();
        this.amount = event.amount();
        this.fromCurrency = event.fromCurrency();
        this.toCurrency = event.toCurrency();
        this.convertedAmount = event.convertedAmount();
        this.appliedExchangeRate = event.appliedExchangeRate();
        this.paymentType = event.paymentType();
        this.paymentScheme = event.paymentScheme();
        this.fixedSide = event.fixedSide();
        this.isCrossBorderPayment = event.isCrossBorderPayment();
        this.description = event.description();
        this.status = PaymentStatus.INITIATED;
        this.fraudStatus = FraudAnalysisStatus.PENDING;
        this.initiatedAt = Instant.now();
        log.info("Payment initiated: {}", this.paymentId);
    }

    @EventSourcingHandler
    public void on(RiskAssessmentInitiatedEvent event) {
        this.status = PaymentStatus.FRAUD_CHECK_PENDING;
        this.riskAssessmentRequestedAt = Instant.now();
        log.info("Risk assessment initiated event for payment: {}", event.paymentId());
    }

    @EventSourcingHandler
    public void on(RiskAssessmentCompletedEvent event) {
        this.riskAssessment = event.riskAssessment();
        this.riskAssessmentCompletedAt = Instant.now();
        log.info("Risk assessment completed event for payment: {}, action: {}",
                this.paymentId,
                event.riskAssessment().riskAction());
    }

    @EventSourcingHandler
    public void on(FraudCheckApprovedEvent event) {
        this.status = PaymentStatus.FRAUD_CHECK_APPROVED;
        this.fraudStatus = FraudAnalysisStatus.APPROVED;
        this.fraudCheckApprovedAt = Instant.now();
        this.riskAssessment = event.riskAssessment();
        this.riskAssessmentCompletedAt = Instant.now();
        log.info("Fraud check approved for payment: {}", event.paymentId());

        apply(new LedgerSettlementInitiatedEvent(this.paymentId));
    }

    @EventSourcingHandler
    public void on(DecisionOverriddenEvent event) {
        this.status = event.approvePayment() ? PaymentStatus.OVERRIDE_APPROVED : PaymentStatus.OVERRIDE_REJECTED;
        this.decisionOverriddenAt = Instant.now();
        this.decisionOverriddenBy = event.overriddenBy();
        this.decisionOverrideReason = event.overrideReason();

        log.info("Decision overridden for payment: {} by: {}, approve={}",
                event.paymentId(),
                event.overriddenBy(),
                event.approvePayment());
    }

    @EventSourcingHandler
    public void on(LedgerAuthorisationInitiatedEvent event) {
        this.status = PaymentStatus.AUTHORISATION_PENDING;
        this.ledgerAuthorisationInitiatedAt = Instant.now();
        log.info("Ledger authorisation initiated for payment: {}", event.paymentId());
    }

    @EventSourcingHandler
    public void on(LedgerAuthorisedEvent event) {
        this.status = PaymentStatus.AUTHORISED;
        this.ledgerAuthorisedAt = Instant.now();
        this.authorisationTransferId = event.transferId();
        log.info("Ledger authorised payment: {}, transferId: {}",
                event.paymentId(),
                event.transferId());

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
    public void on(LedgerAuthorisationDeclinedEvent event) {
        this.status = PaymentStatus.AUTHORISATION_DECLINED;
        this.failedAt = Instant.now();
        this.failureReason = event.reason();
        log.warn("Ledger declined authorisation for payment: {}, reason: {}",
                event.paymentId(),
                event.reason());

        apply(new PaymentCompletedEvent(
                this.paymentId,
                PaymentStatus.AUTHORISATION_DECLINED,
                event.reason()
        ));
    }

    @EventSourcingHandler
    public void on(LedgerSettlementInitiatedEvent event) {
        this.status = PaymentStatus.SETTLEMENT_PENDING;
        this.ledgerSettlementInitiatedAt = Instant.now();
        log.info("Ledger settlement initiated for payment: {}", event.paymentId());
    }

    @EventSourcingHandler
    public void on(LedgerSettledEvent event) {
        this.status = PaymentStatus.SETTLED;
        this.ledgerSettledAt = Instant.now();
        this.settlementTransferId = event.transferId();
        log.info("Ledger settled payment: {}, transferId: {}",
                event.paymentId(),
                event.transferId());

        apply(new PaymentCompletedEvent(
                this.paymentId,
                PaymentStatus.COMPLETED,
                "Payment successfully settled on the ledger"
        ));
    }

    @EventSourcingHandler
    public void on(LedgerSettlementFailedEvent event) {
        this.status = PaymentStatus.FAILED;
        this.failedAt = Instant.now();
        this.failureReason = event.reason();
        log.error("Ledger settlement failed for payment: {}, reason: {}",
                event.paymentId(),
                event.reason());

        apply(new PaymentCompletedEvent(
                this.paymentId,
                PaymentStatus.FAILED,
                event.reason()
        ));
    }

    @EventSourcingHandler
    public void on(LedgerReleaseInitiatedEvent event) {
        this.status = PaymentStatus.RELEASE_PENDING;
        this.ledgerReleaseInitiatedAt = Instant.now();
        log.info("Ledger release initiated for payment: {}", event.paymentId());
    }

    @EventSourcingHandler
    public void on(LedgerReleasedEvent event) {
        this.status = PaymentStatus.RELEASED;
        this.ledgerReleasedAt = Instant.now();
        log.info("Ledger released the authorisation for payment: {}", event.paymentId());

        apply(new PaymentCompletedEvent(
                this.paymentId,
                PaymentStatus.BLOCKED,
                this.blockReason
        ));
    }

    @EventSourcingHandler
    public void on(PaymentBlockedEvent event) {
        this.status = PaymentStatus.BLOCKED;
        this.fraudStatus = FraudAnalysisStatus.BLOCKED;
        this.blockedAt = Instant.now();
        this.blockReason = event.reason();
        this.riskAssessmentCompletedAt = Instant.now();
        this.riskAssessment = event.riskAssessment();
        log.info("Payment blocked: {} - Reason: {}",
                event.paymentId(),
                event.reason());

        apply(new LedgerReleaseInitiatedEvent(this.paymentId));
    }

    @EventSourcingHandler
    public void on(ManualReviewRequestedEvent event) {
        this.status = PaymentStatus.MANUAL_REVIEW_REQUIRED;
        this.fraudStatus = FraudAnalysisStatus.REVIEW_REQUIRED;
        this.riskAssessment = event.riskAssessment();
        this.riskAssessmentCompletedAt = Instant.now();
        this.manualReviewRequestedAt = Instant.now();
        log.info("Manual review requested for payment: {}", event.paymentId());
    }

    @EventSourcingHandler
    public void on(ManualReviewApprovedEvent event) {
        this.status = PaymentStatus.FRAUD_CHECK_APPROVED;
        this.fraudStatus = FraudAnalysisStatus.APPROVED;
        this.manualReviewApprovedAt = Instant.now();
        this.manualReviewedBy = event.approvedBy();
        this.manualReviewNotes = event.approvalNotes();
        this.riskAssessmentCompletedAt = Instant.now();
        log.info("Manual review approved for payment: {} by {}",
                event.paymentId(),
                event.approvedBy());

        apply(new LedgerSettlementInitiatedEvent(this.paymentId));
    }

    @EventSourcingHandler
    public void on(ManualReviewRejectedEvent event) {
        this.status = PaymentStatus.BLOCKED;
        this.fraudStatus = FraudAnalysisStatus.BLOCKED;
        this.blockedAt = Instant.now();
        this.manualReviewRejectedAt = Instant.now();
        this.manualReviewedBy = event.rejectedBy();
        this.manualReviewNotes = event.rejectionReason();
        this.blockReason = "Manual review rejected: " + event.rejectionReason();
        log.info("Manual review rejected for payment: {} by: {}, reason: {}",
                event.paymentId(),
                event.rejectedBy(),
                event.rejectionReason());

        apply(new LedgerReleaseInitiatedEvent(this.paymentId));
    }

    @EventSourcingHandler
    public void on(PaymentCompletedEvent event) {
        this.status = event.finalStatus();
        this.completedAt = Instant.now();
        log.info("Payment completed with status: {} - {}",
                event.finalStatus(),
                event.reason());
    }

}
