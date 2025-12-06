package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentStatusProjection;
import org.banksolution.model.response.PaymentStatusResponse;

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
                .fraudConfidence(projection.getFraudConfidence())
                .maddpgQValue(projection.getMaddpgQValue())
                .initiatedAt(projection.getInitiatedAt())
                .fraudCheckedAt(projection.getFraudCheckedAt())
                .completedAt(projection.getCompletedAt())
                .blockedAt(projection.getBlockedAt())
                .build();
    }
}
