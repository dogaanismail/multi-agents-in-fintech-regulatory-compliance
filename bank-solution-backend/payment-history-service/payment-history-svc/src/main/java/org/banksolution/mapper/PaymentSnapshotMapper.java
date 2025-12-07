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
        history.setExternalPaymentId(UUID.fromString(snapshot.getExternalPaymentId()));
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

        history.setInitiatedAt(convertToInstant(snapshot.getInitiatedAt()));
        history.setRiskCheckRequestedAt(convertToInstant(snapshot.getRiskCheckRequestedAt()));
        history.setRiskCheckCompletedAt(convertToInstant(snapshot.getRiskCheckCompletedAt()));
        history.setCompletedAt(convertToInstant(snapshot.getCompletedAt()));
        history.setBlockedAt(convertToInstant(snapshot.getBlockedAt()));

        history.setAggregateVersion(snapshot.getVersion());
    }

    private static Instant convertToInstant(Long epochMilli) {
        return epochMilli != null ? Instant.ofEpochMilli(epochMilli) : null;
    }

}
