package org.banksolution.mapper;

import com.aml.payment.PaymentCreatedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentRequestEntity;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class PaymentCreatedEventMapper {

    public static PaymentCreatedEvent toPaymentCreatedEvent(PaymentRequestEntity paymentRequestEntity) {
        return PaymentCreatedEvent.newBuilder()
                .setPaymentId(paymentRequestEntity.getId().toString())
                .setEventId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now().toEpochMilli())
                .setCustomerId(paymentRequestEntity.getCustomerId().toString())
                .setSourceAccountId(paymentRequestEntity.getSourceAccountId() != null ?
                        paymentRequestEntity.getSourceAccountId().toString() : null)
                .setDestinationAccountId(paymentRequestEntity.getDestinationAccountId() != null ?
                        paymentRequestEntity.getDestinationAccountId().toString() : null)
                .setAmount(paymentRequestEntity.getAmount().toString())
                .setCurrency(paymentRequestEntity.getCurrency().name())
                .setPaymentType(com.aml.payment.PaymentType.valueOf(paymentRequestEntity.getPaymentType().name()))
                .setDescription(paymentRequestEntity.getDescription())
                .build();
    }

}
