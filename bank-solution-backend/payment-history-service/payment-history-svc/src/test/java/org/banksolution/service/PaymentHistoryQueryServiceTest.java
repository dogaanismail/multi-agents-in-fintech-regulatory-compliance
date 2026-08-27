package org.banksolution.service;

import org.banksolution.dto.PaymentHistoryResponse;
import org.banksolution.entity.PaymentHistoryEntity;
import org.banksolution.repository.PaymentHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentHistoryFixtures.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryQueryServiceTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @InjectMocks
    private PaymentHistoryQueryService paymentHistoryQueryService;

    @Test
    void shouldMapASinglePaymentOrReportItMissing() {
        UUID paymentId = UUID.randomUUID();
        when(paymentHistoryRepository.findById(paymentId)).thenReturn(Optional.of(createPaymentHistoryEntity(paymentId, CUSTOMER_ID)));
        UUID unknownPaymentId = UUID.randomUUID();
        when(paymentHistoryRepository.findById(unknownPaymentId)).thenReturn(Optional.empty());

        assertThat(paymentHistoryQueryService.getPaymentHistoryById(paymentId)).map(PaymentHistoryResponse::getPaymentId).contains(paymentId);
        assertThat(paymentHistoryQueryService.getPaymentHistoryById(unknownPaymentId)).isEmpty();
    }

    @Test
    void shouldMapEveryPagedLookupOntoResponses() {
        Page<PaymentHistoryEntity> paymentHistoryEntities =
                new PageImpl<>(List.of(createPaymentHistoryEntity(UUID.randomUUID(), CUSTOMER_ID)), FIRST_PAGE, 1);
        when(paymentHistoryRepository.findByCustomerId(CUSTOMER_ID, FIRST_PAGE)).thenReturn(paymentHistoryEntities);
        when(paymentHistoryRepository.findByCustomerIdAndDateRange(CUSTOMER_ID, INITIATED_AT, COMPLETED_AT, FIRST_PAGE)).thenReturn(paymentHistoryEntities);
        when(paymentHistoryRepository.findByStatus("COMPLETED", FIRST_PAGE)).thenReturn(paymentHistoryEntities);
        when(paymentHistoryRepository.findByFraudStatus("APPROVED", FIRST_PAGE)).thenReturn(paymentHistoryEntities);
        when(paymentHistoryRepository.findByRiskLevel("LOW", FIRST_PAGE)).thenReturn(paymentHistoryEntities);
        when(paymentHistoryRepository.findByDateRange(INITIATED_AT, COMPLETED_AT, FIRST_PAGE)).thenReturn(paymentHistoryEntities);
        when(paymentHistoryRepository.findAll(FIRST_PAGE)).thenReturn(paymentHistoryEntities);

        assertThat(paymentHistoryQueryService.getCustomerPaymentHistory(CUSTOMER_ID, FIRST_PAGE).getContent()).hasSize(1);
        assertThat(paymentHistoryQueryService.getCustomerPaymentHistoryByDateRange(CUSTOMER_ID, INITIATED_AT, COMPLETED_AT, FIRST_PAGE).getContent()).hasSize(1);
        assertThat(paymentHistoryQueryService.getPaymentHistoryByStatus("COMPLETED", FIRST_PAGE).getContent()).hasSize(1);
        assertThat(paymentHistoryQueryService.getPaymentHistoryByFraudStatus("APPROVED", FIRST_PAGE).getContent()).hasSize(1);
        assertThat(paymentHistoryQueryService.getPaymentHistoryByRiskLevel("LOW", FIRST_PAGE).getContent()).hasSize(1);
        assertThat(paymentHistoryQueryService.getPaymentHistoryByDateRange(INITIATED_AT, COMPLETED_AT, FIRST_PAGE).getContent()).hasSize(1);
        assertThat(paymentHistoryQueryService.getAllPaymentHistory(FIRST_PAGE).getContent())
                .singleElement()
                .satisfies(paymentHistoryResponse -> assertThat(paymentHistoryResponse.getCustomerId()).isEqualTo(CUSTOMER_ID));
    }
}
