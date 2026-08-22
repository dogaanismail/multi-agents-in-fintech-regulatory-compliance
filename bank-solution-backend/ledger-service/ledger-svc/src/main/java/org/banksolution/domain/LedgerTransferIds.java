package org.banksolution.domain;

import org.banksolution.enums.PostingInstructionType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class LedgerTransferIds {

    private LedgerTransferIds() {
    }

    public static UUID deriveTransferId(
            UUID clientTransactionId,
            PostingInstructionType postingInstructionType) {

        String seed = "posting:" + clientTransactionId + ":" + postingInstructionType.name();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
