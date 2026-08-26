package org.banksolution.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentRequestEntity;

class BaseEntityTest {

    @Test
    void shouldStampCreationAndUpdateTimestampsBeforePersisting() {
        PaymentRequestEntity paymentRequestEntity = createPaymentRequestEntity(UUID.randomUUID());

        paymentRequestEntity.prePersist();

        assertThat(paymentRequestEntity.getCreatedAt()).isNotNull();
        assertThat(paymentRequestEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRefreshOnlyTheUpdateTimestampBeforeUpdating() {
        PaymentRequestEntity paymentRequestEntity = createPaymentRequestEntity(UUID.randomUUID());
        paymentRequestEntity.prePersist();
        Instant createdAtBeforeUpdate = paymentRequestEntity.getCreatedAt();

        paymentRequestEntity.preUpdate();

        assertThat(paymentRequestEntity.getCreatedAt()).isEqualTo(createdAtBeforeUpdate);
        assertThat(paymentRequestEntity.getUpdatedAt()).isAfterOrEqualTo(createdAtBeforeUpdate);
    }
}
