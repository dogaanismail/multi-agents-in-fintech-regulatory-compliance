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
import org.banksolution.domain.payment.command.ApproveFraudCheckCommand;
import org.banksolution.domain.payment.command.BlockPaymentCommand;
import org.banksolution.domain.payment.command.ExpireRiskAssessmentCommand;
import org.banksolution.domain.payment.command.RequestManualReviewCommand;
import org.banksolution.domain.payment.event.*;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.infrastructure.messaging.kafka.producer.RiskAssessmentRequestedEventProducer;

import java.time.Duration;

@Saga(sagaStore = "sagaStore")
@Slf4j
public class PaymentRiskSaga {

    private static final String PAYMENT_ID_ASSOCIATION = "paymentId";
    private static final String RISK_ASSESSMENT_TIMEOUT_DEADLINE = "risk-assessment-timeout";
    private static final Duration RISK_CHECK_TIMEOUT = Duration.ofMinutes(1);

    private PaymentId paymentId;
    private String deadlineId;
    private boolean riskAssessmentCompleted = false;

    @StartSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(RiskAssessmentInitiatedEvent riskAssessmentInitiatedEvent,
                   DeadlineManager deadlineManager,
                   RiskAssessmentRequestedEventProducer riskAssessmentRequestedEventProducer) {
        log.info("Risk check started for payment id {}", riskAssessmentInitiatedEvent.paymentId());

        this.paymentId = riskAssessmentInitiatedEvent.paymentId();
        this.riskAssessmentCompleted = false;

        log.info("Publishing RiskCheckRequest to Kafka for payment: {}", this.paymentId);
        riskAssessmentRequestedEventProducer.publishRiskAssessmentRequestedEvent(riskAssessmentInitiatedEvent);

        this.deadlineId = deadlineManager.schedule(RISK_CHECK_TIMEOUT, RISK_ASSESSMENT_TIMEOUT_DEADLINE, this.paymentId);
        log.info("Scheduled risk check timeout deadline for payment: {} with deadlineId: {}", this.paymentId, this.deadlineId);

        log.info("Saga setup complete, awaiting risk check completion or timeout");
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(RiskAssessmentCompletedEvent riskAssessmentCompletedEvent,
                   DeadlineManager deadlineManager,
                   CommandGateway commandGateway) {
        log.info("Risk check completed for payment id {}", riskAssessmentCompletedEvent.paymentId());

        if (!riskAssessmentCompleted) {
            deadlineManager.cancelSchedule(RISK_ASSESSMENT_TIMEOUT_DEADLINE, deadlineId);
            log.info("Cancelled risk check timeout deadline for payment: {}", riskAssessmentCompletedEvent.paymentId());
        }

        this.riskAssessmentCompleted = true;

        RiskAssessment riskAssessment = riskAssessmentCompletedEvent.riskAssessment();

        if (riskAssessment == null) {
            log.error("Risk assessment is null for payment: {}, ending saga", riskAssessmentCompletedEvent.paymentId());
            SagaLifecycle.end();
            return;
        }

        String riskAction = riskAssessment.riskAction();

        try {
            switch (riskAction) {
                case "PROCEED" -> {
                    log.info("Risk action: PROCEED - Approving payment: {}", riskAssessmentCompletedEvent.paymentId());
                    commandGateway.sendAndWait(
                            new ApproveFraudCheckCommand(riskAssessmentCompletedEvent.paymentId(), riskAssessment));
                }
                case "ESCALATE" -> {
                    log.info("Risk action: ESCALATE - Requesting manual review for payment: {}",
                            riskAssessmentCompletedEvent.paymentId());
                    commandGateway.sendAndWait(
                            new RequestManualReviewCommand(riskAssessmentCompletedEvent.paymentId(), riskAssessment));
                }
                case "BLOCK" -> {
                    log.info("Risk action: BLOCK - Blocking payment: {}", riskAssessmentCompletedEvent.paymentId());
                    commandGateway.sendAndWait(
                            new BlockPaymentCommand(riskAssessmentCompletedEvent.paymentId(), riskAssessment));
                }
                default -> {
                    log.warn("Unknown risk action: {} for payment: {}, ending saga",
                            riskAction, riskAssessmentCompletedEvent.paymentId());
                    SagaLifecycle.end();
                }
            }
        } catch (Exception exception) {
            log.error("Error processing risk action for payment: {}", riskAssessmentCompletedEvent.paymentId(), exception);
            SagaLifecycle.end();
        }
    }

    @EndSaga
    @DeadlineHandler(deadlineName = RISK_ASSESSMENT_TIMEOUT_DEADLINE)
    public void on(PaymentId timedOutPaymentId, CommandGateway commandGateway) {
        log.error("Risk check timed out for payment: {}, expiring the assessment to release the held funds",
                timedOutPaymentId);

        try {
            commandGateway.sendAndWait(new ExpireRiskAssessmentCommand(timedOutPaymentId));
        } catch (Exception exception) {
            log.error("Failed to expire the risk assessment for payment: {}", timedOutPaymentId, exception);
        }
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(FraudCheckApprovedEvent fraudCheckApprovedEvent) {
        log.info("Fraud check approved, ending PaymentRiskSaga for payment: {}", fraudCheckApprovedEvent.paymentId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(PaymentCompletedEvent paymentCompletedEvent) {
        log.info("Payment completed, ending PaymentRiskSaga for payment: {}", paymentCompletedEvent.paymentId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(PaymentBlockedEvent paymentBlockedEvent) {
        log.info("Payment blocked, ending PaymentRiskSaga for payment: {}", paymentBlockedEvent.paymentId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(ManualReviewRequestedEvent manualReviewRequestedEvent) {
        log.info("Manual review requested, ending PaymentRiskSaga for payment: {}", manualReviewRequestedEvent.paymentId());
    }

}
