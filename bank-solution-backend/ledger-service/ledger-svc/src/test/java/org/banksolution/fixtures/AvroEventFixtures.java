package org.banksolution.fixtures;

import com.aml.ledger.LedgerPostingRequestedEvent;
import com.aml.ledger.PostingInstructionType;
import org.banksolution.enums.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public final class AvroEventFixtures {

    public static final long TIMESTAMP = 1_700_000_000_000L;

    private AvroEventFixtures() {
    }

    public static LedgerPostingRequestedEvent createOutboundAuthorisationRequestedEvent(
            UUID clientTransactionId,
            UUID customerAccountId,
            BigDecimal amount,
            Currency currency) {

        return newLedgerPostingRequestedEventBuilder(clientTransactionId, PostingInstructionType.OUTBOUND_AUTHORISATION)
                .setAmount(amount.toPlainString())
                .setCurrency(currency.name())
                .setCustomerAccountId(customerAccountId.toString())
                .build();
    }

    public static LedgerPostingRequestedEvent createInboundHardSettlementRequestedEvent(
            UUID clientTransactionId,
            UUID customerAccountId,
            BigDecimal amount,
            Currency currency,
            String internalAccountType) {

        return newLedgerPostingRequestedEventBuilder(clientTransactionId, PostingInstructionType.INBOUND_HARD_SETTLEMENT)
                .setAmount(amount.toPlainString())
                .setCurrency(currency.name())
                .setCustomerAccountId(customerAccountId.toString())
                .setInternalAccountType(internalAccountType)
                .build();
    }

    public static LedgerPostingRequestedEvent createCrossCurrencyTransferAuthorisationRequestedEvent(
            UUID clientTransactionId,
            UUID sourceCustomerAccountId,
            UUID destinationCustomerAccountId) {

        return newLedgerPostingRequestedEventBuilder(clientTransactionId, PostingInstructionType.CROSS_CURRENCY_TRANSFER_AUTHORISATION)
                .setAmount("250.00")
                .setCurrency(Currency.GBP.name())
                .setBuyAmount("290.00")
                .setBuyCurrency(Currency.EUR.name())
                .setCustomerAccountId(sourceCustomerAccountId.toString())
                .setCounterpartyCustomerAccountId(destinationCustomerAccountId.toString())
                .build();
    }

    public static LedgerPostingRequestedEvent createSettlementRequestedEvent(UUID clientTransactionId) {
        return newLedgerPostingRequestedEventBuilder(clientTransactionId, PostingInstructionType.SETTLEMENT).build();
    }

    public static LedgerPostingRequestedEvent createReleaseRequestedEvent(UUID clientTransactionId) {
        return newLedgerPostingRequestedEventBuilder(clientTransactionId, PostingInstructionType.RELEASE).build();
    }

    public static LedgerPostingRequestedEvent createMalformedLedgerPostingRequestedEvent(String clientTransactionId) {
        return LedgerPostingRequestedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setClientTransactionId(clientTransactionId)
                .setPostingInstructionType(PostingInstructionType.SETTLEMENT)
                .setTimestamp(TIMESTAMP)
                .build();
    }

    private static LedgerPostingRequestedEvent.Builder newLedgerPostingRequestedEventBuilder(
            UUID clientTransactionId,
            PostingInstructionType postingInstructionType) {

        return LedgerPostingRequestedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setClientTransactionId(clientTransactionId.toString())
                .setPostingInstructionType(postingInstructionType)
                .setTimestamp(TIMESTAMP);
    }
}
