package org.banksolution.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerAccountTypeTest {

    private static final int UNKNOWN_CODE = 99;

    @Test
    void shouldHaveUniqueCodes() {
        assertThat(LedgerAccountType.values())
                .extracting(LedgerAccountType::getCode)
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldNeverUseZeroAsCode() {
        assertThat(LedgerAccountType.values()).allMatch(type -> type.getCode() > 0);
    }

    @Test
    void shouldResolveEveryTypeFromItsCode() {
        assertThat(LedgerAccountType.values())
                .allSatisfy(type -> assertThat(LedgerAccountType.fromCode(type.getCode())).isEqualTo(type));
    }

    @Test
    void shouldRejectUnknownCode() {
        assertThatThrownBy(() -> LedgerAccountType.fromCode(UNKNOWN_CODE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown ledger account type code");
    }

    @Test
    void shouldTreatWalletAsCustomerOwnedAndTheRestAsInternal() {
        assertThat(LedgerAccountType.WALLET.isInternal()).isFalse();
        assertThat(LedgerAccountType.internalTypes())
                .doesNotContain(LedgerAccountType.WALLET)
                .hasSize(LedgerAccountType.values().length - 1)
                .allMatch(LedgerAccountType::isInternal);
    }
}
