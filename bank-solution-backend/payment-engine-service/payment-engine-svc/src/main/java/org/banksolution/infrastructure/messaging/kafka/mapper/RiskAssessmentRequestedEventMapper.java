package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.risk.PaymentType;
import com.aml.risk.RiskAssessmentRequestedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.event.RiskAssessmentInitiatedEvent;

import java.time.Instant;

@UtilityClass
public class RiskAssessmentRequestedEventMapper {

    public static RiskAssessmentRequestedEvent toAvroRequest(RiskAssessmentInitiatedEvent riskAssessmentInitiatedEvent) {
        return RiskAssessmentRequestedEvent.newBuilder()
                .setTimestamp(Instant.now().toEpochMilli())
                .setPaymentId(riskAssessmentInitiatedEvent.paymentId().toString())
                .setCustomerId(riskAssessmentInitiatedEvent.customerId().toString())
                .setSourceAccountId(riskAssessmentInitiatedEvent.sourceAccountId() != null ? riskAssessmentInitiatedEvent.sourceAccountId().toString() : null)
                .setDestinationAccountId(riskAssessmentInitiatedEvent.destinationAccountId() != null ? riskAssessmentInitiatedEvent.destinationAccountId().toString() : null)
                .setAmount(riskAssessmentInitiatedEvent.amount().toString())
                .setFromCurrency(riskAssessmentInitiatedEvent.fromCurrency())
                .setToCurrency(riskAssessmentInitiatedEvent.toCurrency())
                .setPaymentType(PaymentType.valueOf(riskAssessmentInitiatedEvent.paymentType()))
                .setDescription(riskAssessmentInitiatedEvent.description())
                .build();
    }
}
