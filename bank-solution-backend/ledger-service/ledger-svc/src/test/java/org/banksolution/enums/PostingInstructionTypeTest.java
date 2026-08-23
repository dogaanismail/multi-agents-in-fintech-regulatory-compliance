package org.banksolution.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.enums.PostingInstructionType.*;

class PostingInstructionTypeTest {

    private static final int UNKNOWN_CODE = 99;

    @Test
    void shouldHaveUniqueCodes() {
        assertThat(values()).extracting(PostingInstructionType::getCode).doesNotHaveDuplicates();
    }

    @Test
    void shouldNeverUseZeroAsCode() {
        assertThat(values()).allMatch(type -> type.getCode() > 0);
    }

    @ParameterizedTest
    @EnumSource(PostingInstructionType.class)
    void shouldResolveEveryTypeFromItsCode(PostingInstructionType type) {
        assertThat(fromCode(type.getCode())).isEqualTo(type);
    }

    @Test
    void shouldRejectUnknownCode() {
        assertThatThrownBy(() -> fromCode(UNKNOWN_CODE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown posting instruction type code");
    }

    @Test
    void shouldMapAuthorisationsToPendingTransfers() {
        assertThat(INBOUND_AUTHORISATION.getTransferType()).isEqualTo(TransferType.PENDING);
        assertThat(OUTBOUND_AUTHORISATION.getTransferType()).isEqualTo(TransferType.PENDING);
    }

    @Test
    void shouldMapSettlementAndReleaseToTheirPendingResolutions() {
        assertThat(SETTLEMENT.getTransferType()).isEqualTo(TransferType.POST_PENDING);
        assertThat(RELEASE.getTransferType()).isEqualTo(TransferType.VOID_PENDING);
    }

    @Test
    void shouldMapHardSettlementsToSinglePhaseTransfers() {
        assertThat(INBOUND_HARD_SETTLEMENT.getTransferType()).isEqualTo(TransferType.SINGLE_PHASE);
        assertThat(OUTBOUND_HARD_SETTLEMENT.getTransferType()).isEqualTo(TransferType.SINGLE_PHASE);
    }

    @Test
    void shouldTreatOnlyAuthorisationsAsAuthorisations() {
        assertThat(values())
                .filteredOn(PostingInstructionType::isAuthorisation)
                .containsExactlyInAnyOrder(
                        INBOUND_AUTHORISATION,
                        OUTBOUND_AUTHORISATION,
                        INTERNAL_TRANSFER_AUTHORISATION,
                        CROSS_CURRENCY_TRANSFER_AUTHORISATION);
    }

    @Test
    void shouldMapInternalTransferAuthorisationToAPendingTransfer() {
        assertThat(INTERNAL_TRANSFER_AUTHORISATION.getTransferType()).isEqualTo(TransferType.PENDING);
    }

    @Test
    void shouldFlagOnlyInternalTransferAsMovingBetweenCustomerWallets() {
        assertThat(values())
                .filteredOn(PostingInstructionType::movesBetweenCustomerWallets)
                .containsExactly(INTERNAL_TRANSFER_AUTHORISATION);
    }

    @Test
    void shouldFlagOnlyInboundMovementsAsInbound() {
        assertThat(values())
                .filteredOn(PostingInstructionType::isInbound)
                .containsExactlyInAnyOrder(INBOUND_AUTHORISATION, INBOUND_HARD_SETTLEMENT);
    }

    @Test
    void shouldFlagOnlyCrossCurrencyTransferAsCrossingCurrencies() {
        assertThat(values())
                .filteredOn(PostingInstructionType::crossesCurrencies)
                .containsExactly(CROSS_CURRENCY_TRANSFER_AUTHORISATION);
    }
}
