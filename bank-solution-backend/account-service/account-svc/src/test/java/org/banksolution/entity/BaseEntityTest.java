package org.banksolution.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.AccountFixtures.createAccountEntity;

class BaseEntityTest {

    @Test
    void shouldStampCreationAndUpdateTimestampsBeforePersisting() {
        AccountEntity accountEntity = createAccountEntity(UUID.randomUUID());

        accountEntity.prePersist();

        assertThat(accountEntity.getCreatedAt()).isNotNull();
        assertThat(accountEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRefreshOnlyTheUpdateTimestampBeforeUpdating() {
        AccountEntity accountEntity = createAccountEntity(UUID.randomUUID());
        accountEntity.prePersist();
        Instant createdAtBeforeUpdate = accountEntity.getCreatedAt();

        accountEntity.preUpdate();

        assertThat(accountEntity.getCreatedAt()).isEqualTo(createdAtBeforeUpdate);
        assertThat(accountEntity.getUpdatedAt()).isAfterOrEqualTo(createdAtBeforeUpdate);
    }

    @Test
    void shouldStampTheDeletionTimestampBeforeRemoving() {
        AccountEntity accountEntity = createAccountEntity(UUID.randomUUID());

        accountEntity.preRemove();

        assertThat(accountEntity.getDeletedAt()).isNotNull();
    }
}
