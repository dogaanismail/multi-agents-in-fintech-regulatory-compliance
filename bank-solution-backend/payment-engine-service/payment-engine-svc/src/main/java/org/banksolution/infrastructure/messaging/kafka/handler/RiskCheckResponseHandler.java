package org.banksolution.infrastructure.messaging.kafka.handler;

import com.aml.risk.RiskAction;
import com.aml.risk.RiskCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.banksolution.domain.payment.command.ApproveFraudCheckCommand;
import org.banksolution.domain.payment.command.BlockPaymentCommand;
import org.banksolution.domain.payment.command.RequestManualReviewCommand;
import org.banksolution.domain.payment.event.RiskCheckCompletedEvent;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.exception.PaymentNotFoundException;
import org.banksolution.infrastructure.messaging.kafka.mapper.RiskAssessmentMapper;
import org.banksolution.infrastructure.persistence.repository.PaymentIdMappingRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskCheckResponseHandler {

    private final CommandGateway commandGateway;
    private final EventGateway eventGateway;
    private final PaymentIdMappingRepository paymentIdMappingRepository;

    public void handle(RiskCheckResponse response) {

        PaymentId paymentId = paymentIdMappingRepository.findPaymentIdByReferenceNumber(response.getReferenceNumber())
                .orElseThrow(() -> new PaymentNotFoundException("reference: " + response.getReferenceNumber()));

        log.info("Risk check completed for payment: {}, action: {}",
                paymentId,
                response.getAction());

        RiskAssessment riskAssessment = RiskAssessmentMapper.toRiskAssessment(response);

        // Emit RiskCheckCompletedEvent (stores risk assessment in aggregate)
        eventGateway.publish(new RiskCheckCompletedEvent(paymentId, riskAssessment));

        // Execute appropriate command based on risk action
        switch (response.getAction()) {
            case RiskAction.PROCEED:
                commandGateway.sendAndWait(new ApproveFraudCheckCommand(paymentId, riskAssessment));
                log.info("Risk check passed, payment approved: {}", paymentId);
                break;

            case RiskAction.ESCALATE:
                commandGateway.sendAndWait(new RequestManualReviewCommand(paymentId, riskAssessment));
                log.info("Risk check requires manual review for payment: {}", paymentId);
                break;

            case RiskAction.BLOCK:
                commandGateway.sendAndWait(new BlockPaymentCommand(paymentId, riskAssessment));
                log.info("Risk check blocked payment: {}", paymentId);
                break;

            default:
                log.warn("Unknown risk action: {} for payment: {}", response.getAction(), paymentId);
        }
    }
}
