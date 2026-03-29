package org.banksolution.infrastructure.messaging.kafka.handler;

import com.aml.account.AccountChargeCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.domain.payment.command.ConfirmAccountChargedCommand;
import org.banksolution.domain.payment.command.FailAccountChargeCommand;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountChargeCompletedEventHandler {

    private final CommandGateway commandGateway;

    public void handle(AccountChargeCompletedEvent event) {
        log.info("Received account charge completed event for payment: {}, success: {}",
                event.getPaymentId(),
                event.getSuccess());

        PaymentId paymentId = new PaymentId(UUID.fromString(event.getPaymentId()));

        try {
            if (event.getSuccess()) {
                UUID sourceAccountId = event.getSourceAccountId() != null ? UUID.fromString(event.getSourceAccountId()) : null;
                UUID destinationAccountId = event.getDestinationAccountId() != null ? UUID.fromString(event.getDestinationAccountId()) : null;
                BigDecimal amount = new BigDecimal(event.getAmount());

                ConfirmAccountChargedCommand command = new ConfirmAccountChargedCommand(
                        paymentId,
                        sourceAccountId,
                        destinationAccountId,
                        amount,
                        event.getFromCurrency(),
                        event.getToCurrency(),
                        event.getPaymentType().toString()
                );

                commandGateway.sendAndWait(command);
                log.info("ConfirmAccountChargedCommand sent for paymentId: {}", paymentId);
            } else {
                log.error("Account charge failed for payment: {}, reason: {}", paymentId, event.getFailureReason());

                FailAccountChargeCommand command = new FailAccountChargeCommand(
                        paymentId,
                        event.getFailureReason()
                );

                commandGateway.sendAndWait(command);
                log.info("FailAccountChargeCommand sent for paymentId: {}", paymentId);
            }
        } catch (Exception ex) {
            // Stale events (e.g. Kafka consumer rewound to offset 0 after container restart)
            // arrive for payments already in a terminal state. Log and skip — do not rethrow
            // so the Kafka consumer commits the offset and continues.
            log.warn("Skipping AccountChargeCompletedEvent for paymentId={} — aggregate rejected command ({}): {}",
                    paymentId, ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
