package org.banksolution.service;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.LedgerPostingRequestedEvent;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PostingInstructionType;
import org.banksolution.exception.InsufficientLedgerFundsException;
import org.banksolution.exception.LedgerPostingException;
import org.banksolution.exception.LedgerUnavailableException;
import org.banksolution.exception.PendingAuthorisationNotFoundException;
import org.banksolution.infrastructure.messaging.kafka.producer.LedgerPostingCompletedEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AvroEventFixtures.createOutboundAuthorisationRequestedEvent;
import static org.banksolution.fixtures.AvroEventFixtures.createSettlementRequestedEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerPostingRequestServiceTest {

    private static final UUID CLIENT_TRANSACTION_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ACCOUNT_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");

    @Mock
    private LedgerPostingService ledgerPostingService;

    @Mock
    private LedgerPostingCompletedEventProducer ledgerPostingCompletedEventProducer;

    @InjectMocks
    private LedgerPostingRequestService ledgerPostingRequestService;

    @Test
    void shouldApplyTheInstructionAndReportTheFirstLegAsTheOutcome() {
        LedgerPostingRequestedEvent ledgerPostingRequestedEvent =
                createOutboundAuthorisationRequestedEvent(CLIENT_TRANSACTION_ID, CUSTOMER_ACCOUNT_ID, AMOUNT, Currency.GBP);
        LedgerTransfer appliedLedgerTransfer = LedgerTransfer.builder()
                .id(UUID.randomUUID())
                .clientTransactionId(CLIENT_TRANSACTION_ID)
                .postingInstructionType(PostingInstructionType.OUTBOUND_AUTHORISATION)
                .amount(AMOUNT)
                .currency(Currency.GBP)
                .build();
        when(ledgerPostingService.applyPostingInstruction(any())).thenReturn(List.of(appliedLedgerTransfer));

        ledgerPostingRequestService.processLedgerPostingRequest(ledgerPostingRequestedEvent);

        ArgumentCaptor<LedgerPostingInstruction> ledgerPostingInstructionCaptor = ArgumentCaptor.forClass(LedgerPostingInstruction.class);
        verify(ledgerPostingService).applyPostingInstruction(ledgerPostingInstructionCaptor.capture());
        assertThat(ledgerPostingInstructionCaptor.getValue().customerAccountId()).isEqualTo(CUSTOMER_ACCOUNT_ID);
        LedgerPostingCompletedEvent ledgerPostingCompletedEvent = capturePublishedOutcome();
        assertThat(ledgerPostingCompletedEvent.getSuccess()).isTrue();
        assertThat(ledgerPostingCompletedEvent.getTransferId()).isEqualTo(appliedLedgerTransfer.id().toString());
    }

    @Test
    void shouldReportInsufficientFundsAsARejectionRatherThanAFailure() {
        when(ledgerPostingService.applyPostingInstruction(any()))
                .thenThrow(new InsufficientLedgerFundsException(UUID.randomUUID()));

        ledgerPostingRequestService.processLedgerPostingRequest(
                createOutboundAuthorisationRequestedEvent(CLIENT_TRANSACTION_ID, CUSTOMER_ACCOUNT_ID, AMOUNT, Currency.GBP));

        LedgerPostingCompletedEvent ledgerPostingCompletedEvent = capturePublishedOutcome();
        assertThat(ledgerPostingCompletedEvent.getSuccess()).isFalse();
        assertThat(ledgerPostingCompletedEvent.getFailureReason()).startsWith("Insufficient funds on ledger account");
        assertThat(ledgerPostingCompletedEvent.getPostingInstructionType())
                .isEqualTo(com.aml.ledger.PostingInstructionType.OUTBOUND_AUTHORISATION);
    }

    @Test
    void shouldReportAMissingAuthorisationAndOtherLedgerRejections() {
        when(ledgerPostingService.applyPostingInstruction(any()))
                .thenThrow(new PendingAuthorisationNotFoundException(CLIENT_TRANSACTION_ID))
                .thenThrow(new LedgerPostingException("AccountsMustBeDifferent"));

        ledgerPostingRequestService.processLedgerPostingRequest(createSettlementRequestedEvent(CLIENT_TRANSACTION_ID));
        ledgerPostingRequestService.processLedgerPostingRequest(createSettlementRequestedEvent(CLIENT_TRANSACTION_ID));

        ArgumentCaptor<LedgerPostingCompletedEvent> ledgerPostingCompletedEventCaptor =
                ArgumentCaptor.forClass(LedgerPostingCompletedEvent.class);
        verify(ledgerPostingCompletedEventProducer, times(2)).publish(ledgerPostingCompletedEventCaptor.capture());
        assertThat(ledgerPostingCompletedEventCaptor.getAllValues())
                .extracting(LedgerPostingCompletedEvent::getFailureReason)
                .containsExactly(
                        "No authorisation found for client transaction: " + CLIENT_TRANSACTION_ID,
                        "Failed to post ledger transfer, TigerBeetle returned: AccountsMustBeDifferent");
    }

    @Test
    void shouldLetAnUnavailableLedgerPropagateSoTheRecordIsRetried() {
        LedgerUnavailableException ledgerUnavailable = new LedgerUnavailableException(new InterruptedException("cluster down"));
        when(ledgerPostingService.applyPostingInstruction(any())).thenThrow(ledgerUnavailable);
        LedgerPostingRequestedEvent settlementRequestedEvent = createSettlementRequestedEvent(CLIENT_TRANSACTION_ID);

        assertThatThrownBy(() -> ledgerPostingRequestService.processLedgerPostingRequest(settlementRequestedEvent))
                .isSameAs(ledgerUnavailable);

        verifyNoInteractions(ledgerPostingCompletedEventProducer);
    }

    private LedgerPostingCompletedEvent capturePublishedOutcome() {
        ArgumentCaptor<LedgerPostingCompletedEvent> ledgerPostingCompletedEventCaptor =
                ArgumentCaptor.forClass(LedgerPostingCompletedEvent.class);
        verify(ledgerPostingCompletedEventProducer).publish(ledgerPostingCompletedEventCaptor.capture());
        return ledgerPostingCompletedEventCaptor.getValue();
    }
}
