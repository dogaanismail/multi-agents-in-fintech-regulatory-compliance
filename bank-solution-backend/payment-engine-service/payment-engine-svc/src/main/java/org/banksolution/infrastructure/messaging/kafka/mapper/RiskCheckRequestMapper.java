package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.risk.PaymentType;
import com.aml.risk.RiskCheckRequest;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.event.RiskCheckRequestedEvent;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class RiskCheckRequestMapper {

    public static RiskCheckRequest toAvroRequest(RiskCheckRequestedEvent event) {
        return RiskCheckRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setPaymentId(event.paymentId().toString())
                .setReferenceNumber(event.referenceNumber())
                .setCustomerId(event.customerId().toString())
                .setSourceAccountId(event.sourceAccountId() != null ? event.sourceAccountId().toString() : null)
                .setDestinationAccountId(event.destinationAccountId() != null ? event.destinationAccountId().toString() : null)
                .setAmount(event.amount().toString())
                .setCurrency(event.currency())
                .setPaymentType(PaymentType.PAYMENT)
                .setTimestamp(Instant.now().toEpochMilli())
                .setDescription(event.description())
                .build();
    }
}
