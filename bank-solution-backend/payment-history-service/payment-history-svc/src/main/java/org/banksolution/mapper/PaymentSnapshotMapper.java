package org.banksolution.mapper;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentHistoryEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.banksolution.mapper.RiskAssessmentSnapshotMapper.mapRiskAssessment;

@UtilityClass
public class PaymentSnapshotMapper {

    public static void mapSnapshotToHistory(PaymentSnapshotEvent snapshot, PaymentHistoryEntity history) {
        // Basic payment information
        history.setPaymentId(UUID.fromString(snapshot.getPaymentId()));
        history.setReferenceNumber(snapshot.getReferenceNumber());
        history.setCustomerId(UUID.fromString(snapshot.getCustomerId()));
        history.setSourceAccountId(UUID.fromString(snapshot.getSourceAccountId()));
        history.setDestinationAccountId(UUID.fromString(snapshot.getDestinationAccountId()));

        // Amount and currency
        history.setAmount(new BigDecimal(snapshot.getAmount()));
        history.setCurrency(snapshot.getCurrency());
        history.setPaymentType(snapshot.getPaymentType());
        history.setDescription(snapshot.getDescription() != null ? snapshot.getDescription() : null);

        history.setStatus(snapshot.getStatus().toString());
        history.setFraudStatus(snapshot.getFraudStatus().toString());

        if (snapshot.getRiskAssessment() != null) {
            mapRiskAssessment(snapshot.getRiskAssessment(), history);
        }

        // Lifecycle timestamps - complete audit trail
        history.setInitiatedAt(convertToInstant(snapshot.getInitiatedAt()));
        history.setRiskCheckRequestedAt(convertToInstant(snapshot.getRiskCheckRequestedAt()));
        history.setRiskCheckCompletedAt(convertToInstant(snapshot.getRiskCheckCompletedAt()));
        history.setFraudCheckApprovedAt(convertToInstant(snapshot.getFraudCheckApprovedAt()));
        history.setManualReviewRequestedAt(convertToInstant(snapshot.getManualReviewRequestedAt()));
        history.setManualReviewApprovedAt(convertToInstant(snapshot.getManualReviewApprovedAt()));
        history.setManualReviewRejectedAt(convertToInstant(snapshot.getManualReviewRejectedAt()));
        history.setAccountChargeInitiatedAt(convertToInstant(snapshot.getAccountChargeInitiatedAt()));
        history.setAccountChargedAt(convertToInstant(snapshot.getAccountChargedAt()));
        history.setAccountChargeFailedAt(convertToInstant(snapshot.getAccountChargeFailedAt()));
        history.setCompletedAt(convertToInstant(snapshot.getCompletedAt()));
        history.setBlockedAt(convertToInstant(snapshot.getBlockedAt()));

        // Decision metadata
        history.setManualReviewedBy(snapshot.getManualReviewedBy());
        history.setManualReviewNotes(snapshot.getManualReviewNotes());
        history.setBlockReason(snapshot.getBlockReason());
        history.setFailureReason(snapshot.getFailureReason());

        // Decision override metadata
        history.setDecisionOverriddenBy(snapshot.getDecisionOverriddenBy());
        history.setDecisionOverrideReason(snapshot.getDecisionOverrideReason());
        history.setDecisionOverriddenAt(convertToInstant(snapshot.getDecisionOverriddenAt()));

        history.setAggregateVersion(snapshot.getVersion());
    }

    private static Instant convertToInstant(Long epochMilli) {
        return epochMilli != null ? Instant.ofEpochMilli(epochMilli) : null;
    }

}
