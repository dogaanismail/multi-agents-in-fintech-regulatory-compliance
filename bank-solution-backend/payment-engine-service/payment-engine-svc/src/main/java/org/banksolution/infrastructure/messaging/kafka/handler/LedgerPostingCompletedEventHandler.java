package org.banksolution.infrastructure.messaging.kafka.handler;

import com.aml.ledger.LedgerPostingCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.domain.payment.command.*;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingCompletedEventHandler {

    private final CommandGateway commandGateway;

    public void handle(LedgerPostingCompletedEvent event) {
        PaymentId paymentId = new PaymentId(UUID.fromString(event.getClientTransactionId()));

        switch (event.getPostingInstructionType()) {
            case INBOUND_AUTHORISATION, OUTBOUND_AUTHORISATION, INTERNAL_TRANSFER_AUTHORISATION ->
                    handleAuthorisation(paymentId, event);
            case SETTLEMENT -> handleSettlement(paymentId, event);
            case RELEASE -> handleRelease(paymentId, event);
            default -> log.warn("Ignoring unexpected posting instruction type {} for payment {}",
                    event.getPostingInstructionType(),
                    paymentId);
        }
    }

    private void handleAuthorisation(
            PaymentId paymentId,
            LedgerPostingCompletedEvent event) {

        if (event.getSuccess()) {
            commandGateway.sendAndWait(new ConfirmLedgerAuthorisationCommand(paymentId, toTransferId(event)));
            return;
        }

        commandGateway.sendAndWait(new DeclineLedgerAuthorisationCommand(paymentId, event.getFailureReason()));
    }

    private void handleSettlement(
            PaymentId paymentId,
            LedgerPostingCompletedEvent event) {

        if (event.getSuccess()) {
            commandGateway.sendAndWait(new ConfirmLedgerSettlementCommand(paymentId, toTransferId(event)));
            return;
        }

        commandGateway.sendAndWait(new FailLedgerSettlementCommand(paymentId, event.getFailureReason()));
    }

    private void handleRelease(
            PaymentId paymentId,
            LedgerPostingCompletedEvent event) {

        if (event.getSuccess()) {
            commandGateway.sendAndWait(new ConfirmLedgerReleaseCommand(paymentId));
            return;
        }

        log.error("Ledger release failed for payment {}: {}", paymentId, event.getFailureReason());
    }

    private static UUID toTransferId(LedgerPostingCompletedEvent event) {
        return event.getTransferId() == null ?
                null :
                UUID.fromString(event.getTransferId());
    }
}
