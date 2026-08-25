package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.PaymentType;
import org.banksolution.enums.RiskCheckStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createRiskCheckRequestEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;

class RiskCheckRequestRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private RiskCheckRequestRepository riskCheckRequestRepository;

    @Test
    void shouldPersistAndReloadEveryColumnOfTheMigratedSchema() {
        RiskCheckRequestEntity entity = createTransferRiskCheckRequestEntity();
        entity.setId(null);

        RiskCheckRequestEntity saved = riskCheckRequestRepository.saveAndFlush(entity);
        RiskCheckRequestEntity reloaded = riskCheckRequestRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getPaymentId()).isEqualTo(entity.getPaymentId());
        assertThat(reloaded.getCustomerId()).isEqualTo(entity.getCustomerId());
        assertThat(reloaded.getSourceAccountId()).isEqualTo(entity.getSourceAccountId());
        assertThat(reloaded.getDestinationAccountId()).isEqualTo(entity.getDestinationAccountId());
        assertThat(reloaded.getAmount()).isEqualByComparingTo(entity.getAmount());
        assertThat(reloaded.getFromCurrency()).isEqualTo(entity.getFromCurrency());
        assertThat(reloaded.getToCurrency()).isEqualTo(entity.getToCurrency());
        assertThat(reloaded.getPaymentType()).isEqualTo(entity.getPaymentType());
        assertThat(reloaded.getDescription()).isEqualTo(entity.getDescription());
        assertThat(reloaded.getRequestTimestamp()).isEqualTo(entity.getRequestTimestamp());
        assertThat(reloaded.getStatus()).isEqualTo(RiskCheckStatus.AWAITING_MARL);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldKeepAllFourAmountDecimalsThroughThePostgresColumn() {
        RiskCheckRequestEntity entity = createTransferRiskCheckRequestEntity();
        entity.setId(null);
        entity.setAmount(new BigDecimal("9999999999999.1234"));

        UUID savedId = riskCheckRequestRepository.saveAndFlush(entity).getId();

        assertThat(riskCheckRequestRepository.findById(savedId).orElseThrow().getAmount())
                .isEqualByComparingTo(new BigDecimal("9999999999999.1234"));
    }

    @Test
    void shouldAcceptEveryPaymentTypeAgainstTheCheckConstraint() {
        for (PaymentType paymentType : PaymentType.values()) {
            RiskCheckRequestEntity entity = createRiskCheckRequestEntity(
                    paymentType, UUID.randomUUID().toString(), UUID.randomUUID().toString());
            entity.setId(null);

            UUID savedId = riskCheckRequestRepository.saveAndFlush(entity).getId();

            assertThat(riskCheckRequestRepository.findById(savedId).orElseThrow().getPaymentType())
                    .isEqualTo(paymentType);
        }
    }

    @Test
    void shouldBumpTheOptimisticLockVersionWhenTheStatusIsUpdated() {
        RiskCheckRequestEntity entity = createTransferRiskCheckRequestEntity();
        entity.setId(null);
        UUID savedId = riskCheckRequestRepository.saveAndFlush(entity).getId();

        RiskCheckRequestEntity persisted = riskCheckRequestRepository.findById(savedId).orElseThrow();
        short versionBefore = persisted.getVersion();
        persisted.setStatus(RiskCheckStatus.COMPLETED);
        riskCheckRequestRepository.saveAndFlush(persisted);

        RiskCheckRequestEntity updated = riskCheckRequestRepository.findById(savedId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RiskCheckStatus.COMPLETED);
        assertThat(updated.getVersion()).isEqualTo((short) (versionBefore + 1));
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
    }

    @Test
    void shouldOnlyReportExistingPaymentIds() {
        RiskCheckRequestEntity entity = createTransferRiskCheckRequestEntity();
        entity.setId(null);
        riskCheckRequestRepository.saveAndFlush(entity);

        assertThat(riskCheckRequestRepository.existsByPaymentId(entity.getPaymentId())).isTrue();
        assertThat(riskCheckRequestRepository.existsByPaymentId("PAY-unknown-" + UUID.randomUUID())).isFalse();
    }
}
