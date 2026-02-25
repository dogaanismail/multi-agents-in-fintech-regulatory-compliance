package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.account.AccountChargeCompletedEvent;
import com.aml.account.AccountChargeRequestedEvent;
import com.aml.account.PaymentType;
import lombok.experimental.UtilityClass;
import org.banksolution.model.response.AccountChargeResponse;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class AccountChargeCompletedEventMapper {

    public static AccountChargeCompletedEvent toAvroEvent(
            AccountChargeRequestedEvent request,
            AccountChargeResponse result) {

        return AccountChargeCompletedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(request.getPaymentId())
                .setTimestamp(Instant.now().toEpochMilli())
                .setSourceAccountId(request.getSourceAccountId())
                .setDestinationAccountId(request.getDestinationAccountId())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setPaymentType(PaymentType.valueOf(request.getPaymentType().name()))
                .setSuccess(result.isSuccess())
                .setFailureReason(result.getFailureReason())
                .build();
    }
}
