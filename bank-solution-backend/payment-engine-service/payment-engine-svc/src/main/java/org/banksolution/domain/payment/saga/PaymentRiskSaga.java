package org.banksolution.domain.payment.saga;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.banksolution.domain.payment.command.ApproveFraudCheckCommand;
import org.banksolution.domain.payment.command.BlockPaymentCommand;
import org.banksolution.domain.payment.command.RequestManualReviewCommand;
import org.banksolution.domain.payment.event.PaymentBlockedEvent;
import org.banksolution.domain.payment.event.PaymentCompletedEvent;
import org.banksolution.domain.payment.event.RiskCheckCompletedEvent;
import org.banksolution.domain.payment.event.RiskCheckRequestedEvent;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.infrastructure.messaging.kafka.producer.RiskCheckRequestProducer;

import static org.axonframework.modelling.saga.SagaLifecycle.associateWith;

/**
 * Saga that orchestrates the payment risk check workflow.
 * <p>
 * This saga:
 * 1. Starts when the RiskCheckRequestedEvent is published
 * 2. Publishes RiskCheckRequest to Kafka for risk-engine processing
 * 3. Waits for RiskCheckCompletedEvent (triggered by Kafka response)
 * 4. Dispatches the appropriate command based on risk action (Approve/Block/Review)
 * 5. Ends when payment reaches a terminal state (Completed, Blocked, or Manual Review)
 * <p>
 * This replaces the scattered "Process Manager" logic previously in RiskCheckResponseHandler.
 */
@Saga
@Slf4j
public class PaymentRiskSaga {

    private static final String PAYMENT_ID_ASSOCIATION = "paymentId";

    private final CommandGateway commandGateway;
    private final RiskCheckRequestProducer riskCheckRequestProducer;
    private PaymentId paymentId;

    public PaymentRiskSaga(
            CommandGateway commandGateway,
            RiskCheckRequestProducer riskCheckRequestProducer) {
        this.commandGateway = commandGateway;
        this.riskCheckRequestProducer = riskCheckRequestProducer;
    }

    @StartSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(RiskCheckRequestedEvent event) {
        log.info("PaymentRiskSaga started for payment: {}, reference: {}",
                event.getPaymentId(),
                event.getReferenceNumber());

        this.paymentId = event.getPaymentId();

        // Associate saga with payment ID for future events
        associateWith(PAYMENT_ID_ASSOCIATION, paymentId.toString());

        // Publish a risk check request to Kafka for risk-engine processing
        log.info("Publishing RiskCheckRequest to Kafka for payment: {}", paymentId);
        riskCheckRequestProducer.publishRiskCheckRequest(event);

        log.info("Saga associated with paymentId: {}, awaiting risk check completion", paymentId);
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(RiskCheckCompletedEvent event) {
        log.info("Risk check completed for payment: {}, processing action", paymentId);

        RiskAssessment riskAssessment = event.getRiskAssessment();

        if (riskAssessment == null) {
            log.error("Risk assessment is null for payment: {}, ending saga", paymentId);
            SagaLifecycle.end();
            return;
        }

        // The dispatch appropriate command based on risk action
        String riskAction = riskAssessment.getRiskAction();

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
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(PaymentCompletedEvent event) {
        log.info("Payment completed, ending PaymentRiskSaga for payment: {}", event.getPaymentId());
    }

    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(PaymentBlockedEvent event) {
        log.info("Payment blocked, ending PaymentRiskSaga for payment: {}", event.getPaymentId());
    }

    // Note: ManualReviewRequestedEvent also ends the saga as the payment is now in a terminal state
    // requiring human intervention
    @EndSaga
    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(org.banksolution.domain.payment.event.ManualReviewRequestedEvent event) {
        log.info("Manual review requested, ending PaymentRiskSaga for payment: {}", event.getPaymentId());
    }
}
