package org.banksolution.infrastructure.messaging.kafka.handler;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.domain.payment.command.ConfirmAccountChargedCommand;
import org.banksolution.domain.payment.command.FailAccountChargeCommand;
import org.banksolution.exception.InvalidPaymentStateException;
import org.banksolution.fixtures.AvroEventFixtures;
import org.banksolution.fixtures.PaymentFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountChargeCompletedEventHandlerTest {

    private CommandGateway commandGateway;
    private AccountChargeCompletedEventHandler handler;

    @BeforeEach
    void setUp() {
        commandGateway = mock(CommandGateway.class);
        handler = new AccountChargeCompletedEventHandler(commandGateway);
    }

    @Test
    void shouldDispatchConfirmCommandOnSuccessfulCharge() {
        handler.handle(AvroEventFixtures.accountChargeCompletedEvent(true, null));

        ArgumentCaptor<ConfirmAccountChargedCommand> captor = ArgumentCaptor.forClass(ConfirmAccountChargedCommand.class);
        verify(commandGateway).sendAndWait(captor.capture());

        ConfirmAccountChargedCommand command = captor.getValue();
        assertThat(command.paymentId().getIdentifier()).isEqualTo(PaymentFixtures.PAYMENT_UUID);
        assertThat(command.amount()).isEqualByComparingTo(PaymentFixtures.AMOUNT);
        assertThat(command.paymentType()).isEqualTo(PaymentFixtures.PAYMENT_TYPE);
    }

    @Test
    void shouldDispatchFailCommandOnFailedCharge() {
        handler.handle(AvroEventFixtures.accountChargeCompletedEvent(false, "Insufficient funds"));

        ArgumentCaptor<FailAccountChargeCommand> captor = ArgumentCaptor.forClass(FailAccountChargeCommand.class);
        verify(commandGateway).sendAndWait(captor.capture());

        assertThat(captor.getValue().paymentId().getIdentifier()).isEqualTo(PaymentFixtures.PAYMENT_UUID);
        assertThat(captor.getValue().failureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void shouldSwallowStaleEventRejectedByAggregate() {
        when(commandGateway.sendAndWait(any())).thenThrow(new InvalidPaymentStateException("payment already terminal"));

        assertThatNoException()
                .isThrownBy(() -> handler.handle(AvroEventFixtures.accountChargeCompletedEvent(true, null)));
    }
}
