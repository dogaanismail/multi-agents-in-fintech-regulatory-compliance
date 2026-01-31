package org.banksolution.domain.payment.saga;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.banksolution.domain.payment.command.ConfirmAccountChargedCommand;
import org.banksolution.domain.payment.command.FailAccountChargeCommand;
import org.banksolution.domain.payment.event.*;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.infrastructure.messaging.kafka.producer.AccountChargeRequestedEventProducer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Saga(sagaStore = "sagaStore")
@Slf4j
public class AccountChargeSaga {

    private static final String PAYMENT_ID_ASSOCIATION = "paymentId";
    private static final String ACCOUNT_CHARGE_TIMEOUT_DEADLINE = "account-charge-timeout";
    private static final Duration ACCOUNT_CHARGE_TIMEOUT = Duration.ofMinutes(2);

    private PaymentId paymentId;
    private UUID customerId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private String paymentType;
    private String description;
    private String deadlineId;
    private boolean accountChargeCompleted = false;

    @StartSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(AccountChargeInitiatedEvent event,
                   DeadlineManager deadlineManager,
                   AccountChargeRequestedEventProducer accountChargeRequestedEventProducer) {
        log.info("Account charge initiated for payment id {}, sending request to account-service", event.paymentId());

        this.paymentId = event.paymentId();
        this.customerId = event.customerId();
        this.sourceAccountId = event.sourceAccountId();
        this.destinationAccountId = event.destinationAccountId();
        this.amount = event.amount();
        this.currency = event.currency();
        this.paymentType = event.paymentType();
        this.description = event.description();
        this.accountChargeCompleted = false;

        try {
            log.info("Scheduling account charge timeout deadline for payment: {}", event.paymentId());
            this.deadlineId = deadlineManager.schedule(ACCOUNT_CHARGE_TIMEOUT, ACCOUNT_CHARGE_TIMEOUT_DEADLINE, event.paymentId());

            log.info("Publishing AccountChargeRequest to Kafka for payment: {}", event.paymentId());
            accountChargeRequestedEventProducer.publishAccountChargeRequestedEvent(event);

            log.info("Account charge request published, awaiting confirmation for payment: {}", event.paymentId());
        } catch (Exception e) {
            log.error("Error initiating account charge for payment: {}", event.paymentId(), e);
            SagaLifecycle.end();
        }
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(AccountChargedEvent event,
                   DeadlineManager deadlineManager,
                   CommandGateway commandGateway) {
        log.info("Account charged successfully for payment id {}", event.paymentId());

        if (deadlineId != null && !accountChargeCompleted) {
            deadlineManager.cancelSchedule(ACCOUNT_CHARGE_TIMEOUT_DEADLINE, deadlineId);
            log.info("Cancelled account charge timeout deadline for payment: {}", event.paymentId());
        }

        this.accountChargeCompleted = true;
        try {
            commandGateway.sendAndWait(new ConfirmAccountChargedCommand(
                    event.paymentId(),
                    event.sourceAccountId(),
                    event.destinationAccountId(),
                    event.amount(),
                    event.currency(),
                    event.paymentType()
            ));
            log.info("Payment completion command sent for payment: {}", event.paymentId());
        } catch (Exception e) {
            log.error("Error confirming account charge for payment: {}", event.paymentId(), e);
            SagaLifecycle.end();
        }
    }

    @DeadlineHandler(deadlineName = ACCOUNT_CHARGE_TIMEOUT_DEADLINE)
    public void on(PaymentId paymentId, CommandGateway commandGateway) {
        log.warn("Account charge timeout for payment: {}, failing the payment", paymentId);

        try {
            String failureReason = String.format("Account charge timeout after minutes: %s", ACCOUNT_CHARGE_TIMEOUT.toMinutes());
            commandGateway.sendAndWait(new FailAccountChargeCommand(paymentId, failureReason));
            log.info("FailAccountChargeCommand sent due to timeout for payment: {}", paymentId);
        } catch (Exception e) {
            log.error("Error handling account charge timeout for payment: {}", paymentId, e);
        }
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(PaymentCompletedEvent event) {
        log.info("Payment completed, ending AccountChargeSaga for payment: {}", event.paymentId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(PaymentBlockedEvent event) {
        log.info("Payment blocked during account charge, ending AccountChargeSaga for payment: {}", event.paymentId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(AccountChargeFailedEvent event) {
        log.info("Account charge failed, ending AccountChargeSaga for payment: {}", event.paymentId());
    }

}
