package org.banksolution.mapper;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.LedgerPostingRequestedEvent;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.enums.PostingInstructionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.AvroEventFixtures.*;

class LedgerPostingEventMapperTest {

    private static final UUID CLIENT_TRANSACTION_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ACCOUNT_ID = UUID.randomUUID();
    private static final UUID COUNTERPARTY_ACCOUNT_ID = UUID.randomUUID();

    @Test
    void shouldMapAMovementRequestWithEveryOptionalFieldPresent() {
        LedgerPostingRequestedEvent ledgerPostingRequestedEvent = createInboundHardSettlementRequestedEvent(
                CLIENT_TRANSACTION_ID, CUSTOMER_ACCOUNT_ID, new BigDecimal("1000.00"), Currency.GBP, "SUSPENSE");

        LedgerPostingInstruction ledgerPostingInstruction =
                LedgerPostingEventMapper.toLedgerPostingInstruction(ledgerPostingRequestedEvent);

        assertThat(ledgerPostingInstruction.clientTransactionId()).isEqualTo(CLIENT_TRANSACTION_ID);
        assertThat(ledgerPostingInstruction.postingInstructionType()).isEqualTo(PostingInstructionType.INBOUND_HARD_SETTLEMENT);
        assertThat(ledgerPostingInstruction.amount()).isEqualByComparingTo("1000.00");
        assertThat(ledgerPostingInstruction.currency()).isEqualTo(Currency.GBP);
        assertThat(ledgerPostingInstruction.customerAccountId()).isEqualTo(CUSTOMER_ACCOUNT_ID);
        assertThat(ledgerPostingInstruction.internalAccountType()).isEqualTo(LedgerAccountType.SUSPENSE);
        assertThat(ledgerPostingInstruction.buyAmount()).isNull();
        assertThat(ledgerPostingInstruction.counterpartyCustomerAccountId()).isNull();
    }

    @Test
    void shouldMapACrossCurrencyRequestWithBothLegs() {
        LedgerPostingInstruction ledgerPostingInstruction = LedgerPostingEventMapper.toLedgerPostingInstruction(
                createCrossCurrencyTransferAuthorisationRequestedEvent(CLIENT_TRANSACTION_ID, CUSTOMER_ACCOUNT_ID, COUNTERPARTY_ACCOUNT_ID));

        assertThat(ledgerPostingInstruction.postingInstructionType()).isEqualTo(PostingInstructionType.CROSS_CURRENCY_TRANSFER_AUTHORISATION);
        assertThat(ledgerPostingInstruction.buyAmount()).isEqualByComparingTo("290.00");
        assertThat(ledgerPostingInstruction.buyCurrency()).isEqualTo(Currency.EUR);
        assertThat(ledgerPostingInstruction.counterpartyCustomerAccountId()).isEqualTo(COUNTERPARTY_ACCOUNT_ID);
    }

    @Test
    void shouldMapAFollowUpRequestThatCarriesOnlyTheClientTransactionId() {
        LedgerPostingInstruction ledgerPostingInstruction =
                LedgerPostingEventMapper.toLedgerPostingInstruction(createReleaseRequestedEvent(CLIENT_TRANSACTION_ID));

        assertThat(ledgerPostingInstruction.postingInstructionType()).isEqualTo(PostingInstructionType.RELEASE);
        assertThat(ledgerPostingInstruction.amount()).isNull();
        assertThat(ledgerPostingInstruction.currency()).isNull();
        assertThat(ledgerPostingInstruction.customerAccountId()).isNull();
        assertThat(ledgerPostingInstruction.internalAccountType()).isNull();
    }

    @Test
    void shouldReportASuccessfulPostingWithItsTransferAndAmount() {
        LedgerTransfer ledgerTransfer = LedgerTransfer.builder()
                .id(UUID.randomUUID())
                .clientTransactionId(CLIENT_TRANSACTION_ID)
                .postingInstructionType(PostingInstructionType.OUTBOUND_AUTHORISATION)
                .amount(new BigDecimal("250.00"))
                .currency(Currency.GBP)
                .build();

        LedgerPostingCompletedEvent ledgerPostingCompletedEvent =
                LedgerPostingEventMapper.toSuccessfulLedgerPostingCompletedEvent(ledgerTransfer);

        assertThat(ledgerPostingCompletedEvent.getSuccess()).isTrue();
        assertThat(ledgerPostingCompletedEvent.getClientTransactionId()).isEqualTo(CLIENT_TRANSACTION_ID.toString());
        assertThat(ledgerPostingCompletedEvent.getTransferId()).isEqualTo(ledgerTransfer.id().toString());
        assertThat(ledgerPostingCompletedEvent.getPostingInstructionType()).isEqualTo(com.aml.ledger.PostingInstructionType.OUTBOUND_AUTHORISATION);
        assertThat(ledgerPostingCompletedEvent.getAmount()).isEqualTo("250.00");
        assertThat(ledgerPostingCompletedEvent.getCurrency()).isEqualTo("GBP");
        assertThat(ledgerPostingCompletedEvent.getFailureReason()).isNull();
        assertThat(ledgerPostingCompletedEvent.getEventId()).isNotBlank();
        assertThat(ledgerPostingCompletedEvent.getTimestamp()).isPositive();
    }

    @Test
    void shouldReportASuccessfulFollowUpWithoutAmountOrCurrency() {
        LedgerTransfer settlementTransfer = LedgerTransfer.builder()
                .id(UUID.randomUUID())
                .clientTransactionId(CLIENT_TRANSACTION_ID)
                .postingInstructionType(PostingInstructionType.SETTLEMENT)
                .build();

        LedgerPostingCompletedEvent ledgerPostingCompletedEvent =
                LedgerPostingEventMapper.toSuccessfulLedgerPostingCompletedEvent(settlementTransfer);

        assertThat(ledgerPostingCompletedEvent.getAmount()).isNull();
        assertThat(ledgerPostingCompletedEvent.getCurrency()).isNull();
    }

    @Test
    void shouldReportARejectionWithTheLedgerReason() {
        LedgerPostingCompletedEvent ledgerPostingCompletedEvent = LedgerPostingEventMapper.toFailedLedgerPostingCompletedEvent(
                createSettlementRequestedEvent(CLIENT_TRANSACTION_ID), "No authorisation found");

        assertThat(ledgerPostingCompletedEvent.getSuccess()).isFalse();
        assertThat(ledgerPostingCompletedEvent.getClientTransactionId()).isEqualTo(CLIENT_TRANSACTION_ID.toString());
        assertThat(ledgerPostingCompletedEvent.getPostingInstructionType()).isEqualTo(com.aml.ledger.PostingInstructionType.SETTLEMENT);
        assertThat(ledgerPostingCompletedEvent.getFailureReason()).isEqualTo("No authorisation found");
        assertThat(ledgerPostingCompletedEvent.getTransferId()).isNull();
    }
}
