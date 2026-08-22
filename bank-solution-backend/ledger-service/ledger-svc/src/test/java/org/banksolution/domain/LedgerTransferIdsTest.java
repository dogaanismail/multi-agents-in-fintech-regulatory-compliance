package org.banksolution.domain;

import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.enums.PostingInstructionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.domain.LedgerTransferIds.deriveTransferId;
import static org.banksolution.enums.PostingInstructionType.OUTBOUND_AUTHORISATION;
import static org.banksolution.enums.PostingInstructionType.SETTLEMENT;

class LedgerTransferIdsTest {

    private static final UUID CLIENT_TRANSACTION_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @ParameterizedTest
    @EnumSource(PostingInstructionType.class)
    void shouldDeriveTheSameTransferIdForTheSameInputs(PostingInstructionType postingInstructionType) {
        assertThat(deriveTransferId(CLIENT_TRANSACTION_ID, postingInstructionType))
                .isEqualTo(deriveTransferId(CLIENT_TRANSACTION_ID, postingInstructionType));
    }

    @Test
    void shouldDeriveDistinctTransferIdsPerPostingInstructionType() {
        assertThat(Arrays.stream(PostingInstructionType.values())
                .map(type -> deriveTransferId(CLIENT_TRANSACTION_ID, type)))
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldDeriveDistinctTransferIdsPerClientTransaction() {
        assertThat(deriveTransferId(CLIENT_TRANSACTION_ID, SETTLEMENT))
                .isNotEqualTo(deriveTransferId(UUID.randomUUID(), SETTLEMENT));
    }

    @Test
    void shouldNotCollideWithLedgerAccountIds() {
        assertThat(deriveTransferId(CLIENT_TRANSACTION_ID, OUTBOUND_AUTHORISATION))
                .isNotEqualTo(LedgerAccountIds.deriveWalletAccountId(CLIENT_TRANSACTION_ID, Currency.GBP))
                .isNotEqualTo(LedgerAccountIds.deriveInternalAccountId(
                        LedgerAccountType.OUTBOUND_CLEARING, Currency.GBP));
    }
}
