package org.banksolution.repository;

import com.tigerbeetle.CreateAccountStatus;
import com.tigerbeetle.CreateTransferStatus;
import org.banksolution.enums.TransferType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TigerBeetleStatusesTest {

    @Test
    void shouldTreatACreatedOrAlreadyExistingAccountAsPersisted() {
        assertThat(TigerBeetleStatuses.isAccountPersisted(CreateAccountStatus.Created)).isTrue();
        assertThat(TigerBeetleStatuses.isAccountPersisted(CreateAccountStatus.Exists)).isTrue();
        assertThat(TigerBeetleStatuses.isAccountPersisted(CreateAccountStatus.ExistsWithDifferentLedger)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TransferType.class)
    void shouldTreatACreatedOrRedeliveredTransferAsPersistedForEveryTransferType(TransferType transferType) {
        assertThat(TigerBeetleStatuses.isTransferPersisted(CreateTransferStatus.Created, transferType)).isTrue();
        assertThat(TigerBeetleStatuses.isTransferPersisted(CreateTransferStatus.Exists, transferType)).isTrue();
    }

    @Test
    void shouldTreatAnAlreadyResolvedAuthorisationAsPersistedOnlyWhenResolvedTheSameWay() {
        assertThat(TigerBeetleStatuses.isTransferPersisted(CreateTransferStatus.PendingTransferAlreadyPosted, TransferType.POST_PENDING)).isTrue();
        assertThat(TigerBeetleStatuses.isTransferPersisted(CreateTransferStatus.PendingTransferAlreadyVoided, TransferType.VOID_PENDING)).isTrue();
        assertThat(TigerBeetleStatuses.isTransferPersisted(CreateTransferStatus.PendingTransferAlreadyVoided, TransferType.POST_PENDING)).isFalse();
        assertThat(TigerBeetleStatuses.isTransferPersisted(CreateTransferStatus.PendingTransferAlreadyPosted, TransferType.VOID_PENDING)).isFalse();
    }

    @Test
    void shouldClassifyEachRejectionSeparately() {
        assertThat(TigerBeetleStatuses.isTransferPersisted(CreateTransferStatus.ExceedsCredits, TransferType.PENDING)).isFalse();
        assertThat(TigerBeetleStatuses.isInsufficientFunds(CreateTransferStatus.ExceedsCredits)).isTrue();
        assertThat(TigerBeetleStatuses.isInsufficientFunds(CreateTransferStatus.PendingTransferNotFound)).isFalse();
        assertThat(TigerBeetleStatuses.isPendingTransferMissing(CreateTransferStatus.PendingTransferNotFound)).isTrue();
        assertThat(TigerBeetleStatuses.isPendingTransferMissing(CreateTransferStatus.AccountsMustBeDifferent)).isFalse();
        assertThat(TigerBeetleStatuses.isPendingTransferAlreadyResolved(CreateTransferStatus.PendingTransferAlreadyPosted)).isTrue();
        assertThat(TigerBeetleStatuses.isPendingTransferAlreadyResolved(CreateTransferStatus.PendingTransferAlreadyVoided)).isTrue();
        assertThat(TigerBeetleStatuses.isPendingTransferAlreadyResolved(CreateTransferStatus.Exists)).isFalse();
    }
}
