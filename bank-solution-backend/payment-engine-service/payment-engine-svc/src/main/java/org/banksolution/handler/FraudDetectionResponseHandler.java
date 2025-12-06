package org.banksolution.handler;

import com.aml.fraud.FraudDetectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.command.ApproveFraudCheckCommand;
import org.banksolution.command.BlockPaymentCommand;
import org.banksolution.command.RequestManualReviewCommand;
import org.banksolution.enums.FraudAction;
import org.banksolution.repository.PaymentIdMappingRepository;
import org.banksolution.valueobject.PaymentId;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionResponseHandler {

    private final CommandGateway commandGateway;
    private final PaymentIdMappingRepository paymentIdMappingRepository;

    public void handle(FraudDetectionResponse response) {
        log.info("Handling FraudDetectionResponse: requestId:{}, transactionId:{}, action:{}",
                response.getRequestId(),
                response.getTransactionId(),
                response.getAction());

        PaymentId paymentId = paymentIdMappingRepository
                .findPaymentIdByReferenceNumber(response.getTransactionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found for transaction: " + response.getTransactionId()));

        FraudAction action = FraudAction.valueOf(response.getAction().name());
        Double confidence = response.getConfidence();
        Double maddpgQValue = response.getMaddpgQValue();

        switch (action) {
            case ALLOW:
                commandGateway.sendAndWait(new ApproveFraudCheckCommand(
                        paymentId,
                        confidence,
                        maddpgQValue
                ));
                log.info("Fraud check approved for payment: {}", paymentId);
                break;

            case BLOCK:
                commandGateway.sendAndWait(new BlockPaymentCommand(
                        paymentId,
                        "Fraud detected by MARL Orchestrator",
                        confidence,
                        maddpgQValue
                ));
                log.info("Payment blocked for payment: {}", paymentId);
                break;

            case REVIEW:
                commandGateway.sendAndWait(new RequestManualReviewCommand(
                        paymentId,
                        confidence,
                        maddpgQValue
                ));
                log.info("Manual review requested for payment: {}", paymentId);
                break;

            default:
                log.warn("Unknown fraud action: {}", action);
        }
    }
}
