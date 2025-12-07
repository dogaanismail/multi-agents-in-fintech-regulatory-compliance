package org.banksolution.domain.payment.aggregate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.AggregateVersion;
import org.axonframework.spring.stereotype.Aggregate;
import org.banksolution.domain.payment.command.*;
import org.banksolution.enums.FraudCheckStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.domain.payment.event.*;
import org.banksolution.exception.InvalidPaymentStateException;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
        log.info("Handling InitiatePaymentCommand for payment: {}", command.getPaymentId());

        AggregateLifecycle.apply(new PaymentInitiatedEvent(
                command.getPaymentId(),
                command.getExternalPaymentId(),
                command.getReferenceNumber(),
                command.getCustomerId(),
                command.getSourceAccountId(),
                command.getDestinationAccountId(),
                command.getAmount(),
                command.getCurrency(),
                command.getPaymentType(),
                command.getDescription()
        ));

        AggregateLifecycle.apply(new RiskCheckRequestedEvent(
                command.getPaymentId(),
                command.getReferenceNumber(),
                command.getCustomerId(),
                command.getSourceAccountId(),
                command.getDestinationAccountId(),
                command.getAmount(),
                command.getCurrency(),
                command.getPaymentType(),
                command.getDescription()
        ));
    }

    @CommandHandler
    public void handle(ApproveFraudCheckCommand command) {
        log.info("Handling ApproveFraudCheckCommand for payment: {}", command.getPaymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        // Directly complete payment - RiskCheckCompletedEvent already has full assessment
        AggregateLifecycle.apply(new PaymentCompletedEvent(command.getPaymentId()));
    }

    @CommandHandler
    public void handle(BlockPaymentCommand command) {
        log.info("Handling BlockPaymentCommand for payment: {}", command.getPaymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        String reason = "Risk level: " + command.getRiskAssessment().getRiskLevel() +
                ", Risk score: " + command.getRiskAssessment().getRiskScore();

        AggregateLifecycle.apply(new PaymentBlockedEvent(
                command.getPaymentId(),
                reason,
                command.getRiskAssessment().getRiskScore(),
                command.getRiskAssessment().getMarlAssessment() != null ?
                        command.getRiskAssessment().getMarlAssessment().getMaddpgQValue() : null
        ));
    }

    @CommandHandler
    public void handle(RequestManualReviewCommand command) {
        log.info("Handling RequestManualReviewCommand for payment: {}", command.getPaymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new InvalidPaymentStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        AggregateLifecycle.apply(new ManualReviewRequestedEvent(
                command.getPaymentId(),
                command.getRiskAssessment().getRiskScore(),
                command.getRiskAssessment().getMarlAssessment() != null ?
                        command.getRiskAssessment().getMarlAssessment().getMaddpgQValue() : null
        ));
    }

    @EventSourcingHandler
    public void on(PaymentInitiatedEvent event) {
        this.paymentId = event.getPaymentId();
        this.externalPaymentId = event.getExternalPaymentId();
        this.referenceNumber = event.getReferenceNumber();
        this.customerId = event.getCustomerId();
        this.sourceAccountId = event.getSourceAccountId();
        this.destinationAccountId = event.getDestinationAccountId();
        this.amount = event.getAmount();
        this.currency = event.getCurrency();
        this.paymentType = event.getPaymentType();
        this.description = event.getDescription();
        this.status = PaymentStatus.INITIATED;
        this.fraudStatus = FraudCheckStatus.PENDING;
        this.initiatedAt = Instant.now();
        log.info("Payment initiated: {}", this.paymentId);
    }

    @EventSourcingHandler
    public void on(RiskCheckRequestedEvent event) {
        this.status = PaymentStatus.FRAUD_CHECK_PENDING;
        this.riskCheckRequestedAt = Instant.now();
        log.info("Risk check requested for payment: {}", event.getPaymentId());
    }

    @EventSourcingHandler
    public void on(RiskCheckCompletedEvent event) {
        this.riskAssessment = event.getRiskAssessment();
        this.riskCheckCompletedAt = Instant.now();
        log.info("Risk check completed for payment: {}, action: {}",
                this.paymentId, event.getRiskAssessment().getRiskAction());
    }

    @EventSourcingHandler
    public void on(PaymentBlockedEvent event) {
        this.status = PaymentStatus.BLOCKED;
        this.fraudStatus = FraudCheckStatus.BLOCKED;
        this.blockedAt = Instant.now();
        log.info("Payment blocked: {} - Reason: {}", event.getPaymentId(), event.getReason());
    }

    @EventSourcingHandler
    public void on(ManualReviewRequestedEvent event) {
        this.status = PaymentStatus.MANUAL_REVIEW_REQUIRED;
        this.fraudStatus = FraudCheckStatus.REVIEW_REQUIRED;
        this.manualReviewRequestedAt = Instant.now();
        log.info("Manual review requested for payment: {}", event.getPaymentId());
    }

    @EventSourcingHandler
    public void on(PaymentCompletedEvent event) {
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = Instant.now();
        log.info("Payment completed: {}", event.getPaymentId());
    }

}
