package org.banksolution.infrastructure.messaging.kafka.handler;

import com.aml.ledger.PostingInstructionType;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.domain.payment.command.ConfirmLedgerAuthorisationCommand;
import org.banksolution.domain.payment.command.ConfirmLedgerReleaseCommand;
import org.banksolution.domain.payment.command.ConfirmLedgerSettlementCommand;
import org.banksolution.domain.payment.command.DeclineLedgerAuthorisationCommand;
import org.banksolution.domain.payment.command.FailLedgerReleaseCommand;
import org.banksolution.domain.payment.command.FailLedgerSettlementCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.banksolution.fixtures.AvroEventFixtures.createLedgerPostingCompletedEvent;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LedgerPostingCompletedEventHandlerTest {

    private static final String FAILURE_REASON = "Insufficient funds";

    @Mock
    private CommandGateway commandGateway;

    @InjectMocks
    private LedgerPostingCompletedEventHandler ledgerPostingCompletedEventHandler;

    @ParameterizedTest
    @EnumSource(value = PostingInstructionType.class, names = {
            "INBOUND_AUTHORISATION",
            "OUTBOUND_AUTHORISATION",
            "INTERNAL_TRANSFER_AUTHORISATION",
            "CROSS_CURRENCY_TRANSFER_AUTHORISATION"})
    void shouldConfirmTheAuthorisationForEveryAuthorisationType(PostingInstructionType postingInstructionType) {
        ledgerPostingCompletedEventHandler.handle(createLedgerPostingCompletedEvent(
                PAYMENT_UUID, postingInstructionType, true, AUTHORISATION_TRANSFER_ID, null));

        verify(commandGateway).sendAndWait(new ConfirmLedgerAuthorisationCommand(createPaymentId(), AUTHORISATION_TRANSFER_ID));
    }

    @Test
    void shouldDeclineTheAuthorisationWhenTheLedgerRefusedIt() {
        ledgerPostingCompletedEventHandler.handle(createLedgerPostingCompletedEvent(
                PAYMENT_UUID, PostingInstructionType.OUTBOUND_AUTHORISATION, false, null, FAILURE_REASON));

        verify(commandGateway).sendAndWait(new DeclineLedgerAuthorisationCommand(createPaymentId(), FAILURE_REASON));
    }

    @Test
    void shouldConfirmOrFailTheSettlement() {
        ledgerPostingCompletedEventHandler.handle(createLedgerPostingCompletedEvent(
                PAYMENT_UUID, PostingInstructionType.SETTLEMENT, true, SETTLEMENT_TRANSFER_ID, null));
        ledgerPostingCompletedEventHandler.handle(createLedgerPostingCompletedEvent(
                PAYMENT_UUID, PostingInstructionType.SETTLEMENT, false, null, FAILURE_REASON));

        verify(commandGateway).sendAndWait(new ConfirmLedgerSettlementCommand(createPaymentId(), SETTLEMENT_TRANSFER_ID));
        verify(commandGateway).sendAndWait(new FailLedgerSettlementCommand(createPaymentId(), FAILURE_REASON));
    }

    @Test
    void shouldConfirmOrFailTheRelease() {
        ledgerPostingCompletedEventHandler.handle(createLedgerPostingCompletedEvent(
                PAYMENT_UUID, PostingInstructionType.RELEASE, true, null, null));
        ledgerPostingCompletedEventHandler.handle(createLedgerPostingCompletedEvent(
                PAYMENT_UUID, PostingInstructionType.RELEASE, false, null, FAILURE_REASON));

        verify(commandGateway).sendAndWait(new ConfirmLedgerReleaseCommand(createPaymentId()));
        verify(commandGateway).sendAndWait(new FailLedgerReleaseCommand(createPaymentId(), FAILURE_REASON));
    }

    @Test
    void shouldConfirmASettlementWithoutATransferId() {
        ledgerPostingCompletedEventHandler.handle(createLedgerPostingCompletedEvent(
                PAYMENT_UUID, PostingInstructionType.SETTLEMENT, true, null, null));

        verify(commandGateway).sendAndWait(new ConfirmLedgerSettlementCommand(createPaymentId(), null));
    }

    @ParameterizedTest
    @EnumSource(value = PostingInstructionType.class, names = {"INBOUND_HARD_SETTLEMENT", "OUTBOUND_HARD_SETTLEMENT"})
    void shouldIgnoreHardSettlementsNoSagaAwaits(PostingInstructionType postingInstructionType) {
        ledgerPostingCompletedEventHandler.handle(createLedgerPostingCompletedEvent(
                PAYMENT_UUID, postingInstructionType, true, SETTLEMENT_TRANSFER_ID, null));

        verifyNoInteractions(commandGateway);
    }
}
