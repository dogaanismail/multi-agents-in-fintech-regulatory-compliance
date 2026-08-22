package org.banksolution.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LedgerAccountType {

    WALLET(1, false),
    INBOUND_CLEARING(2, true),
    OUTBOUND_CLEARING(3, true),
    FEES_INCOME(4, true),
    SUSPENSE(5, true);

    private final int code;
    private final boolean internal;

    public static LedgerAccountType fromCode(int code) {
        for (LedgerAccountType type : values()) {
            if (type.code == code) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown ledger account type code: " + code);
    }

    public static LedgerAccountType[] internalTypes() {
        return java.util.Arrays.stream(values())
                .filter(LedgerAccountType::isInternal)
                .toArray(LedgerAccountType[]::new);
    }
}
