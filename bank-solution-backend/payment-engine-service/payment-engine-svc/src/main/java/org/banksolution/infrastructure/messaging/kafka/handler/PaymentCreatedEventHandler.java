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
        UUID sourceAccountId = event.getSourceAccountId() != null ? UUID.fromString(event.getSourceAccountId()) : null;
        UUID destinationAccountId = event.getDestinationAccountId() != null ? UUID.fromString(event.getDestinationAccountId()) : null;
        UUID customerId = UUID.fromString(event.getCustomerId());

        InitiatePaymentCommand command = new InitiatePaymentCommand(
                paymentId,
                customerId,
                sourceAccountId,
                destinationAccountId,
                new BigDecimal(event.getAmount()),
                event.getFromCurrency(),
                event.getToCurrency(),
                new BigDecimal(event.getConvertedAmount()),
                event.getAppliedExchangeRate() != null ? new BigDecimal(event.getAppliedExchangeRate()) : null,
                event.getPaymentType().toString(),
                event.getIsCrossBorderPayment(),
                event.getDescription()
        );

        commandGateway.send(command);
        log.info("Payment initiated for paymentId:{} successfully", paymentId);
    }
}
