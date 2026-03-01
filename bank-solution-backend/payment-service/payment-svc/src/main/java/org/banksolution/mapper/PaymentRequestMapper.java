package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;

@UtilityClass
public class PaymentRequestMapper {

    public static PaymentRequestEntity toEntity(
            PaymentRequest paymentRequest) {

        return PaymentRequestEntity.builder()
                .customerId(paymentRequest.getCustomerId())
                .sourceAccountId(paymentRequest.getSourceAccountId())
                .destinationAccountId(paymentRequest.getDestinationAccountId())
                .amount(paymentRequest.getAmount())
                .fromCurrency(paymentRequest.getFromCurrency())
                .toCurrency(paymentRequest.getToCurrency())
                .paymentType(paymentRequest.getPaymentType())
                .description(paymentRequest.getDescription())
                .build();
    }

    public static PaymentRequestResponse toResponse(
            PaymentRequestEntity paymentRequestEntity,
            String message) {

        return PaymentRequestResponse.builder()
                .id(paymentRequestEntity.getId())
                .customerId(paymentRequestEntity.getCustomerId())
                .sourceAccountId(paymentRequestEntity.getSourceAccountId())
                .destinationAccountId(paymentRequestEntity.getDestinationAccountId())
                .amount(paymentRequestEntity.getAmount())
                .fromCurrency(paymentRequestEntity.getFromCurrency())
                .toCurrency(paymentRequestEntity.getToCurrency())
                .convertedAmount(paymentRequestEntity.getConvertedAmount())
                .appliedExchangeRate(paymentRequestEntity.getAppliedExchangeRate())
                .paymentType(paymentRequestEntity.getPaymentType())
                .description(paymentRequestEntity.getDescription())
                .createdAt(paymentRequestEntity.getCreatedAt())
                .message(message)
                .build();
    }

}
