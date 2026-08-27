package org.banksolution.mapper;

import org.banksolution.dto.PaymentHistoryResponse;
import org.banksolution.entity.PaymentHistoryEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentHistoryFixtures.*;

class PaymentHistoryResponseMapperTest {

    @Test
    void shouldCopyEveryColumnIntoTheResponse() {
        UUID paymentId = UUID.randomUUID();
        PaymentHistoryEntity paymentHistoryEntity = createPaymentHistoryEntity(paymentId, CUSTOMER_ID);
        paymentHistoryEntity.setMarlAssessment(createMarlAssessment());
        paymentHistoryEntity.setMarlProcessingTimeMs(34L);
        paymentHistoryEntity.setManualReviewedBy(OFFICER);
        paymentHistoryEntity.setDecisionOverriddenBy(OFFICER);
        paymentHistoryEntity.setCreatedAt(INITIATED_AT);
        paymentHistoryEntity.setUpdatedAt(COMPLETED_AT);

        PaymentHistoryResponse paymentHistoryResponse = PaymentHistoryResponseMapper.toPaymentHistoryResponse(paymentHistoryEntity);

        assertThat(paymentHistoryResponse.getPaymentId()).isEqualTo(paymentId);
        assertThat(paymentHistoryResponse.getReferenceNumber()).isEqualTo(paymentHistoryEntity.getReferenceNumber());
        assertThat(paymentHistoryResponse.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(paymentHistoryResponse.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(paymentHistoryResponse.getDestinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID);
        assertThat(paymentHistoryResponse.getAmount()).isEqualByComparingTo("100.00");
        assertThat(paymentHistoryResponse.getFromCurrency()).isEqualTo("GBP");
        assertThat(paymentHistoryResponse.getToCurrency()).isEqualTo("EUR");
        assertThat(paymentHistoryResponse.getConvertedAmount()).isEqualByComparingTo("116");
        assertThat(paymentHistoryResponse.getAppliedExchangeRate()).isEqualByComparingTo("1.16");
        assertThat(paymentHistoryResponse.getPaymentType()).isEqualTo("TRANSFER_OUT");
        assertThat(paymentHistoryResponse.getDescription()).isEqualTo("Rent");
        assertThat(paymentHistoryResponse.getStatus()).isEqualTo("COMPLETED");
        assertThat(paymentHistoryResponse.getFraudStatus()).isEqualTo("APPROVED");
        assertThat(paymentHistoryResponse.getRiskScore()).isEqualTo(0.10);
        assertThat(paymentHistoryResponse.getRiskLevel()).isEqualTo("LOW");
        assertThat(paymentHistoryResponse.getRiskAction()).isEqualTo("PROCEED");
        assertThat(paymentHistoryResponse.getFraudIndicators()).containsExactly("NONE");
        assertThat(paymentHistoryResponse.getInitiatedAt()).isEqualTo(INITIATED_AT);
        assertThat(paymentHistoryResponse.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertThat(paymentHistoryResponse.getManualReviewedBy()).isEqualTo(OFFICER);
        assertThat(paymentHistoryResponse.getDecisionOverriddenBy()).isEqualTo(OFFICER);
        assertThat(paymentHistoryResponse.getRiskProcessingTimeMs()).isEqualTo(12L);
        assertThat(paymentHistoryResponse.getMarlProcessingTimeMs()).isEqualTo(34L);
        assertThat(paymentHistoryResponse.getMlModelVersion()).isEqualTo("model-v1");
        assertThat(paymentHistoryResponse.getAggregateVersion()).isEqualTo(7);
        assertThat(paymentHistoryResponse.getCreatedAt()).isEqualTo(INITIATED_AT);
        assertThat(paymentHistoryResponse.getUpdatedAt()).isEqualTo(COMPLETED_AT);
        PaymentHistoryResponse.MarlAssessmentDto marlAssessmentDto = paymentHistoryResponse.getMarlAssessment();
        assertThat(marlAssessmentDto.getAction()).isEqualTo("BLOCK");
        assertThat(marlAssessmentDto.getAgentContributions()).containsEntry("transaction", 1.0);
        assertThat(marlAssessmentDto.getTransactionAgentObservation().getFeatureContributions())
                .singleElement()
                .satisfies(featureContributionDto -> assertThat(featureContributionDto.getFeature()).isEqualTo("amount"));
        assertThat(marlAssessmentDto.getCustomerAgentObservation()).isNull();
        assertThat(marlAssessmentDto.getNetworkAgentObservation()).isNull();
    }

    @Test
    void shouldLeaveTheMarlAssessmentAndUnexplainedFeaturesAbsent() {
        PaymentHistoryEntity paymentHistoryEntity = createPaymentHistoryEntity(UUID.randomUUID(), CUSTOMER_ID);

        assertThat(PaymentHistoryResponseMapper.toPaymentHistoryResponse(paymentHistoryEntity).getMarlAssessment()).isNull();

        PaymentHistoryEntity.MarlAssessment marlAssessment = createMarlAssessment();
        marlAssessment.getTransactionAgentObservation().setFeatureContributions(null);
        paymentHistoryEntity.setMarlAssessment(marlAssessment);

        assertThat(PaymentHistoryResponseMapper.toPaymentHistoryResponse(paymentHistoryEntity)
                .getMarlAssessment().getTransactionAgentObservation().getFeatureContributions()).isNull();
    }
}
