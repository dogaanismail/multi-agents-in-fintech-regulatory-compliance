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
import java.util.function.Function;

@UtilityClass
public class LedgerPostingEventMapper {

    public static LedgerPostingInstruction toLedgerPostingInstruction(LedgerPostingRequestedEvent event) {
        UUID clientTransactionId = UUID.fromString(event.getClientTransactionId());

        return new LedgerPostingInstruction(
                clientTransactionId,
                toPostingInstructionType(event),
                parseIfPresent(event.getAmount(), BigDecimal::new),
                parseIfPresent(event.getCurrency(), Currency::valueOf),
                parseIfPresent(event.getBuyAmount(), BigDecimal::new),
                parseIfPresent(event.getBuyCurrency(), Currency::valueOf),
                parseIfPresent(event.getCustomerAccountId(), UUID::fromString),
                parseIfPresent(event.getCounterpartyCustomerAccountId(), UUID::fromString),
                parseIfPresent(event.getInternalAccountType(), LedgerAccountType::valueOf));
    }

    public static LedgerPostingCompletedEvent toSuccessfulLedgerPostingCompletedEvent(LedgerTransfer ledgerTransfer) {
        return LedgerPostingCompletedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setClientTransactionId(ledgerTransfer.clientTransactionId().toString())
                .setPostingInstructionType(toAvroPostingInstructionType(ledgerTransfer.postingInstructionType()))
                .setSuccess(true)
                .setTransferId(ledgerTransfer.id().toString())
                .setAmount(formatIfPresent(ledgerTransfer.amount(), BigDecimal::toPlainString))
                .setCurrency(formatIfPresent(ledgerTransfer.currency(), Currency::name))
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

    private static <T> T parseIfPresent(String value, Function<String, T> parse) {
        return value == null ? null : parse.apply(value);
    }

    private static <T> String formatIfPresent(T value, Function<T, String> format) {
        return value == null ? null : format.apply(value);
    }
}
