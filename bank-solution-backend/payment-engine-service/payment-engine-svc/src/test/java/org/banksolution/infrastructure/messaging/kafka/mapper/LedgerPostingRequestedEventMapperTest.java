package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.ledger.LedgerPostingRequestedEvent;
import com.aml.ledger.PostingInstructionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;

class LedgerPostingRequestedEventMapperTest {

    @Test
    void shouldRequestAnInternalTransferAuthorisationBetweenTwoWalletsOfOneCurrency() {
        LedgerPostingRequestedEvent ledgerPostingRequestedEvent = LedgerPostingRequestedEventMapper
                .toAuthorisationEvent(createLedgerAuthorisationInitiatedEvent("INTERNAL_TRANSFER", FROM_CURRENCY));

        assertThat(ledgerPostingRequestedEvent.getPostingInstructionType()).isEqualTo(PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION);
        assertThat(ledgerPostingRequestedEvent.getClientTransactionId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(ledgerPostingRequestedEvent.getAmount()).isEqualTo("100.00");
        assertThat(ledgerPostingRequestedEvent.getCurrency()).isEqualTo(FROM_CURRENCY);
        assertThat(ledgerPostingRequestedEvent.getCustomerAccountId()).isEqualTo(SOURCE_ACCOUNT_ID.toString());
        assertThat(ledgerPostingRequestedEvent.getCounterpartyCustomerAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID.toString());
        assertThat(ledgerPostingRequestedEvent.getBuyAmount()).isNull();
        assertThat(ledgerPostingRequestedEvent.getEventId()).isNotBlank();
        assertThat(ledgerPostingRequestedEvent.getTimestamp()).isPositive();
    }

    @Test
    void shouldRequestACrossCurrencyAuthorisationWhenTheTransferCrossesCurrencies() {
        LedgerPostingRequestedEvent ledgerPostingRequestedEvent = LedgerPostingRequestedEventMapper
                .toAuthorisationEvent(createLedgerAuthorisationInitiatedEvent("INTERNAL_TRANSFER", "EUR"));

        assertThat(ledgerPostingRequestedEvent.getPostingInstructionType()).isEqualTo(PostingInstructionType.CROSS_CURRENCY_TRANSFER_AUTHORISATION);
        assertThat(ledgerPostingRequestedEvent.getAmount()).isEqualTo("100.00");
        assertThat(ledgerPostingRequestedEvent.getCurrency()).isEqualTo(FROM_CURRENCY);
        assertThat(ledgerPostingRequestedEvent.getBuyAmount()).isEqualTo("100.00");
        assertThat(ledgerPostingRequestedEvent.getBuyCurrency()).isEqualTo("EUR");
        assertThat(ledgerPostingRequestedEvent.getCustomerAccountId()).isEqualTo(SOURCE_ACCOUNT_ID.toString());
        assertThat(ledgerPostingRequestedEvent.getCounterpartyCustomerAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID.toString());
    }

    @Test
    void shouldRequestAnOutboundAuthorisationAgainstTheSourceWallet() {
        LedgerPostingRequestedEvent ledgerPostingRequestedEvent = LedgerPostingRequestedEventMapper
                .toAuthorisationEvent(createLedgerAuthorisationInitiatedEvent("EXTERNAL_OUTBOUND", FROM_CURRENCY));

        assertThat(ledgerPostingRequestedEvent.getPostingInstructionType()).isEqualTo(PostingInstructionType.OUTBOUND_AUTHORISATION);
        assertThat(ledgerPostingRequestedEvent.getCurrency()).isEqualTo(FROM_CURRENCY);
        assertThat(ledgerPostingRequestedEvent.getCustomerAccountId()).isEqualTo(SOURCE_ACCOUNT_ID.toString());
        assertThat(ledgerPostingRequestedEvent.getCounterpartyCustomerAccountId()).isNull();
    }

    @Test
    void shouldRequestAnInboundAuthorisationAgainstTheDestinationWalletInTheTargetCurrency() {
        LedgerPostingRequestedEvent ledgerPostingRequestedEvent = LedgerPostingRequestedEventMapper
                .toAuthorisationEvent(createLedgerAuthorisationInitiatedEvent("EXTERNAL_INBOUND", "EUR"));

        assertThat(ledgerPostingRequestedEvent.getPostingInstructionType()).isEqualTo(PostingInstructionType.INBOUND_AUTHORISATION);
        assertThat(ledgerPostingRequestedEvent.getAmount()).isEqualTo(CONVERTED_AMOUNT.toPlainString());
        assertThat(ledgerPostingRequestedEvent.getCurrency()).isEqualTo("EUR");
        assertThat(ledgerPostingRequestedEvent.getCustomerAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID.toString());
    }

    @Test
    void shouldRequestSettlementAndReleaseByClientTransactionIdOnly() {
        LedgerPostingRequestedEvent settlementEvent = LedgerPostingRequestedEventMapper.toSettlementEvent(createPaymentId());
        LedgerPostingRequestedEvent releaseEvent = LedgerPostingRequestedEventMapper.toReleaseEvent(createPaymentId());

        assertThat(settlementEvent.getPostingInstructionType()).isEqualTo(PostingInstructionType.SETTLEMENT);
        assertThat(releaseEvent.getPostingInstructionType()).isEqualTo(PostingInstructionType.RELEASE);
        assertThat(settlementEvent.getClientTransactionId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(releaseEvent.getClientTransactionId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(settlementEvent.getAmount()).isNull();
        assertThat(releaseEvent.getCustomerAccountId()).isNull();
    }
}
