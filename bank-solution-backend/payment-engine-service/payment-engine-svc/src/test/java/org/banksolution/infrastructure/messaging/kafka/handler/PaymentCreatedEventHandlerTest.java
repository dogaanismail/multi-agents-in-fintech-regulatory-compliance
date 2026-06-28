package org.banksolution.infrastructure.messaging.kafka.handler;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.banksolution.fixtures.AvroEventFixtures;
import org.banksolution.fixtures.PaymentFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentCreatedEventHandlerTest {

    private CommandGateway commandGateway;
    private PaymentCreatedEventHandler handler;

    @BeforeEach
    void setUp() {
        commandGateway = mock(CommandGateway.class);
        handler = new PaymentCreatedEventHandler(commandGateway);
    }

    @Test
    void dispatchesInitiatePaymentCommandFromAvroEvent() {
        handler.handle(AvroEventFixtures.paymentCreatedEvent());

        ArgumentCaptor<InitiatePaymentCommand> captor = ArgumentCaptor.forClass(InitiatePaymentCommand.class);
        verify(commandGateway).send(captor.capture());

        InitiatePaymentCommand command = captor.getValue();
        assertThat(command.paymentId().getIdentifier()).isEqualTo(PaymentFixtures.PAYMENT_UUID);
        assertThat(command.customerId()).isEqualTo(PaymentFixtures.CUSTOMER_ID);
        assertThat(command.sourceAccountId()).isEqualTo(PaymentFixtures.SOURCE_ACCOUNT_ID);
        assertThat(command.amount()).isEqualByComparingTo(PaymentFixtures.AMOUNT);
        assertThat(command.paymentType()).isEqualTo(PaymentFixtures.PAYMENT_TYPE);
        assertThat(command.isCrossBorderPayment()).isFalse();
    }
}
