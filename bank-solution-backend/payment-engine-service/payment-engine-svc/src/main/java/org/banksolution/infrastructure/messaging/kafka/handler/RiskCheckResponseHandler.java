package org.banksolution.infrastructure.messaging.kafka.handler;

import com.aml.risk.RiskCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.banksolution.domain.payment.event.RiskCheckCompletedEvent;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.banksolution.infrastructure.messaging.kafka.mapper.RiskAssessmentMapper.toRiskAssessment;

/**
 * Handler for Kafka risk check responses.
 * <p>
 * This handler publishes the RiskCheckCompletedEvent using the paymentId from the response.
 * The PaymentRiskSaga will handle the orchestration and command dispatching
 * based on the risk action, following proper Axon Saga patterns.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RiskCheckResponseHandler {

    private final EventGateway eventGateway;

    public void handle(RiskCheckResponse response) {
        log.info("Risk check response received for payment: {}, action: {}", response.getPaymentId(), response.getAction());

        PaymentId paymentId = new PaymentId(UUID.fromString(response.getPaymentId()));
        RiskAssessment riskAssessment = toRiskAssessment(response);

        eventGateway.publish(new RiskCheckCompletedEvent(paymentId, riskAssessment));

        log.info("RiskCheckCompletedEvent published for payment: {}, saga will handle workflow", paymentId);
    }
}
