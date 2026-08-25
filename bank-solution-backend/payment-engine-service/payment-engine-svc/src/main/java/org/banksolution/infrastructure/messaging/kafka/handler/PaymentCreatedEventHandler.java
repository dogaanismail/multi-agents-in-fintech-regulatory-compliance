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

    public void handle(PaymentCreatedEvent paymentCreatedEvent) {
        log.info("Handling PaymentCreatedEvent: eventId:{}, paymentId:{}",
                paymentCreatedEvent.getEventId(),
                paymentCreatedEvent.getPaymentId());

        PaymentId paymentId = new PaymentId(UUID.fromString(paymentCreatedEvent.getPaymentId()));
        UUID sourceAccountId = toUuid(paymentCreatedEvent.getSourceAccountId());
        UUID destinationAccountId = toUuid(paymentCreatedEvent.getDestinationAccountId());
        UUID customerId = UUID.fromString(paymentCreatedEvent.getCustomerId());

        InitiatePaymentCommand initiatePaymentCommand = new InitiatePaymentCommand(
                paymentId,
                customerId,
                sourceAccountId,
                destinationAccountId,
                new BigDecimal(paymentCreatedEvent.getAmount()),
                paymentCreatedEvent.getFromCurrency(),
                paymentCreatedEvent.getToCurrency(),
                new BigDecimal(paymentCreatedEvent.getConvertedAmount()),
                toBigDecimal(paymentCreatedEvent.getAppliedExchangeRate()),
                paymentCreatedEvent.getPaymentType().name(),
                paymentCreatedEvent.getPaymentScheme().name(),
                paymentCreatedEvent.getFixedSide().name(),
                paymentCreatedEvent.getIsCrossBorderPayment(),
                paymentCreatedEvent.getDescription()
        );

        // sendAndWait so a rejected command fails the Kafka record (retry, then DLT)
        // instead of being acknowledged and silently lost.
        commandGateway.sendAndWait(initiatePaymentCommand);
        log.info("Payment initiated for paymentId:{} successfully", paymentId);
    }

    private static UUID toUuid(String identifier) {
        return identifier != null ? UUID.fromString(identifier) : null;
    }

    private static BigDecimal toBigDecimal(String amount) {
        return amount != null ? new BigDecimal(amount) : null;
    }
}
