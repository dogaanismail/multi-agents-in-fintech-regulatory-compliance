package org.banksolution.entity;

import org.banksolution.enums.Currency;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.AccountFixtures.createAccountWalletEntity;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountEntity;

/**
 * Account and wallet reference each other; Lombok's generated hashCode must leave the
 * back-reference out or hashing either side recurses until the stack overflows.
 */
class AccountEntityTest {

    @Test
    void shouldHashAnAccountAndItsWalletsWithoutRecursingThroughTheBackReference() {
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        AccountWalletEntity accountWalletEntity = createAccountWalletEntity(accountEntity, Currency.GBP, true);
        accountEntity.setWallets(List.of(accountWalletEntity));

        assertThat(accountEntity.hashCode()).isEqualTo(accountEntity.hashCode());
        assertThat(accountWalletEntity.hashCode()).isEqualTo(accountWalletEntity.hashCode());
        assertThat(accountEntity).isEqualTo(accountEntity);
        assertThat(accountWalletEntity).isEqualTo(accountWalletEntity);
    }
}
