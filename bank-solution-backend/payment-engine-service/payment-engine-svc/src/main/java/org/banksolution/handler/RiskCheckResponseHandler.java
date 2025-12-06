package org.banksolution.handler;

import com.aml.risk.RiskCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.command.ApproveFraudCheckCommand;
import org.banksolution.command.BlockPaymentCommand;
import org.banksolution.repository.PaymentIdMappingRepository;
import org.banksolution.valueobject.PaymentId;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskCheckResponseHandler {

    private final CommandGateway commandGateway;
    private final PaymentIdMappingRepository paymentIdMappingRepository;

    public void handle(RiskCheckResponse response) {
        PaymentId paymentId = paymentIdMappingRepository
                .findPaymentIdByReferenceNumber(response.getReferenceNumber())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found for reference: " + response.getReferenceNumber()));

        String action = response.getAction().toString();
        Double riskScore = response.getRiskScore();

        log.info("Risk check completed for payment: {}, action: {}, riskScore: {}",
                paymentId, action, riskScore);

        switch (action) {
            case "PROCEED":
                commandGateway.sendAndWait(new ApproveFraudCheckCommand(
                        paymentId,
                        riskScore,
                        null
                ));
                log.info("Risk check passed, payment approved: {}", paymentId);
                break;

            case "ESCALATE":
                log.info("Risk check requires MARL escalation for payment: {}", paymentId);
                break;

            case "BLOCK":
                commandGateway.sendAndWait(new BlockPaymentCommand(
                        paymentId,
                        "Risk check blocked payment - risk score: " + riskScore,
                        riskScore,
                        null
                ));
                log.info("Risk check blocked payment: {}", paymentId);
                break;

            default:
                log.warn("Unknown risk action: {} for payment: {}", action, paymentId);
        }
    }
}
