package org.banksolution.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostingInstructionType {

    INBOUND_AUTHORISATION(1, TransferType.PENDING, true),
    OUTBOUND_AUTHORISATION(2, TransferType.PENDING, false),
    SETTLEMENT(3, TransferType.POST_PENDING, false),
    RELEASE(4, TransferType.VOID_PENDING, false),
    INBOUND_HARD_SETTLEMENT(5, TransferType.SINGLE_PHASE, true),
    OUTBOUND_HARD_SETTLEMENT(6, TransferType.SINGLE_PHASE, false),
    INTERNAL_TRANSFER_AUTHORISATION(7, TransferType.PENDING, false);

    private final int code;
    private final TransferType transferType;
    private final boolean inbound;

    public static PostingInstructionType fromCode(int code) {
        for (PostingInstructionType type : values()) {
            if (type.code == code) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown posting instruction type code: " + code);
    }

    public boolean isAuthorisation() {
        return transferType == TransferType.PENDING;
    }

    public boolean movesBetweenCustomerWallets() {
        return this == INTERNAL_TRANSFER_AUTHORISATION;
    }
}
