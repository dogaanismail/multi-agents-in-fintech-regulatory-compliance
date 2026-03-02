package org.banksolution.mapper;

import com.aml.payment.PaymentCreatedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentRequestEntity;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class PaymentCreatedEventMapper {

    public static PaymentCreatedEvent toPaymentCreatedEvent(
            PaymentRequestEntity paymentRequestEntity,
            boolean isCrossOrderPayment) {

        return PaymentCreatedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now().toEpochMilli())
                .setPaymentId(paymentRequestEntity.getId().toString())
                .setCustomerId(paymentRequestEntity.getCustomerId().toString())
                .setSourceAccountId(paymentRequestEntity.getSourceAccountId() != null ?
                        paymentRequestEntity.getSourceAccountId().toString() : null)
                .setDestinationAccountId(paymentRequestEntity.getDestinationAccountId() != null ?
                        paymentRequestEntity.getDestinationAccountId().toString() : null)
                .setAmount(paymentRequestEntity.getAmount().toString())
                .setFromCurrency(paymentRequestEntity.getFromCurrency().name())
                .setToCurrency(paymentRequestEntity.getToCurrency().name())
                .setConvertedAmount(paymentRequestEntity.getConvertedAmount().toString())
                .setAppliedExchangeRate(paymentRequestEntity.getAppliedExchangeRate() != null ? paymentRequestEntity.getAppliedExchangeRate().toString() : null)
                .setPaymentType(com.aml.payment.PaymentType.valueOf(paymentRequestEntity.getPaymentType().name()))
                .setIsCrossBorderPayment(isCrossOrderPayment)
                .setDescription(paymentRequestEntity.getDescription())
                .build();
    }

}
