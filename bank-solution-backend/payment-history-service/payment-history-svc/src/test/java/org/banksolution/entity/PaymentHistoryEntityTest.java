package org.banksolution.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentHistoryFixtures.CUSTOMER_ID;
import static org.banksolution.fixtures.PaymentHistoryFixtures.createPaymentHistoryEntity;

class PaymentHistoryEntityTest {

    @Test
    void shouldStampCreationAndUpdateTimestampsBeforePersisting() {
        PaymentHistoryEntity paymentHistoryEntity = createPaymentHistoryEntity(UUID.randomUUID(), CUSTOMER_ID);

        paymentHistoryEntity.onCreate();

        assertThat(paymentHistoryEntity.getCreatedAt()).isNotNull();
        assertThat(paymentHistoryEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRefreshOnlyTheUpdateTimestampBeforeUpdating() {
        PaymentHistoryEntity paymentHistoryEntity = createPaymentHistoryEntity(UUID.randomUUID(), CUSTOMER_ID);
        paymentHistoryEntity.onCreate();
        Instant createdAtBeforeUpdate = paymentHistoryEntity.getCreatedAt();

        paymentHistoryEntity.onUpdate();

        assertThat(paymentHistoryEntity.getCreatedAt()).isEqualTo(createdAtBeforeUpdate);
        assertThat(paymentHistoryEntity.getUpdatedAt()).isAfterOrEqualTo(createdAtBeforeUpdate);
    }
}
