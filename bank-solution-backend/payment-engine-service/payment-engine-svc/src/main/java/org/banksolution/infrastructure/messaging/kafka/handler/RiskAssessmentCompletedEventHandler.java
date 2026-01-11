package org.banksolution.infrastructure.messaging.kafka.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.banksolution.domain.payment.event.RiskAssessmentCompletedEvent;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.banksolution.infrastructure.messaging.kafka.mapper.RiskAssessmentMapper.toRiskAssessment;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentCompletedEventHandler {

    private final EventGateway eventGateway;

    public void handle(com.aml.risk.RiskAssessmentCompletedEvent event) {
        log.info("Received risk assessment completed event for payment: {}, riskCheckRequestId: {}. action: {}",
                event.getPaymentId(),
                event.getRiskCheckRequestId(),
                event.getAction());

        PaymentId paymentId = new PaymentId(UUID.fromString(event.getPaymentId()));
        RiskAssessment riskAssessment = toRiskAssessment(event);

        eventGateway.publish(new RiskAssessmentCompletedEvent(paymentId, riskAssessment));

        log.info("RiskAssessmentCompletedEvent published for paymentId: {}, saga will handle workflow", paymentId);
    }
}
