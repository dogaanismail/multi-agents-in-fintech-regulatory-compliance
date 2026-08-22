package org.banksolution.domain;

import lombok.Builder;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PostingInstructionType;
import org.banksolution.enums.TransferType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record LedgerTransfer(
        UUID id,
        UUID clientTransactionId,
        PostingInstructionType postingInstructionType,
        UUID debitAccountId,
        UUID creditAccountId,
        BigDecimal amount,
        Currency currency,
        UUID pendingTransferId,
        int timeoutSeconds,
        Instant createdAt) {

    public TransferType transferType() {
        return postingInstructionType.getTransferType();
    }
}
