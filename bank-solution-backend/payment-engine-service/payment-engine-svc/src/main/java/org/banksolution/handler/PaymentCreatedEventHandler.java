package org.banksolution.handler;

import com.aml.payment.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.command.InitiatePaymentCommand;
import org.banksolution.valueobject.PaymentId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreatedEventHandler {

    private final CommandGateway commandGateway;

    public void handle(PaymentCreatedEvent event) {
        log.info("Handling PaymentCreatedEvent: eventId:{}, paymentId:{}",
                event.getEventId(),
                event.getPaymentId());

        PaymentId paymentId = new PaymentId();
        UUID externalPaymentId = UUID.fromString(event.getPaymentId());
        UUID sourceAccountId = event.getSourceAccountId() != null ?
                UUID.fromString(event.getSourceAccountId()) : null;
        UUID destinationAccountId = event.getDestinationAccountId() != null ?
                UUID.fromString(event.getDestinationAccountId()) : null;

        InitiatePaymentCommand command = new InitiatePaymentCommand(
                paymentId,
                externalPaymentId,
                event.getReferenceNumber(),
                UUID.fromString(event.getCustomerId()),
                sourceAccountId,
                destinationAccountId,
                new BigDecimal(event.getAmount()),
                event.getCurrency(),
                event.getPaymentType().toString(),
                event.getDescription()
        );

        commandGateway.sendAndWait(command);

        log.info("Payment initiated successfully: internalId:{}, externalId:{}",
                paymentId,
                externalPaymentId);
    }
}
