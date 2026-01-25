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
    public void on(RiskAssessmentInitiatedEvent event,
                   DeadlineManager deadlineManager,
                   RiskAssessmentRequestedEventProducer riskAssessmentRequestedEventProducer) {
        log.info("Risk check started for payment id {}", event.paymentId());

        this.paymentId = event.paymentId();
        this.riskAssessmentCompleted = false;

        log.info("Publishing RiskCheckRequest to Kafka for payment: {}", this.paymentId);
        riskAssessmentRequestedEventProducer.publishRiskAssessmentRequestedEvent(event);

        this.deadlineId = deadlineManager.schedule(RISK_CHECK_TIMEOUT, RISK_ASSESSMENT_TIMEOUT_DEADLINE, this.paymentId);
        log.info("Scheduled risk check timeout deadline for payment: {} with deadlineId: {}", this.paymentId, this.deadlineId);

        log.info("Saga setup complete, awaiting risk check completion or timeout");
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(RiskAssessmentCompletedEvent event,
                   DeadlineManager deadlineManager,
                   CommandGateway commandGateway) {
        log.info("Risk check completed for payment id {}", event.paymentId());

        if (deadlineId != null && !riskAssessmentCompleted) {
            deadlineManager.cancelSchedule(RISK_ASSESSMENT_TIMEOUT_DEADLINE, deadlineId);
            log.info("Cancelled risk check timeout deadline for payment: {}", paymentId);
        }

        this.riskAssessmentCompleted = true;

        RiskAssessment riskAssessment = event.riskAssessment();

        if (riskAssessment == null) {
            log.error("Risk assessment is null for payment: {}, ending saga", paymentId);
            SagaLifecycle.end();
            return;
        }

        String riskAction = riskAssessment.riskAction();

        try {
            switch (riskAction) {
                case "PROCEED" -> {
                    log.info("Risk action: PROCEED - Approving payment: {}", paymentId);
                    commandGateway.sendAndWait(new ApproveFraudCheckCommand(paymentId, riskAssessment));
                }
                case "ESCALATE" -> {
                    log.info("Risk action: ESCALATE - Requesting manual review for payment: {}", paymentId);
                    commandGateway.sendAndWait(new RequestManualReviewCommand(paymentId, riskAssessment));
                }
                case "BLOCK" -> {
                    log.info("Risk action: BLOCK - Blocking payment: {}", paymentId);
                    commandGateway.sendAndWait(new BlockPaymentCommand(paymentId, riskAssessment));
                }
                default -> {
                    log.warn("Unknown risk action: {} for payment: {}, ending saga", riskAction, paymentId);
                    SagaLifecycle.end();
                }
            }
        } catch (Exception e) {
            log.error("Error processing risk action for payment: {}", paymentId, e);
            SagaLifecycle.end();
        }
    }

    @EndSaga
    @DeadlineHandler(deadlineName = RISK_ASSESSMENT_TIMEOUT_DEADLINE)
    public void on(PaymentId paymentId) {
        log.info("Risk check timeout for payment: {}", paymentId);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(FraudCheckApprovedEvent event) {
        log.info("Fraud check approved, ending PaymentRiskSaga for payment: {}", event.paymentId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(PaymentCompletedEvent event) {
        log.info("Payment completed, ending PaymentRiskSaga for payment: {}", event.paymentId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(PaymentBlockedEvent event) {
        log.info("Payment blocked, ending PaymentRiskSaga for payment: {}", event.paymentId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(ManualReviewRequestedEvent event) {
        log.info("Manual review requested, ending PaymentRiskSaga for payment: {}", event.paymentId());
    }

}
