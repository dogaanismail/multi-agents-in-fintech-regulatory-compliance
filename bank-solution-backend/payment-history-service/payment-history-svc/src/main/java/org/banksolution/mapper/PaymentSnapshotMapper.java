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

    public static void mapSnapshotToHistory(PaymentSnapshotEvent paymentSnapshotEvent, PaymentHistoryEntity paymentHistoryEntity) {
        // Basic payment information
        paymentHistoryEntity.setPaymentId(UUID.fromString(paymentSnapshotEvent.getPaymentId()));
        paymentHistoryEntity.setReferenceNumber(paymentSnapshotEvent.getReferenceNumber());
        paymentHistoryEntity.setCustomerId(UUID.fromString(paymentSnapshotEvent.getCustomerId()));
        paymentHistoryEntity.setSourceAccountId(UUID.fromString(paymentSnapshotEvent.getSourceAccountId()));
        paymentHistoryEntity.setDestinationAccountId(UUID.fromString(paymentSnapshotEvent.getDestinationAccountId()));

        // Amount and currency
        paymentHistoryEntity.setAmount(new BigDecimal(paymentSnapshotEvent.getAmount()));
        paymentHistoryEntity.setFromCurrency(paymentSnapshotEvent.getFromCurrency());
        paymentHistoryEntity.setToCurrency(paymentSnapshotEvent.getToCurrency());
        paymentHistoryEntity.setConvertedAmount(new BigDecimal(paymentSnapshotEvent.getConvertedAmount()));
        paymentHistoryEntity.setAppliedExchangeRate(paymentSnapshotEvent.getAppliedExchangeRate() != null ? new BigDecimal(paymentSnapshotEvent.getAppliedExchangeRate()) : null);
        paymentHistoryEntity.setPaymentType(paymentSnapshotEvent.getPaymentType());
        paymentHistoryEntity.setDescription(paymentSnapshotEvent.getDescription());

        paymentHistoryEntity.setStatus(paymentSnapshotEvent.getStatus().toString());
        paymentHistoryEntity.setFraudStatus(paymentSnapshotEvent.getFraudStatus().toString());

        if (paymentSnapshotEvent.getRiskAssessment() != null) {
            mapRiskAssessment(paymentSnapshotEvent.getRiskAssessment(), paymentHistoryEntity);
        }

        // Lifecycle timestamps - complete audit trail
        paymentHistoryEntity.setInitiatedAt(convertToInstant(paymentSnapshotEvent.getInitiatedAt()));
        paymentHistoryEntity.setRiskCheckRequestedAt(convertToInstant(paymentSnapshotEvent.getRiskCheckRequestedAt()));
        paymentHistoryEntity.setRiskCheckCompletedAt(convertToInstant(paymentSnapshotEvent.getRiskCheckCompletedAt()));
        paymentHistoryEntity.setFraudCheckApprovedAt(convertToInstant(paymentSnapshotEvent.getFraudCheckApprovedAt()));
        paymentHistoryEntity.setManualReviewRequestedAt(convertToInstant(paymentSnapshotEvent.getManualReviewRequestedAt()));
        paymentHistoryEntity.setManualReviewApprovedAt(convertToInstant(paymentSnapshotEvent.getManualReviewApprovedAt()));
        paymentHistoryEntity.setManualReviewRejectedAt(convertToInstant(paymentSnapshotEvent.getManualReviewRejectedAt()));
        paymentHistoryEntity.setLedgerAuthorisationInitiatedAt(convertToInstant(paymentSnapshotEvent.getLedgerAuthorisationInitiatedAt()));
        paymentHistoryEntity.setLedgerAuthorisedAt(convertToInstant(paymentSnapshotEvent.getLedgerAuthorisedAt()));
        paymentHistoryEntity.setLedgerSettlementInitiatedAt(convertToInstant(paymentSnapshotEvent.getLedgerSettlementInitiatedAt()));
        paymentHistoryEntity.setLedgerSettledAt(convertToInstant(paymentSnapshotEvent.getLedgerSettledAt()));
        paymentHistoryEntity.setLedgerReleaseInitiatedAt(convertToInstant(paymentSnapshotEvent.getLedgerReleaseInitiatedAt()));
        paymentHistoryEntity.setLedgerReleasedAt(convertToInstant(paymentSnapshotEvent.getLedgerReleasedAt()));
        paymentHistoryEntity.setCompletedAt(convertToInstant(paymentSnapshotEvent.getCompletedAt()));
        paymentHistoryEntity.setBlockedAt(convertToInstant(paymentSnapshotEvent.getBlockedAt()));

        // Decision metadata
        paymentHistoryEntity.setManualReviewedBy(paymentSnapshotEvent.getManualReviewedBy());
        paymentHistoryEntity.setManualReviewNotes(paymentSnapshotEvent.getManualReviewNotes());
        paymentHistoryEntity.setBlockReason(paymentSnapshotEvent.getBlockReason());
        paymentHistoryEntity.setFailureReason(paymentSnapshotEvent.getFailureReason());

        // Decision override metadata
        paymentHistoryEntity.setDecisionOverriddenBy(paymentSnapshotEvent.getDecisionOverriddenBy());
        paymentHistoryEntity.setDecisionOverrideReason(paymentSnapshotEvent.getDecisionOverrideReason());
        paymentHistoryEntity.setDecisionOverriddenAt(convertToInstant(paymentSnapshotEvent.getDecisionOverriddenAt()));

        paymentHistoryEntity.setAggregateVersion(paymentSnapshotEvent.getVersion());
    }

    private static Instant convertToInstant(Long epochMilli) {
        return epochMilli != null ? Instant.ofEpochMilli(epochMilli) : null;
    }

}
