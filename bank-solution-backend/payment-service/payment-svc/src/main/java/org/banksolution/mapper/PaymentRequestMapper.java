package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.model.request.PaymentRequestRequest;
import org.banksolution.model.response.PaymentRequestResponse;

@UtilityClass
public class PaymentRequestMapper {

    public static PaymentRequestEntity toEntity(
            PaymentRequestRequest request) {

        return PaymentRequestEntity.builder()
                .customerId(request.getCustomerId())
                .sourceAccountId(request.getSourceAccountId())
                .destinationAccountId(request.getDestinationAccountId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentType(request.getPaymentType())
                .description(request.getDescription())
                .build();
    }

    public static PaymentRequestResponse toResponse(
            PaymentRequestEntity entity,
            String message) {

        return PaymentRequestResponse.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .sourceAccountId(entity.getSourceAccountId())
                .destinationAccountId(entity.getDestinationAccountId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .paymentType(entity.getPaymentType())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .message(message)
                .build();
    }

}
