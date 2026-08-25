package org.banksolution.infrastructure.messaging.kafka.handler;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AvroEventFixtures.createDepositPaymentCreatedEvent;
import static org.banksolution.fixtures.AvroEventFixtures.createPaymentCreatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCreatedEventHandlerTest {

    @Mock
    private CommandGateway commandGateway;

    @InjectMocks
    private PaymentCreatedEventHandler paymentCreatedEventHandler;

    @Test
    void shouldInitiateThePaymentSynchronouslyFromTheAvroEvent() {
        paymentCreatedEventHandler.handle(createPaymentCreatedEvent());

        ArgumentCaptor<InitiatePaymentCommand> initiatePaymentCommandCaptor = ArgumentCaptor.forClass(InitiatePaymentCommand.class);
        verify(commandGateway).sendAndWait(initiatePaymentCommandCaptor.capture());

        InitiatePaymentCommand initiatePaymentCommand = initiatePaymentCommandCaptor.getValue();
        assertThat(initiatePaymentCommand.paymentId()).isEqualTo(createPaymentId());
        assertThat(initiatePaymentCommand.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(initiatePaymentCommand.sourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(initiatePaymentCommand.destinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID);
        assertThat(initiatePaymentCommand.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(initiatePaymentCommand.convertedAmount()).isEqualByComparingTo(CONVERTED_AMOUNT);
        assertThat(initiatePaymentCommand.appliedExchangeRate()).isEqualByComparingTo(EXCHANGE_RATE);
        assertThat(initiatePaymentCommand.fromCurrency()).isEqualTo(FROM_CURRENCY);
        assertThat(initiatePaymentCommand.toCurrency()).isEqualTo(TO_CURRENCY);
        assertThat(initiatePaymentCommand.paymentType()).isEqualTo(PAYMENT_TYPE);
        assertThat(initiatePaymentCommand.paymentScheme()).isEqualTo(PAYMENT_SCHEME);
        assertThat(initiatePaymentCommand.fixedSide()).isEqualTo(FIXED_SIDE);
        assertThat(initiatePaymentCommand.isCrossBorderPayment()).isFalse();
        assertThat(initiatePaymentCommand.description()).isEqualTo(DESCRIPTION);
    }

    @Test
    void shouldMapNullableAccountsAndExchangeRateForADeposit() {
        paymentCreatedEventHandler.handle(createDepositPaymentCreatedEvent());

        ArgumentCaptor<InitiatePaymentCommand> initiatePaymentCommandCaptor = ArgumentCaptor.forClass(InitiatePaymentCommand.class);
        verify(commandGateway).sendAndWait(initiatePaymentCommandCaptor.capture());

        InitiatePaymentCommand initiatePaymentCommand = initiatePaymentCommandCaptor.getValue();
        assertThat(initiatePaymentCommand.sourceAccountId()).isNull();
        assertThat(initiatePaymentCommand.destinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID);
        assertThat(initiatePaymentCommand.appliedExchangeRate()).isNull();
        assertThat(initiatePaymentCommand.description()).isNull();
        assertThat(initiatePaymentCommand.paymentType()).isEqualTo("DEPOSIT");
        assertThat(initiatePaymentCommand.paymentScheme()).isEqualTo("EXTERNAL_INBOUND");
    }

    @Test
    void shouldLetACommandRejectionSurfaceInsteadOfSwallowingIt() {
        IllegalStateException commandRejection = new IllegalStateException("aggregate already exists");
        when(commandGateway.sendAndWait(any(InitiatePaymentCommand.class))).thenThrow(commandRejection);

        assertThatThrownBy(() -> paymentCreatedEventHandler.handle(createPaymentCreatedEvent())).isSameAs(commandRejection);
    }
}
