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
    private String currency;
    private String paymentType;
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
    private Instant accountChargeInitiatedAt;
    private Instant accountChargedAt;
    private Instant accountChargeFailedAt;
    private Instant completedAt;
    private Instant blockedAt;
    private String blockReason;
    private String failureReason;

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
                command.currency(),
                command.paymentType(),
                command.description()
        ));

        apply(new RiskAssessmentInitiatedEvent(
                command.paymentId(),
                command.customerId(),
                command.sourceAccountId(),
                command.destinationAccountId(),
                command.amount(),
                command.currency(),
                command.paymentType(),
                command.description()
        ));
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

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING && this.status != PaymentStatus.ACCOUNT_CHARGE_PENDING) {
            throw new InvalidPaymentStateException("Payment cannot be blocked from current status: " + this.status);
        }

        String riskLevel = command.riskAssessment().riskLevel();
        Double riskScore = command.riskAssessment().riskScore();
        String reason = String.format("Risk level: %s, Risk score: %s", riskLevel, riskScore);

        apply(new PaymentBlockedEvent(
                command.paymentId(),
                reason,
                command.riskAssessment().riskScore(),
                command.riskAssessment().marlAssessment() != null ?
                        command.riskAssessment().marlAssessment().maddpgQValue() : null,
                command.riskAssessment()
        ));

        apply(new PaymentCompletedEvent(
                command.paymentId(),
                PaymentStatus.BLOCKED,
                reason
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
    public void handle(ChargeAccountCommand command) {
        log.info("Handling ChargeAccountCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_APPROVED) {
            throw new InvalidPaymentStateException("Payment is not in FRAUD_CHECK_APPROVED status");
        }

        apply(new AccountChargeInitiatedEvent(
                command.paymentId(),
                command.customerId(),
                command.sourceAccountId(),
                command.destinationAccountId(),
                command.amount(),
                command.currency(),
                command.paymentType(),
                command.description()
        ));
    }

    @CommandHandler
    public void handle(ConfirmAccountChargedCommand command) {
        log.info("Handling ConfirmAccountChargedCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.ACCOUNT_CHARGE_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in ACCOUNT_CHARGE_PENDING status");
        }

        apply(new AccountChargedEvent(
                command.paymentId(),
                command.sourceAccountId(),
                command.destinationAccountId(),
                command.amount(),
                command.currency(),
                command.paymentType()
        ));

        apply(new PaymentCompletedEvent(
                command.paymentId(),
                PaymentStatus.COMPLETED,
                "Payment successfully processed and account charged"
        ));
    }

    @CommandHandler
    public void handle(FailAccountChargeCommand command) {
        log.error("Handling FailAccountChargeCommand for payment: {}, reason: {}",
                command.paymentId(), command.failureReason());

        if (this.status != PaymentStatus.ACCOUNT_CHARGE_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in ACCOUNT_CHARGE_PENDING status");
        }

        apply(new AccountChargeFailedEvent(
                command.paymentId(),
                command.failureReason()
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
        this.currency = event.currency();
        this.paymentType = event.paymentType();
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
        log.info("Risk assessment completed event for payment: {}, action: {}", this.paymentId, event.riskAssessment().riskAction());
    }

    @EventSourcingHandler
    public void on(FraudCheckApprovedEvent event) {
        this.status = PaymentStatus.FRAUD_CHECK_APPROVED;
        this.fraudStatus = FraudAnalysisStatus.APPROVED;
        this.fraudCheckApprovedAt = Instant.now();
        this.riskAssessment = event.riskAssessment();
        this.riskAssessmentCompletedAt = Instant.now();
        log.info("Fraud check approved for payment: {}", event.paymentId());

        apply(new AccountChargeInitiatedEvent(
                this.paymentId,
                this.customerId,
                this.sourceAccountId,
                this.destinationAccountId,
                this.amount,
                this.currency,
                this.paymentType,
                this.description
        ));
    }

    @EventSourcingHandler
    public void on(AccountChargeInitiatedEvent event) {
        this.status = PaymentStatus.ACCOUNT_CHARGE_PENDING;
        this.accountChargeInitiatedAt = Instant.now();
        log.info("Account charge initiated for payment: {}", event.paymentId());
    }

    @EventSourcingHandler
    public void on(AccountChargedEvent event) {
        this.status = PaymentStatus.ACCOUNT_CHARGED;
        this.accountChargedAt = Instant.now();
        log.info("Account charged for payment: {}", event.paymentId());
    }

    @EventSourcingHandler
    public void on(PaymentBlockedEvent event) {
        this.status = PaymentStatus.BLOCKED;
        this.fraudStatus = FraudAnalysisStatus.BLOCKED;
        this.blockedAt = Instant.now();
        this.blockReason = event.reason();
        this.riskAssessmentCompletedAt = Instant.now();
        this.riskAssessment = event.riskAssessment();
        log.info("Payment blocked: {} - Reason: {}", event.paymentId(), event.reason());
    }

    @EventSourcingHandler
    public void on(ManualReviewRequestedEvent event) {
        this.status = PaymentStatus.MANUAL_REVIEW_REQUIRED;
        this.fraudStatus = FraudAnalysisStatus.REVIEW_REQUIRED;
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
        log.info("Manual review approved for payment: {} by {}", event.paymentId(), event.approvedBy());

        apply(new AccountChargeInitiatedEvent(
                this.paymentId,
                this.customerId,
                this.sourceAccountId,
                this.destinationAccountId,
                this.amount,
                this.currency,
                this.paymentType,
                this.description
        ));
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

        apply(new PaymentCompletedEvent(
                this.paymentId,
                PaymentStatus.BLOCKED,
                "Manual review rejected: " + event.rejectionReason()
        ));
    }

    @EventSourcingHandler
    public void on(AccountChargeFailedEvent event) {
        this.status = PaymentStatus.FAILED;
        this.fraudStatus = FraudAnalysisStatus.APPROVED;
        this.accountChargeFailedAt = Instant.now();
        this.failureReason = event.failureReason();
        log.error("Account charge failed for payment: {}, reason: {}", event.paymentId(), event.failureReason());

        apply(new PaymentCompletedEvent(
                this.paymentId,
                PaymentStatus.FAILED,
                "Account charge failed: " + event.failureReason()
        ));
    }

    @EventSourcingHandler
    public void on(PaymentCompletedEvent event) {
        this.status = event.finalStatus();
        this.completedAt = Instant.now();
        log.info("Payment completed with status: {} - {}", event.finalStatus(), event.reason());
    }

}
