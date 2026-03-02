package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.account.AccountChargeRequestedEvent;
import com.aml.account.PaymentType;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.event.AccountChargeInitiatedEvent;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class AccountChargeRequestedEventMapper {

    public static AccountChargeRequestedEvent toAvroRequest(AccountChargeInitiatedEvent event) {
        return AccountChargeRequestedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now().toEpochMilli())
                .setPaymentId(event.paymentId().toString())
                .setCustomerId(event.customerId().toString())
                .setSourceAccountId(event.sourceAccountId() != null ? event.sourceAccountId().toString() : null)
                .setDestinationAccountId(event.destinationAccountId() != null ? event.destinationAccountId().toString() : null)
                .setAmount(event.amount().toString())
                .setFromCurrency(event.fromCurrency())
                .setToCurrency(event.toCurrency())
                .setConvertedAmount(event.convertedAmount().toString())
                .setAppliedExchangeRate(event.appliedExchangeRate() != null ? event.appliedExchangeRate().toString() : null)
                .setPaymentType(PaymentType.valueOf(event.paymentType()))
                .setDescription(event.description())
                .build();
    }
}
