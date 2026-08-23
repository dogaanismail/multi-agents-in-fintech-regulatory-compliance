package org.banksolution.domain;

import org.banksolution.enums.PostingInstructionType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class LedgerTransferIds {

    public static final int SINGLE_LEG = 0;
    public static final int MAX_LEGS_PER_INSTRUCTION = 2;

    private LedgerTransferIds() {
    }

    public static UUID deriveTransferId(
            UUID clientTransactionId,
            PostingInstructionType postingInstructionType,
            int legIndex) {

        String seed = "posting:" + clientTransactionId + ":" + postingInstructionType.name() + ":" + legIndex;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    public static UUID deriveTransferId(
            UUID clientTransactionId,
            PostingInstructionType postingInstructionType) {

        return deriveTransferId(clientTransactionId, postingInstructionType, SINGLE_LEG);
    }
}
