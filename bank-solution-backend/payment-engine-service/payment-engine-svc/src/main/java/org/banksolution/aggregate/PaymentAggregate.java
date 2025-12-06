package org.banksolution.aggregate;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.banksolution.command.*;
import org.banksolution.enums.FraudCheckStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.event.*;
import org.banksolution.valueobject.PaymentId;

import java.math.BigDecimal;
import java.util.UUID;

@Aggregate
@NoArgsConstructor
@Slf4j
public class PaymentAggregate {

    @AggregateIdentifier
    private PaymentId paymentId;
    
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private UUID externalPaymentId;
    
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private String referenceNumber;
    
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private UUID customerId;
    
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private UUID sourceAccountId;
    
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private UUID destinationAccountId;
    
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private BigDecimal amount;
    
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private String currency;
    
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private String paymentType;
    
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private String description;
    
    private PaymentStatus status;
    private FraudCheckStatus fraudStatus;
    private Double confidence;
    private Double maddpgQValue;

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
            throw new IllegalStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        AggregateLifecycle.apply(new FraudCheckApprovedEvent(
                command.getPaymentId(),
                command.getConfidence(),
                command.getMaddpgQValue()
        ));

        AggregateLifecycle.apply(new PaymentCompletedEvent(
                command.getPaymentId()
        ));
    }

    @CommandHandler
    public void handle(BlockPaymentCommand command) {
        log.info("Handling BlockPaymentCommand for payment: {}", command.getPaymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new IllegalStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        AggregateLifecycle.apply(new PaymentBlockedEvent(
                command.getPaymentId(),
                command.getReason(),
                command.getConfidence(),
                command.getMaddpgQValue()
        ));
    }

    @CommandHandler
    public void handle(RequestManualReviewCommand command) {
        log.info("Handling RequestManualReviewCommand for payment: {}", command.getPaymentId());

        if (this.status != PaymentStatus.FRAUD_CHECK_PENDING) {
            throw new IllegalStateException("Payment is not in FRAUD_CHECK_PENDING status");
        }

        AggregateLifecycle.apply(new ManualReviewRequestedEvent(
                command.getPaymentId(),
                command.getConfidence(),
                command.getMaddpgQValue()
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
        log.info("Payment initiated: {}", this.paymentId);
    }

    @EventSourcingHandler
    public void on(RiskCheckRequestedEvent event) {
        this.status = PaymentStatus.FRAUD_CHECK_PENDING;
        log.info("Risk check requested for payment: {}", event.getPaymentId());
    }

    @EventSourcingHandler
    public void on(FraudCheckApprovedEvent event) {
        this.status = PaymentStatus.FRAUD_CHECK_APPROVED;
        this.fraudStatus = FraudCheckStatus.APPROVED;
        this.confidence = event.getConfidence();
        this.maddpgQValue = event.getMaddpgQValue();
        log.info("Fraud check approved for payment: {}", event.getPaymentId());
    }

    @EventSourcingHandler
    public void on(PaymentBlockedEvent event) {
        this.status = PaymentStatus.BLOCKED;
        this.fraudStatus = FraudCheckStatus.BLOCKED;
        this.confidence = event.getConfidence();
        this.maddpgQValue = event.getMaddpgQValue();
        log.info("Payment blocked: {} - Reason: {}", event.getPaymentId(), event.getReason());
    }

    @EventSourcingHandler
    public void on(ManualReviewRequestedEvent event) {
        this.status = PaymentStatus.MANUAL_REVIEW_REQUIRED;
        this.fraudStatus = FraudCheckStatus.REVIEW_REQUIRED;
        this.confidence = event.getConfidence();
        this.maddpgQValue = event.getMaddpgQValue();
        log.info("Manual review requested for payment: {}", event.getPaymentId());
    }

    @EventSourcingHandler
    public void on(PaymentCompletedEvent event) {
        this.status = PaymentStatus.COMPLETED;
        log.info("Payment completed: {}", event.getPaymentId());
    }
}
