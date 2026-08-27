package org.banksolution.mapper;

import com.aml.payment.PaymentSnapshotEvent;
import org.banksolution.entity.PaymentHistoryEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentHistoryFixtures.*;

class PaymentSnapshotMapperTest {

    @Test
    void shouldCopyEveryFieldOfACompletedSnapshotOntoTheHistoryRow() {
        UUID paymentId = UUID.randomUUID();
        PaymentSnapshotEvent paymentSnapshotEvent = createCompletedPaymentSnapshotEvent(paymentId, CUSTOMER_ID);
        PaymentHistoryEntity paymentHistoryEntity = new PaymentHistoryEntity();

        PaymentSnapshotMapper.mapSnapshotToHistory(paymentSnapshotEvent, paymentHistoryEntity);

        assertThat(paymentHistoryEntity.getPaymentId()).isEqualTo(paymentId);
        assertThat(paymentHistoryEntity.getReferenceNumber()).isEqualTo(paymentSnapshotEvent.getReferenceNumber());
        assertThat(paymentHistoryEntity.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(paymentHistoryEntity.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(paymentHistoryEntity.getDestinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID);
        assertThat(paymentHistoryEntity.getAmount()).isEqualByComparingTo("100.00");
        assertThat(paymentHistoryEntity.getFromCurrency()).isEqualTo("GBP");
        assertThat(paymentHistoryEntity.getToCurrency()).isEqualTo("EUR");
        assertThat(paymentHistoryEntity.getConvertedAmount()).isEqualByComparingTo("116.00");
        assertThat(paymentHistoryEntity.getAppliedExchangeRate()).isEqualByComparingTo("1.16");
        assertThat(paymentHistoryEntity.getPaymentType()).isEqualTo("TRANSFER_OUT");
        assertThat(paymentHistoryEntity.getDescription()).isEqualTo("Rent");
        assertThat(paymentHistoryEntity.getStatus()).isEqualTo("COMPLETED");
        assertThat(paymentHistoryEntity.getFraudStatus()).isEqualTo("APPROVED");
        assertThat(paymentHistoryEntity.getRiskScore()).isEqualTo(0.95);
        assertThat(paymentHistoryEntity.getRiskLevel()).isEqualTo("HIGH");
        assertThat(paymentHistoryEntity.getRiskAction()).isEqualTo("BLOCK");
        assertThat(paymentHistoryEntity.getFraudIndicators()).containsExactly("VELOCITY", "NEW_PAYEE");
        assertThat(paymentHistoryEntity.getMlModelVersion()).isEqualTo("model-v1");
        assertThat(paymentHistoryEntity.getRiskProcessingTimeMs()).isEqualTo(12L);
        assertThat(paymentHistoryEntity.getMarlProcessingTimeMs()).isEqualTo(34L);
        assertThat(paymentHistoryEntity.getMarlAssessment().getAction()).isEqualTo("BLOCK");
        assertThat(paymentHistoryEntity.getInitiatedAt()).isEqualTo(INITIATED_AT);
        assertThat(paymentHistoryEntity.getRiskCheckRequestedAt()).isEqualTo(INITIATED_AT.plusSeconds(1));
        assertThat(paymentHistoryEntity.getRiskCheckCompletedAt()).isEqualTo(INITIATED_AT.plusSeconds(2));
        assertThat(paymentHistoryEntity.getFraudCheckApprovedAt()).isEqualTo(INITIATED_AT.plusSeconds(2));
        assertThat(paymentHistoryEntity.getLedgerAuthorisationInitiatedAt()).isEqualTo(INITIATED_AT);
        assertThat(paymentHistoryEntity.getLedgerAuthorisedAt()).isEqualTo(INITIATED_AT.plusSeconds(1));
        assertThat(paymentHistoryEntity.getLedgerSettlementInitiatedAt()).isEqualTo(INITIATED_AT.plusSeconds(3));
        assertThat(paymentHistoryEntity.getLedgerSettledAt()).isEqualTo(INITIATED_AT.plusSeconds(4));
        assertThat(paymentHistoryEntity.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertThat(paymentHistoryEntity.getManualReviewRequestedAt()).isNull();
        assertThat(paymentHistoryEntity.getBlockedAt()).isNull();
        assertThat(paymentHistoryEntity.getAggregateVersion()).isEqualTo(7);
    }

    @Test
    void shouldLeaveOptionalPartsAbsentForAFreshlyInitiatedPayment() {
        PaymentHistoryEntity paymentHistoryEntity = new PaymentHistoryEntity();

        PaymentSnapshotMapper.mapSnapshotToHistory(createInitiatedPaymentSnapshotEvent(UUID.randomUUID(), CUSTOMER_ID), paymentHistoryEntity);

        assertThat(paymentHistoryEntity.getAppliedExchangeRate()).isNull();
        assertThat(paymentHistoryEntity.getDescription()).isNull();
        assertThat(paymentHistoryEntity.getRiskScore()).isNull();
        assertThat(paymentHistoryEntity.getFraudIndicators()).isNull();
        assertThat(paymentHistoryEntity.getMarlAssessment()).isNull();
        assertThat(paymentHistoryEntity.getCompletedAt()).isNull();
        assertThat(paymentHistoryEntity.getStatus()).isEqualTo("INITIATED");
        assertThat(paymentHistoryEntity.getAggregateVersion()).isZero();
    }

    @Test
    void shouldCarryTheDecisionAndOverrideMetadataOfABlockedPayment() {
        PaymentHistoryEntity paymentHistoryEntity = new PaymentHistoryEntity();

        PaymentSnapshotMapper.mapSnapshotToHistory(createBlockedPaymentSnapshotEvent(UUID.randomUUID(), CUSTOMER_ID), paymentHistoryEntity);

        assertThat(paymentHistoryEntity.getBlockedAt()).isEqualTo(COMPLETED_AT);
        assertThat(paymentHistoryEntity.getBlockReason()).isEqualTo("Risk level: HIGH, Risk score: 0.95");
        assertThat(paymentHistoryEntity.getManualReviewedBy()).isEqualTo(OFFICER);
        assertThat(paymentHistoryEntity.getManualReviewNotes()).isEqualTo("Confirmed fraud");
        assertThat(paymentHistoryEntity.getManualReviewRejectedAt()).isEqualTo(COMPLETED_AT);
        assertThat(paymentHistoryEntity.getDecisionOverriddenBy()).isEqualTo(OFFICER);
        assertThat(paymentHistoryEntity.getDecisionOverrideReason()).isEqualTo("False positive");
        assertThat(paymentHistoryEntity.getDecisionOverriddenAt()).isEqualTo(COMPLETED_AT.plusSeconds(60));
        assertThat(paymentHistoryEntity.getFailureReason()).isNull();
    }
}
