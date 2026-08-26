package org.banksolution.repository;

import com.tigerbeetle.CreateAccountStatus;
import com.tigerbeetle.CreateTransferStatus;
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
    @EnumSource(value = CreateTransferStatus.class, names = {
            "Created", "Exists", "PendingTransferAlreadyPosted", "PendingTransferAlreadyVoided"})
    void shouldTreatARedeliveredTransferAsPersisted(CreateTransferStatus createTransferStatus) {
        assertThat(TigerBeetleStatuses.isTransferPersisted(createTransferStatus)).isTrue();
    }

    @Test
    void shouldClassifyEachRejectionSeparately() {
        assertThat(TigerBeetleStatuses.isTransferPersisted(CreateTransferStatus.ExceedsCredits)).isFalse();
        assertThat(TigerBeetleStatuses.isInsufficientFunds(CreateTransferStatus.ExceedsCredits)).isTrue();
        assertThat(TigerBeetleStatuses.isInsufficientFunds(CreateTransferStatus.PendingTransferNotFound)).isFalse();
        assertThat(TigerBeetleStatuses.isPendingTransferMissing(CreateTransferStatus.PendingTransferNotFound)).isTrue();
        assertThat(TigerBeetleStatuses.isPendingTransferMissing(CreateTransferStatus.AccountsMustBeDifferent)).isFalse();
    }
}
