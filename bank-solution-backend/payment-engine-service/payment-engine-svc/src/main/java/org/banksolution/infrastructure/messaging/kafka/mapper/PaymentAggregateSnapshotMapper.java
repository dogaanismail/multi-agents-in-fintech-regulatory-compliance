package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.aggregate.PaymentAggregate;

import java.time.Instant;

/**
 * Mapper for converting PaymentAggregate to PaymentSnapshotEvent.
 * Uses getter methods instead of reflection (cleaner, faster, no warnings).
 */
@UtilityClass
public class PaymentAggregateSnapshotMapper {

    public static PaymentSnapshotEvent toSnapshot(PaymentAggregate aggregate) {
        PaymentSnapshotEvent.Builder builder = PaymentSnapshotEvent.newBuilder()
                .setPaymentId(aggregate.getPaymentId().toString())
                .setReferenceNumber(aggregate.getReferenceNumber())
                .setCustomerId(aggregate.getCustomerId().toString())
                .setSourceAccountId(aggregate.getSourceAccountId().toString())
                .setDestinationAccountId(aggregate.getDestinationAccountId().toString())
                .setAmount(aggregate.getAmount().toString())
                .setCurrency(aggregate.getCurrency())
                .setPaymentType(aggregate.getPaymentType())
                .setDescription(aggregate.getDescription())
                .setStatus(PaymentStatusMapper.toAvro(aggregate.getStatus()))
                .setFraudStatus(FraudCheckStatusMapper.toAvro(aggregate.getFraudStatus()))
                .setSnapshotTimestamp(Instant.now().toEpochMilli())
                .setEventTrigger("AXON_SNAPSHOT");

        // Map timestamps
        mapTimestamp(builder, "initiatedAt", aggregate.getInitiatedAt());
        mapTimestamp(builder, "riskCheckRequestedAt", aggregate.getRiskCheckRequestedAt());
        mapTimestamp(builder, "riskCheckCompletedAt", aggregate.getRiskCheckCompletedAt());
        mapTimestamp(builder, "completedAt", aggregate.getCompletedAt());
        mapTimestamp(builder, "blockedAt", aggregate.getBlockedAt());
        mapTimestamp(builder, "manualReviewRequestedAt", aggregate.getManualReviewRequestedAt());

        // Map risk assessment if available
        if (aggregate.getRiskAssessment() != null) {
            builder.setRiskAssessment(RiskAssessmentSnapshotMapper.toSnapshot(aggregate.getRiskAssessment()));
        }

        return builder.build();
    }

    private static void mapTimestamp(PaymentSnapshotEvent.Builder builder, String fieldName, Instant timestamp) {
        if (timestamp != null) {
            long epochMilli = timestamp.toEpochMilli();
            switch (fieldName) {
                case "initiatedAt" -> builder.setInitiatedAt(epochMilli);
                case "riskCheckRequestedAt" -> builder.setRiskCheckRequestedAt(epochMilli);
                case "riskCheckCompletedAt" -> builder.setRiskCheckCompletedAt(epochMilli);
                case "completedAt" -> builder.setCompletedAt(epochMilli);
                case "blockedAt" -> builder.setBlockedAt(epochMilli);
                case "manualReviewRequestedAt" -> builder.setManualReviewRequestedAt(epochMilli);
            }
        }
    }
}
