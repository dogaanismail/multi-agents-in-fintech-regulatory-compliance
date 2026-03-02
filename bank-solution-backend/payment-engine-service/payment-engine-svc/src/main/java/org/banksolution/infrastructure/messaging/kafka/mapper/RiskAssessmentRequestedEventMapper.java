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
                .setTimestamp(Instant.now().toEpochMilli())
                .setPaymentId(event.paymentId().toString())
                .setCustomerId(event.customerId().toString())
                .setSourceAccountId(event.sourceAccountId() != null ? event.sourceAccountId().toString() : null)
                .setDestinationAccountId(event.destinationAccountId() != null ? event.destinationAccountId().toString() : null)
                .setAmount(event.amount().toString())
                .setFromCurrency(event.fromCurrency())
                .setToCurrency(event.toCurrency())
                .setPaymentType(PaymentType.valueOf(event.paymentType()))
                .setDescription(event.description())
                .build();
    }
}
