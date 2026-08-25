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

    public void handle(LedgerPostingCompletedEvent ledgerPostingCompletedEvent) {
        PaymentId paymentId = new PaymentId(UUID.fromString(ledgerPostingCompletedEvent.getClientTransactionId()));

        switch (ledgerPostingCompletedEvent.getPostingInstructionType()) {
            case INBOUND_AUTHORISATION,
                 OUTBOUND_AUTHORISATION,
                 INTERNAL_TRANSFER_AUTHORISATION,
                 CROSS_CURRENCY_TRANSFER_AUTHORISATION -> handleAuthorisation(paymentId, ledgerPostingCompletedEvent);
            case SETTLEMENT -> handleSettlement(paymentId, ledgerPostingCompletedEvent);
            case RELEASE -> handleRelease(paymentId, ledgerPostingCompletedEvent);
            default -> log.debug("Ignoring {} for payment {}, no saga awaits it",
                    ledgerPostingCompletedEvent.getPostingInstructionType(),
                    paymentId);
        }
    }

    private void handleAuthorisation(
            PaymentId paymentId,
            LedgerPostingCompletedEvent ledgerPostingCompletedEvent) {

        if (ledgerPostingCompletedEvent.getSuccess()) {
            commandGateway.sendAndWait(new ConfirmLedgerAuthorisationCommand(paymentId, toTransferId(ledgerPostingCompletedEvent)));
            return;
        }

        commandGateway.sendAndWait(new DeclineLedgerAuthorisationCommand(paymentId, ledgerPostingCompletedEvent.getFailureReason()));
    }

    private void handleSettlement(
            PaymentId paymentId,
            LedgerPostingCompletedEvent ledgerPostingCompletedEvent) {

        if (ledgerPostingCompletedEvent.getSuccess()) {
            commandGateway.sendAndWait(new ConfirmLedgerSettlementCommand(paymentId, toTransferId(ledgerPostingCompletedEvent)));
            return;
        }

        commandGateway.sendAndWait(new FailLedgerSettlementCommand(paymentId, ledgerPostingCompletedEvent.getFailureReason()));
    }

    private void handleRelease(
            PaymentId paymentId,
            LedgerPostingCompletedEvent ledgerPostingCompletedEvent) {

        if (ledgerPostingCompletedEvent.getSuccess()) {
            commandGateway.sendAndWait(new ConfirmLedgerReleaseCommand(paymentId));
            return;
        }

        commandGateway.sendAndWait(new FailLedgerReleaseCommand(paymentId, ledgerPostingCompletedEvent.getFailureReason()));
    }

    private static UUID toTransferId(LedgerPostingCompletedEvent ledgerPostingCompletedEvent) {
        return ledgerPostingCompletedEvent.getTransferId() == null ?
                null :
                UUID.fromString(ledgerPostingCompletedEvent.getTransferId());
    }
}
