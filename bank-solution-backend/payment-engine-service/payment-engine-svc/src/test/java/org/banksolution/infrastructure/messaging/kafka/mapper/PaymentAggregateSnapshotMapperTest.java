package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.FraudCheckStatus;
import com.aml.payment.PaymentScheme;
import com.aml.payment.PaymentSnapshotEvent;
import com.aml.payment.RiskAssessmentSnapshot;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.valueobject.AgentObservation;
import org.banksolution.domain.payment.valueobject.MarlAssessment;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;

class PaymentAggregateSnapshotMapperTest {

    @Test
    void shouldSnapshotEveryScalarFieldAndLifecycleTimestamp() {
        PaymentResponse paymentResponse =
                createPaymentResponse(PaymentStatus.COMPLETED, FraudAnalysisStatus.APPROVED, createProceedAssessment());

        PaymentSnapshotEvent paymentSnapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(paymentResponse, "PAYMENT_COMPLETED");

        assertThat(paymentSnapshotEvent.getPaymentId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(paymentSnapshotEvent.getReferenceNumber()).isEqualTo("PAY-11111111");
        assertThat(paymentSnapshotEvent.getCustomerId()).isEqualTo(CUSTOMER_ID.toString());
        assertThat(paymentSnapshotEvent.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID.toString());
        assertThat(paymentSnapshotEvent.getDestinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID.toString());
        assertThat(paymentSnapshotEvent.getAmount()).isEqualTo("100.00");
        assertThat(paymentSnapshotEvent.getConvertedAmount()).isEqualTo("100.00");
        assertThat(paymentSnapshotEvent.getAppliedExchangeRate()).isEqualTo("1.00");
        assertThat(paymentSnapshotEvent.getPaymentScheme()).isEqualTo(PaymentScheme.INTERNAL_TRANSFER);
        assertThat(paymentSnapshotEvent.getPaymentType()).isEqualTo("TRANSFER_OUT");
        assertThat(paymentSnapshotEvent.getStatus()).isEqualTo(com.aml.payment.PaymentStatus.COMPLETED);
        assertThat(paymentSnapshotEvent.getFraudStatus()).isEqualTo(FraudCheckStatus.APPROVED);
        assertThat(paymentSnapshotEvent.getEventTrigger()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(paymentSnapshotEvent.getInitiatedAt()).isEqualTo(INITIATED_AT.toEpochMilli());
        assertThat(paymentSnapshotEvent.getRiskCheckRequestedAt()).isEqualTo(INITIATED_AT.plusSeconds(1).toEpochMilli());
        assertThat(paymentSnapshotEvent.getLedgerSettledAt()).isEqualTo(INITIATED_AT.plusSeconds(4).toEpochMilli());
        assertThat(paymentSnapshotEvent.getCompletedAt()).isEqualTo(COMPLETED_AT.toEpochMilli());
        assertThat(paymentSnapshotEvent.getManualReviewRequestedAt()).isNull();
        assertThat(paymentSnapshotEvent.getBlockedAt()).isNull();
        assertThat(paymentSnapshotEvent.getSnapshotTimestamp()).isPositive();
        assertThat(paymentSnapshotEvent.getVersion()).isEqualTo(7);
        RiskAssessmentSnapshot riskAssessmentSnapshot = paymentSnapshotEvent.getRiskAssessment();
        assertThat(riskAssessmentSnapshot.getRiskScore()).isEqualTo(0.10);
        assertThat(riskAssessmentSnapshot.getRiskAction()).isEqualTo("PROCEED");
        assertThat(riskAssessmentSnapshot.getFraudIndicators()).containsExactly("NONE");
        assertThat(riskAssessmentSnapshot.getMarlAssessment()).isNull();
    }

    @Test
    void shouldSnapshotTheMarlAssessmentWithEveryAgentObservation() {
        PaymentResponse paymentResponse = createPaymentResponse(PaymentStatus.BLOCKED, FraudAnalysisStatus.BLOCKED,
                createRiskAssessmentWithMarl("BLOCK", "HIGH", 0.95));

        PaymentSnapshotEvent paymentSnapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(paymentResponse, "PAYMENT_BLOCKED");

        var marlAssessmentSnapshot = paymentSnapshotEvent.getRiskAssessment().getMarlAssessment();
        assertThat(marlAssessmentSnapshot.getAction()).isEqualTo("BLOCK");
        assertThat(marlAssessmentSnapshot.getMaddpgQValue()).isEqualTo(0.42);
        assertThat(marlAssessmentSnapshot.getProcessingTimeMs()).isEqualTo(34.0);
        assertThat(marlAssessmentSnapshot.getAgentContributions()).containsEntry("network", 0.2);
        assertThat(marlAssessmentSnapshot.getTransactionAgentObservation().getAgentName()).isEqualTo("transaction-pattern-agent");
        assertThat(marlAssessmentSnapshot.getTransactionAgentObservation().getFeatureContributions())
                .singleElement()
                .satisfies(featureContribution -> {
                    assertThat(featureContribution.getFeature()).isEqualTo("amount");
                    assertThat(featureContribution.getShapValue()).isEqualTo(0.31);
                });
        assertThat(marlAssessmentSnapshot.getCustomerAgentObservation().getAgentName()).isEqualTo("customer-risk-agent");
        assertThat(marlAssessmentSnapshot.getNetworkAgentObservation().getShapBaseValue()).isEqualTo(0.05);
    }

    @Test
    void shouldLeaveOptionalPartsAbsentWhenThePaymentCarriesNone() {
        PaymentResponse paymentResponse = new PaymentResponse(
                PAYMENT_UUID.toString(), "PAY-11111111", CUSTOMER_ID.toString(), SOURCE_ACCOUNT_ID.toString(),
                DESTINATION_ACCOUNT_ID.toString(), AMOUNT, FROM_CURRENCY, TO_CURRENCY, AMOUNT, null,
                org.banksolution.enums.PaymentType.DEPOSIT, null, null, false,
                PaymentStatus.INITIATED, FraudAnalysisStatus.PENDING, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        PaymentSnapshotEvent paymentSnapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(paymentResponse, "PAYMENT_INITIATED");

        assertThat(paymentSnapshotEvent.getAppliedExchangeRate()).isNull();
        assertThat(paymentSnapshotEvent.getPaymentScheme()).isNull();
        assertThat(paymentSnapshotEvent.getDescription()).isNull();
        assertThat(paymentSnapshotEvent.getRiskAssessment()).isNull();
        assertThat(paymentSnapshotEvent.getInitiatedAt()).isNull();
        assertThat(paymentSnapshotEvent.getCompletedAt()).isNull();
        assertThat(paymentSnapshotEvent.getVersion()).isZero();
    }

    @Test
    void shouldLeaveFeatureContributionsAbsentWhenAnAgentExplainedNothing() {
        RiskAssessment riskAssessment = getRiskAssessment();

        PaymentSnapshotEvent paymentSnapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(
                createPaymentResponse(PaymentStatus.MANUAL_REVIEW_REQUIRED, FraudAnalysisStatus.REVIEW_REQUIRED, riskAssessment),
                "MANUAL_REVIEW_REQUESTED");

        var marlAssessmentSnapshot = paymentSnapshotEvent.getRiskAssessment().getMarlAssessment();
        assertThat(marlAssessmentSnapshot.getTransactionAgentObservation().getFeatureContributions()).isEmpty();
        assertThat(marlAssessmentSnapshot.getCustomerAgentObservation().getShapBaseValue()).isNull();
        assertThat(marlAssessmentSnapshot.getNetworkAgentObservation().getAgentName()).isEqualTo("transaction-pattern-agent");
    }

    @ParameterizedTest
    @EnumSource(PaymentStatus.class)
    void shouldMapEveryPaymentStatusOntoTheAvroContract(PaymentStatus paymentStatus) {
        PaymentSnapshotEvent paymentSnapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(
                createPaymentResponse(paymentStatus, FraudAnalysisStatus.PENDING, null), "PAYMENT_INITIATED");

        assertThat(paymentSnapshotEvent.getStatus().name()).isEqualTo(paymentStatus.name());
    }

    @ParameterizedTest
    @EnumSource(FraudAnalysisStatus.class)
    void shouldMapEveryFraudStatusOntoTheAvroContract(FraudAnalysisStatus fraudAnalysisStatus) {
        PaymentSnapshotEvent paymentSnapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(
                createPaymentResponse(PaymentStatus.INITIATED, fraudAnalysisStatus, null), "PAYMENT_INITIATED");

        assertThat(paymentSnapshotEvent.getFraudStatus().name()).isEqualTo(fraudAnalysisStatus.name());
    }

    @Test
    void shouldRenderAmountsWithoutScientificNotation() {
        PaymentResponse paymentResponse = createPaymentResponse(PaymentStatus.INITIATED, FraudAnalysisStatus.PENDING, null);
        PaymentSnapshotEvent paymentSnapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(paymentResponse, "PAYMENT_INITIATED");

        assertThat(new BigDecimal(paymentSnapshotEvent.getAmount())).isEqualByComparingTo(AMOUNT);
    }

    private static @NonNull RiskAssessment getRiskAssessment() {
        AgentObservation unexplainedAgentObservation =
                new AgentObservation("transaction-pattern-agent", false, 0.1, 0.1, "LOW", 1.0, null, null);
        MarlAssessment partialMarlAssessment = new MarlAssessment("marl-req-2", "REVIEW", 0.5, 0.1,
                unexplainedAgentObservation, unexplainedAgentObservation, unexplainedAgentObservation,
                Map.of(), 5L, "training");

        return new RiskAssessment("risk-req-2",
                0.5,
                "MEDIUM",
                "ESCALATE",
                List.of(),
                null,
                1L,
                partialMarlAssessment
        );
    }
}
