package org.banksolution.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerAddress;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerEntity;
import static org.banksolution.fixtures.CustomerFixtures.createUniqueEmail;

class BaseEntityTest {

    @Test
    void shouldStampCreationAndUpdateTimestampsBeforePersisting() {
        CustomerAddress customerAddress = createCustomerAddress();

        customerAddress.prePersist();

        assertThat(customerAddress.getCreatedAt()).isNotNull();
        assertThat(customerAddress.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRefreshOnlyTheUpdateTimestampBeforeUpdating() {
        CustomerAddress customerAddress = createCustomerAddress();
        customerAddress.prePersist();
        Instant createdAtBeforeUpdate = customerAddress.getCreatedAt();

        customerAddress.preUpdate();

        assertThat(customerAddress.getCreatedAt()).isEqualTo(createdAtBeforeUpdate);
        assertThat(customerAddress.getUpdatedAt()).isAfterOrEqualTo(createdAtBeforeUpdate);
    }

    @Test
    void shouldStampTheDeletionTimestampBeforeRemoving() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());

        customerEntity.preRemove();

        assertThat(customerEntity.getDeletedAt()).isNotNull();
    }
}
