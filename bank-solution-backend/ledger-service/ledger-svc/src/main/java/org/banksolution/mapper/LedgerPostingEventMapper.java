package org.banksolution.mapper;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.LedgerPostingRequestedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.enums.PostingInstructionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class LedgerPostingEventMapper {

    public static LedgerPostingInstruction toLedgerPostingInstruction(LedgerPostingRequestedEvent event) {
        UUID clientTransactionId = UUID.fromString(event.getClientTransactionId());

        return new LedgerPostingInstruction(
                clientTransactionId,
                toPostingInstructionType(event),
                toAmount(event.getAmount()),
                toCurrency(event.getCurrency()),
                toUuid(event.getCustomerAccountId()),
                toUuid(event.getCounterpartyCustomerAccountId()),
                toInternalAccountType(event.getInternalAccountType()));
    }

    public static LedgerPostingCompletedEvent toSuccessfulLedgerPostingCompletedEvent(LedgerTransfer ledgerTransfer) {
        return LedgerPostingCompletedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setClientTransactionId(ledgerTransfer.clientTransactionId().toString())
                .setPostingInstructionType(toAvroPostingInstructionType(ledgerTransfer.postingInstructionType()))
                .setSuccess(true)
                .setTransferId(ledgerTransfer.id().toString())
                .setAmount(ledgerTransfer.amount() == null ? null : ledgerTransfer.amount().toPlainString())
                .setCurrency(ledgerTransfer.currency() == null ? null : ledgerTransfer.currency().name())
                .setTimestamp(Instant.now().toEpochMilli())
                .build();
    }

    public static LedgerPostingCompletedEvent toFailedLedgerPostingCompletedEvent(
            LedgerPostingRequestedEvent event,
            String failureReason) {

        return LedgerPostingCompletedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setClientTransactionId(event.getClientTransactionId())
                .setPostingInstructionType(event.getPostingInstructionType())
                .setSuccess(false)
                .setFailureReason(failureReason)
                .setTimestamp(Instant.now().toEpochMilli())
                .build();
    }

    private static PostingInstructionType toPostingInstructionType(LedgerPostingRequestedEvent event) {
        return PostingInstructionType.valueOf(event.getPostingInstructionType().name());
    }

    private static com.aml.ledger.PostingInstructionType toAvroPostingInstructionType(
            PostingInstructionType postingInstructionType) {

        return com.aml.ledger.PostingInstructionType.valueOf(postingInstructionType.name());
    }

    private static BigDecimal toAmount(String amount) {
        return amount == null ? null : new BigDecimal(amount);
    }

    private static Currency toCurrency(String currency) {
        return currency == null ? null : Currency.valueOf(currency);
    }

    private static LedgerAccountType toInternalAccountType(String internalAccountType) {
        return internalAccountType == null ? null : LedgerAccountType.valueOf(internalAccountType);
    }

    private static UUID toUuid(String id) {
        return id == null ? null : UUID.fromString(id);
    }
}
