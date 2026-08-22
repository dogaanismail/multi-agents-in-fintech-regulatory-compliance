package org.banksolution.mapper;

import com.tigerbeetle.TransferBatch;
import com.tigerbeetle.UInt128;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PostingInstructionType;
import org.banksolution.model.response.LedgerPostingResponse;
import org.banksolution.util.MoneyUtils;

import java.time.Instant;
import java.util.UUID;

public final class LedgerTransferMapper {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private LedgerTransferMapper() {
    }

    public static LedgerTransfer toLedgerTransfer(TransferBatch transferBatch) {
        Currency currency = Currency.fromNumericCode(transferBatch.getLedger());

        return LedgerTransfer.builder()
                .id(UInt128.asUUID(transferBatch.getId()))
                .clientTransactionId(UInt128.asUUID(transferBatch.getUserData128()))
                .postingInstructionType(PostingInstructionType.fromCode(transferBatch.getUserData32()))
                .debitAccountId(UInt128.asUUID(transferBatch.getDebitAccountId()))
                .creditAccountId(UInt128.asUUID(transferBatch.getCreditAccountId()))
                .amount(MoneyUtils.toAmount(transferBatch.getAmount(), currency))
                .currency(currency)
                .pendingTransferId(toNullableUuid(transferBatch.getPendingId()))
                .timeoutSeconds(transferBatch.getTimeout())
                .createdAt(toInstant(transferBatch.getTimestamp()))
                .build();
    }

    public static LedgerPostingResponse toLedgerPostingResponse(LedgerTransfer ledgerTransfer) {
        return LedgerPostingResponse.builder()
                .transferId(ledgerTransfer.id())
                .clientTransactionId(ledgerTransfer.clientTransactionId())
                .postingInstructionType(ledgerTransfer.postingInstructionType())
                .debitAccountId(ledgerTransfer.debitAccountId())
                .creditAccountId(ledgerTransfer.creditAccountId())
                .amount(ledgerTransfer.amount())
                .currency(ledgerTransfer.currency())
                .pendingTransferId(ledgerTransfer.pendingTransferId())
                .createdAt(ledgerTransfer.createdAt())
                .build();
    }

    private static UUID toNullableUuid(byte[] rawId) {
        UUID id = UInt128.asUUID(rawId);
        return id.getMostSignificantBits() == 0 && id.getLeastSignificantBits() == 0 ? null : id;
    }

    private static Instant toInstant(long timestampNanos) {
        if (timestampNanos == 0) {
            return null;
        }

        return Instant.ofEpochSecond(timestampNanos / NANOS_PER_SECOND, timestampNanos % NANOS_PER_SECOND);
    }
}
