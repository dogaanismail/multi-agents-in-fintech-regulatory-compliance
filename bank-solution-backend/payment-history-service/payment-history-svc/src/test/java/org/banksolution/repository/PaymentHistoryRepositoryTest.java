package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.PaymentHistoryEntity;
import org.banksolution.enums.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentHistoryFixtures.*;

class PaymentHistoryRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Test
    void shouldPersistAndReloadEveryColumnIncludingTheJsonbOnes() {
        UUID paymentId = UUID.randomUUID();
        PaymentHistoryEntity paymentHistoryEntity = createPaymentHistoryEntity(paymentId, CUSTOMER_ID);
        paymentHistoryEntity.setMarlAssessment(createMarlAssessment());

        paymentHistoryRepository.saveAndFlush(paymentHistoryEntity);
        PaymentHistoryEntity reloadedPaymentHistoryEntity = paymentHistoryRepository.findById(paymentId).orElseThrow();

        assertThat(reloadedPaymentHistoryEntity.getReferenceNumber()).isEqualTo(paymentHistoryEntity.getReferenceNumber());
        assertThat(reloadedPaymentHistoryEntity.getAmount()).isEqualByComparingTo("100.00");
        assertThat(reloadedPaymentHistoryEntity.getConvertedAmount()).isEqualByComparingTo("116.0000");
        assertThat(reloadedPaymentHistoryEntity.getAppliedExchangeRate()).isEqualByComparingTo("1.16000000");
        assertThat(reloadedPaymentHistoryEntity.getFraudIndicators()).containsExactly("NONE");
        assertThat(reloadedPaymentHistoryEntity.getMarlAssessment().getRequestId()).isEqualTo("marl-req-1");
        assertThat(reloadedPaymentHistoryEntity.getMarlAssessment().getTransactionAgentObservation().getFeatureContributions())
                .singleElement()
                .satisfies(featureContribution -> assertThat(featureContribution.getShapValue()).isEqualTo(0.31));
        assertThat(reloadedPaymentHistoryEntity.getMarlAssessment().getAgentContributions()).containsEntry("transaction", 1.0);
        assertThat(reloadedPaymentHistoryEntity.getInitiatedAt()).isEqualTo(INITIATED_AT);
        assertThat(reloadedPaymentHistoryEntity.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertThat(reloadedPaymentHistoryEntity.getCreatedAt()).isNotNull();
        assertThat(reloadedPaymentHistoryEntity.getEntityVersion()).isZero();
    }

    @Test
    void shouldAcceptEveryCurrencyAgainstTheCheckConstraints() {
        for (Currency currency : Currency.values()) {
            PaymentHistoryEntity paymentHistoryEntity = createPaymentHistoryEntity(UUID.randomUUID(), CUSTOMER_ID);
            paymentHistoryEntity.setFromCurrency(currency.name());
            paymentHistoryEntity.setToCurrency(currency.name());

            paymentHistoryRepository.saveAndFlush(paymentHistoryEntity);
        }
    }

    @Test
    void shouldBumpTheEntityVersionWhenASnapshotUpdatesTheRow() {
        UUID paymentId = UUID.randomUUID();
        paymentHistoryRepository.saveAndFlush(createPaymentHistoryEntity(paymentId, CUSTOMER_ID));

        PaymentHistoryEntity persistedPaymentHistoryEntity = paymentHistoryRepository.findById(paymentId).orElseThrow();
        persistedPaymentHistoryEntity.setStatus("OVERRIDE_APPROVED");
        paymentHistoryRepository.saveAndFlush(persistedPaymentHistoryEntity);

        PaymentHistoryEntity updatedPaymentHistoryEntity = paymentHistoryRepository.findById(paymentId).orElseThrow();
        assertThat(updatedPaymentHistoryEntity.getEntityVersion()).isEqualTo((short) 1);
        assertThat(updatedPaymentHistoryEntity.getUpdatedAt()).isAfterOrEqualTo(updatedPaymentHistoryEntity.getCreatedAt());
    }

    @Test
    void shouldRejectADuplicateReferenceNumberAndANonPositiveAmount() {
        PaymentHistoryEntity paymentHistoryEntity = paymentHistoryRepository.saveAndFlush(createPaymentHistoryEntity(UUID.randomUUID(), CUSTOMER_ID));
        PaymentHistoryEntity duplicateReferenceEntity = createPaymentHistoryEntity(UUID.randomUUID(), CUSTOMER_ID);
        duplicateReferenceEntity.setReferenceNumber(paymentHistoryEntity.getReferenceNumber());
        PaymentHistoryEntity zeroAmountEntity = createPaymentHistoryEntity(UUID.randomUUID(), CUSTOMER_ID);
        zeroAmountEntity.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> paymentHistoryRepository.saveAndFlush(duplicateReferenceEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> paymentHistoryRepository.saveAndFlush(zeroAmountEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFilterByCustomerStatusFraudStatusRiskLevelAndDateRange() {
        UUID customerId = UUID.randomUUID();
        PaymentHistoryEntity completedPaymentHistoryEntity = createPaymentHistoryEntity(UUID.randomUUID(), customerId);
        PaymentHistoryEntity blockedPaymentHistoryEntity = createPaymentHistoryEntity(UUID.randomUUID(), customerId);
        blockedPaymentHistoryEntity.setStatus("BLOCKED");
        blockedPaymentHistoryEntity.setFraudStatus("BLOCKED");
        blockedPaymentHistoryEntity.setRiskLevel("HIGH");

        paymentHistoryRepository.saveAndFlush(completedPaymentHistoryEntity);
        paymentHistoryRepository.saveAndFlush(blockedPaymentHistoryEntity);

        Instant windowStart = completedPaymentHistoryEntity.getCreatedAt().minusSeconds(60);
        Instant windowEnd = blockedPaymentHistoryEntity.getCreatedAt().plusSeconds(60);
        PageRequest firstPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        assertThat(paymentHistoryRepository.findByCustomerId(customerId, firstPage).getTotalElements()).isEqualTo(2);
        assertThat(paymentHistoryRepository.findByStatus("BLOCKED", firstPage).getContent())
                .extracting(PaymentHistoryEntity::getPaymentId).contains(blockedPaymentHistoryEntity.getPaymentId())
                .doesNotContain(completedPaymentHistoryEntity.getPaymentId());
        assertThat(paymentHistoryRepository.findByFraudStatus("BLOCKED", firstPage).getContent())
                .extracting(PaymentHistoryEntity::getPaymentId).contains(blockedPaymentHistoryEntity.getPaymentId());
        assertThat(paymentHistoryRepository.findByRiskLevel("HIGH", firstPage).getContent())
                .extracting(PaymentHistoryEntity::getPaymentId).contains(blockedPaymentHistoryEntity.getPaymentId());
        assertThat(paymentHistoryRepository.findByCustomerIdAndDateRange(customerId, windowStart, windowEnd, firstPage).getTotalElements()).isEqualTo(2);
        assertThat(paymentHistoryRepository.findByCustomerIdAndDateRange(customerId, windowEnd, windowEnd.plusSeconds(1), firstPage).getTotalElements()).isZero();
        assertThat(paymentHistoryRepository.findByDateRange(windowStart, windowEnd, firstPage).getTotalElements()).isGreaterThanOrEqualTo(2);
    }
}
