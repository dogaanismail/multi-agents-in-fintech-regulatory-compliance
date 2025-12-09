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
import org.banksolution.enums.FraudCheckStatus;
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
        snapshotTriggerDefinition = "snapshotTriggerDefinition"
)
@NoArgsConstructor
@Slf4j
public class PaymentAggregate {

    @AggregateIdentifier
    private PaymentId paymentId;

    private UUID externalPaymentId;
    private String referenceNumber;
    private UUID customerId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private String paymentType;
    private String description;

    // Status
    private PaymentStatus status;
    private FraudCheckStatus fraudStatus;

    // Risk Assessment (includes MARL assessment if escalated)
    private RiskAssessment riskAssessment;

    // Lifecycle Timestamps
    private Instant initiatedAt;
    private Instant riskCheckRequestedAt;
    private Instant riskCheckCompletedAt;
    private Instant completedAt;
    private Instant blockedAt;
    private Instant manualReviewRequestedAt;

    // Aggregate version for snapshot tracking
    @AggregateVersion
    private Long version;

    @CommandHandler
    public PaymentAggregate(InitiatePaymentCommand command) {
        log.info("Handling InitiatePaymentCommand for payment: {}", command.paymentId());

        apply(new PaymentInitiatedEvent(
                command.paymentId(),
                command.externalPaymentId(),
                command.referenceNumber(),
                command.customerId(),
                command.sourceAccountId(),
                command.destinationAccountId(),
                command.amount(),
                command.currency(),
                command.paymentType(),
                command.description()
        ));

        apply(new RiskCheckRequestedEvent(
                command.paymentId(),
                command.referenceNumber(),
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

        // Directly complete payment - RiskCheckCompletedEvent already has full assessment
        apply(new PaymentCompletedEvent(command.paymentId()));
    }

    @CommandHandler
    public void handle(BlockPaymentCommand command) {
        log.info("Handling BlockPaymentCommand for payment: {}", command.paymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        String reason = "Risk level: " + command.riskAssessment().getRiskLevel() +
                ", Risk score: " + command.riskAssessment().getRiskScore();

        apply(new PaymentBlockedEvent(
                command.paymentId(),
                reason,
                command.riskAssessment().getRiskScore(),
                command.riskAssessment().getMarlAssessment() != null ?
                        command.riskAssessment().getMarlAssessment().getMaddpgQValue() : null
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
                command.riskAssessment().getRiskScore(),
                command.riskAssessment().getMarlAssessment() != null ?
                        command.riskAssessment().getMarlAssessment().getMaddpgQValue() : null
        ));
    }

    @EventSourcingHandler
    public void on(PaymentInitiatedEvent event) {
        this.paymentId = event.paymentId();
        this.externalPaymentId = event.externalPaymentId();
        this.referenceNumber = event.referenceNumber();
        this.customerId = event.customerId();
        this.sourceAccountId = event.sourceAccountId();
        this.destinationAccountId = event.destinationAccountId();
        this.amount = event.amount();
        this.currency = event.currency();
        this.paymentType = event.paymentType();
        this.description = event.description();
        this.status = PaymentStatus.INITIATED;
        this.fraudStatus = FraudCheckStatus.PENDING;
        this.initiatedAt = Instant.now();
        log.info("Payment initiated: {}", this.paymentId);
    }

    @EventSourcingHandler
    public void on(RiskCheckRequestedEvent event) {
        this.status = PaymentStatus.FRAUD_CHECK_PENDING;
        this.riskCheckRequestedAt = Instant.now();
        log.info("Risk check requested for payment: {}", event.paymentId());
    }

    @EventSourcingHandler
    public void on(RiskCheckCompletedEvent event) {
        this.riskAssessment = event.riskAssessment();
        this.riskCheckCompletedAt = Instant.now();
        log.info("Risk check completed for payment: {}, action: {}",
                this.paymentId, event.riskAssessment().getRiskAction());
    }

    @EventSourcingHandler
    public void on(PaymentBlockedEvent event) {
        this.status = PaymentStatus.BLOCKED;
        this.fraudStatus = FraudCheckStatus.BLOCKED;
        this.blockedAt = Instant.now();
        log.info("Payment blocked: {} - Reason: {}", event.paymentId(), event.reason());
    }

    @EventSourcingHandler
    public void on(ManualReviewRequestedEvent event) {
        this.status = PaymentStatus.MANUAL_REVIEW_REQUIRED;
        this.fraudStatus = FraudCheckStatus.REVIEW_REQUIRED;
        this.manualReviewRequestedAt = Instant.now();
        log.info("Manual review requested for payment: {}", event.paymentId());
    }

    @EventSourcingHandler
    public void on(PaymentCompletedEvent event) {
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = Instant.now();
        log.info("Payment completed: {}", event.paymentId());
    }

}
