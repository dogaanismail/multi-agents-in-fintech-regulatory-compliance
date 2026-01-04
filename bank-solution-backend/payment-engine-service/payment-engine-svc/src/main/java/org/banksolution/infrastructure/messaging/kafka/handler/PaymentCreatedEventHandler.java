package org.banksolution.infrastructure.messaging.kafka.handler;

import com.aml.payment.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreatedEventHandler {

    private final CommandGateway commandGateway;

    public void handle(PaymentCreatedEvent event) {
        log.info("Handling PaymentCreatedEvent: eventId:{}, paymentId:{}", event.getEventId(), event.getPaymentId());

        PaymentId paymentId = new PaymentId(UUID.fromString(event.getPaymentId()));
        UUID sourceAccountId = UUID.fromString(event.getSourceAccountId());
        UUID destinationAccountId = UUID.fromString(event.getDestinationAccountId());
        UUID customerId = UUID.fromString(event.getCustomerId());

        InitiatePaymentCommand command = new InitiatePaymentCommand(
                paymentId,
                event.getReferenceNumber(),
                customerId,
                sourceAccountId,
                destinationAccountId,
                new BigDecimal(event.getAmount()),
                event.getCurrency(),
                event.getPaymentType().toString(),
                event.getDescription()
        );

        commandGateway.send(command);

        log.info("Payment initiated for paymentId:{} successfully", paymentId);
    }
}
