package org.banksolution.api.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.infrastructure.persistence.projection.PaymentStatusProjection;
import org.banksolution.api.dto.PaymentStatusResponse;

@UtilityClass
public class PaymentStatusMapper {

    public static PaymentStatusResponse toResponse(PaymentStatusProjection projection) {
        return PaymentStatusResponse.builder()
                .paymentId(projection.getPaymentId())
                .externalPaymentId(projection.getExternalPaymentId())
                .referenceNumber(projection.getReferenceNumber())
                .customerId(projection.getCustomerId())
                .sourceAccountId(projection.getSourceAccountId())
                .destinationAccountId(projection.getDestinationAccountId())
                .amount(projection.getAmount())
                .currency(projection.getCurrency())
                .paymentType(projection.getPaymentType())
                .description(projection.getDescription())
                .status(projection.getStatus())
                .fraudStatus(projection.getFraudStatus())
                .riskScore(projection.getRiskScore())
                .riskLevel(projection.getRiskLevel())
                .riskAction(projection.getRiskAction())
                .initiatedAt(projection.getInitiatedAt())
                .riskCheckCompletedAt(projection.getRiskCheckCompletedAt())
                .completedAt(projection.getCompletedAt())
                .blockedAt(projection.getBlockedAt())
                .build();
    }
}
