package org.banksolution.mapper;

import com.aml.payment.PaymentCreatedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentRequestEntity;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class PaymentCreatedEventMapper {

    public static PaymentCreatedEvent toPaymentCreatedEvent(PaymentRequestEntity entity) {
        return PaymentCreatedEvent.newBuilder()
                .setPaymentId(entity.getId().toString())
                .setEventId(UUID.randomUUID().toString())
                .setReferenceNumber(entity.getReferenceNumber())
                .setTimestamp(Instant.now().toEpochMilli())
                .setCustomerId(entity.getCustomerId().toString())
                .setSourceAccountId(entity.getSourceAccountId() != null ?
                        entity.getSourceAccountId().toString() : null)
                .setDestinationAccountId(entity.getDestinationAccountId() != null ?
                        entity.getDestinationAccountId().toString() : null)
                .setAmount(entity.getAmount().toString())
                .setCurrency(entity.getCurrency().name())
                .setPaymentType(com.aml.payment.PaymentType.valueOf(entity.getPaymentType().name()))
                .setDescription(entity.getDescription())
                .build();
    }

}
