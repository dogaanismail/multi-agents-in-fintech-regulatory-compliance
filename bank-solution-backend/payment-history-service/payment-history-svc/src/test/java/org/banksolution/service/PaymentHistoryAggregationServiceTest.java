package org.banksolution.service;

import org.banksolution.entity.PaymentHistoryEntity;
import org.banksolution.repository.PaymentHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentHistoryFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryAggregationServiceTest {

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @InjectMocks
    private PaymentHistoryAggregationService paymentHistoryAggregationService;

    @Test
    void shouldCreateTheHistoryRowForAPaymentSeenForTheFirstTime() {
        UUID paymentId = UUID.randomUUID();
        when(paymentHistoryRepository.findById(paymentId)).thenReturn(Optional.empty());

        paymentHistoryAggregationService.processPaymentSnapshotEvent(createInitiatedPaymentSnapshotEvent(paymentId, CUSTOMER_ID));

        ArgumentCaptor<PaymentHistoryEntity> paymentHistoryEntityCaptor = ArgumentCaptor.forClass(PaymentHistoryEntity.class);
        verify(paymentHistoryRepository).save(paymentHistoryEntityCaptor.capture());
        assertThat(paymentHistoryEntityCaptor.getValue().getPaymentId()).isEqualTo(paymentId);
        assertThat(paymentHistoryEntityCaptor.getValue().getStatus()).isEqualTo("INITIATED");
        assertThat(paymentHistoryEntityCaptor.getValue().getEntityVersion()).isNull();
    }

    @Test
    void shouldIgnoreASnapshotOlderThanTheStoredAggregateVersion() {
        UUID paymentId = UUID.randomUUID();
        PaymentHistoryEntity existingPaymentHistoryEntity = createPaymentHistoryEntity(paymentId, CUSTOMER_ID);
        when(paymentHistoryRepository.findById(paymentId)).thenReturn(Optional.of(existingPaymentHistoryEntity));

        paymentHistoryAggregationService.processPaymentSnapshotEvent(createInitiatedPaymentSnapshotEvent(paymentId, CUSTOMER_ID));

        verify(paymentHistoryRepository, never()).save(any());
        assertThat(existingPaymentHistoryEntity.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldStillApplyASnapshotWhenTheStoredRowHasNoAggregateVersion() {
        UUID paymentId = UUID.randomUUID();
        PaymentHistoryEntity existingPaymentHistoryEntity = createPaymentHistoryEntity(paymentId, CUSTOMER_ID);
        existingPaymentHistoryEntity.setAggregateVersion(null);
        when(paymentHistoryRepository.findById(paymentId)).thenReturn(Optional.of(existingPaymentHistoryEntity));

        paymentHistoryAggregationService.processPaymentSnapshotEvent(createInitiatedPaymentSnapshotEvent(paymentId, CUSTOMER_ID));

        verify(paymentHistoryRepository).save(existingPaymentHistoryEntity);
        assertThat(existingPaymentHistoryEntity.getStatus()).isEqualTo("INITIATED");
    }

    @Test
    void shouldOverwriteTheExistingRowWithTheLatestSnapshot() {
        UUID paymentId = UUID.randomUUID();
        PaymentHistoryEntity existingPaymentHistoryEntity = createPaymentHistoryEntity(paymentId, CUSTOMER_ID);
        existingPaymentHistoryEntity.setStatus("INITIATED");
        existingPaymentHistoryEntity.setEntityVersion((short) 3);
        when(paymentHistoryRepository.findById(paymentId)).thenReturn(Optional.of(existingPaymentHistoryEntity));

        paymentHistoryAggregationService.processPaymentSnapshotEvent(createCompletedPaymentSnapshotEvent(paymentId, CUSTOMER_ID));

        verify(paymentHistoryRepository).save(existingPaymentHistoryEntity);
        assertThat(existingPaymentHistoryEntity.getStatus()).isEqualTo("COMPLETED");
        assertThat(existingPaymentHistoryEntity.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertThat(existingPaymentHistoryEntity.getAggregateVersion()).isEqualTo(7);
    }
}
