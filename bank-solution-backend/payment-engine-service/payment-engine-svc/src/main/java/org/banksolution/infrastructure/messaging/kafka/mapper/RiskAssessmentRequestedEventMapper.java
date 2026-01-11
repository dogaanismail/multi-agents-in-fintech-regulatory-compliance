package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.risk.PaymentType;
import com.aml.risk.RiskAssessmentRequestedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.event.RiskAssessmentInitiatedEvent;

import java.time.Instant;

@UtilityClass
public class RiskAssessmentRequestedEventMapper {

    public static RiskAssessmentRequestedEvent toAvroRequest(RiskAssessmentInitiatedEvent event) {
        return RiskAssessmentRequestedEvent.newBuilder()
                .setPaymentId(event.paymentId().toString())
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
