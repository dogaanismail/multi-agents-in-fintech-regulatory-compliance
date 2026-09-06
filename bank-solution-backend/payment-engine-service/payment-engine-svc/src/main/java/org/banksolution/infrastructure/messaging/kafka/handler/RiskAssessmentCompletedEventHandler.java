package org.banksolution.infrastructure.messaging.kafka.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.domain.payment.command.CompleteRiskAssessmentCommand;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.banksolution.infrastructure.messaging.kafka.mapper.RiskAssessmentMapper.toRiskAssessment;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentCompletedEventHandler {

    private final CommandGateway commandGateway;

    public void handle(com.aml.risk.RiskAssessmentCompletedEvent riskAssessmentCompletedAvroEvent) {
        log.info("Received risk assessment completed riskAssessmentCompletedAvroEvent for payment: {}, riskCheckRequestId: {}. action: {}",
                riskAssessmentCompletedAvroEvent.getPaymentId(),
                riskAssessmentCompletedAvroEvent.getRiskCheckRequestId(),
                riskAssessmentCompletedAvroEvent.getAction());

        PaymentId paymentId = new PaymentId(UUID.fromString(riskAssessmentCompletedAvroEvent.getPaymentId()));
        RiskAssessment riskAssessment = toRiskAssessment(riskAssessmentCompletedAvroEvent);

        // sendAndWait so a rejected command fails the Kafka record (retry, then DLT); the
        // aggregate itself ignores redelivered and late completions, so those are acknowledged.
        commandGateway.sendAndWait(new CompleteRiskAssessmentCommand(paymentId, riskAssessment));

        log.info("Risk assessment completion recorded for paymentId: {}, saga will handle workflow", paymentId);
    }
}
