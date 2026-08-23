package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.ledger.LedgerPostingRequestedEvent;
import com.aml.ledger.PostingInstructionType;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.event.LedgerAuthorisationInitiatedEvent;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.enums.PaymentScheme;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class LedgerPostingRequestedEventMapper {

    public static LedgerPostingRequestedEvent toAuthorisationEvent(LedgerAuthorisationInitiatedEvent event) {
        PaymentScheme paymentScheme = PaymentScheme.valueOf(event.paymentScheme());

        return switch (paymentScheme) {
            case INTERNAL_TRANSFER -> crossesCurrencies(event)
                    ? toCrossCurrencyTransferAuthorisation(event)
                    : toInternalTransferAuthorisation(event);
            case EXTERNAL_OUTBOUND -> toOutboundAuthorisation(event);
            case EXTERNAL_INBOUND -> toInboundAuthorisation(event);
        };
    }

    public static LedgerPostingRequestedEvent toSettlementEvent(PaymentId paymentId) {
        return newBuilder(paymentId, PostingInstructionType.SETTLEMENT).build();
    }

    public static LedgerPostingRequestedEvent toReleaseEvent(PaymentId paymentId) {
        return newBuilder(paymentId, PostingInstructionType.RELEASE).build();
    }

    private static boolean crossesCurrencies(LedgerAuthorisationInitiatedEvent event) {
        return !event.fromCurrency().equals(event.toCurrency());
    }

    private static LedgerPostingRequestedEvent toInboundAuthorisation(LedgerAuthorisationInitiatedEvent event) {
        return newBuilder(event.paymentId(), PostingInstructionType.INBOUND_AUTHORISATION)
                .setAmount(event.convertedAmount().toPlainString())
                .setCurrency(event.toCurrency())
                .setCustomerAccountId(event.destinationAccountId().toString())
                .build();
    }

    private static LedgerPostingRequestedEvent toOutboundAuthorisation(LedgerAuthorisationInitiatedEvent event) {
        return newBuilder(event.paymentId(), PostingInstructionType.OUTBOUND_AUTHORISATION)
                .setAmount(event.amount().toPlainString())
                .setCurrency(event.fromCurrency())
                .setCustomerAccountId(event.sourceAccountId().toString())
                .build();
    }

    private static LedgerPostingRequestedEvent toInternalTransferAuthorisation(
            LedgerAuthorisationInitiatedEvent event) {

        return newBuilder(event.paymentId(), PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION)
                .setAmount(event.amount().toPlainString())
                .setCurrency(event.fromCurrency())
                .setCustomerAccountId(event.sourceAccountId().toString())
                .setCounterpartyCustomerAccountId(event.destinationAccountId().toString())
                .build();
    }

    private static LedgerPostingRequestedEvent toCrossCurrencyTransferAuthorisation(
            LedgerAuthorisationInitiatedEvent event) {

        return newBuilder(event.paymentId(), PostingInstructionType.CROSS_CURRENCY_TRANSFER_AUTHORISATION)
                .setAmount(event.amount().toPlainString())
                .setCurrency(event.fromCurrency())
                .setBuyAmount(event.convertedAmount().toPlainString())
                .setBuyCurrency(event.toCurrency())
                .setCustomerAccountId(event.sourceAccountId().toString())
                .setCounterpartyCustomerAccountId(event.destinationAccountId().toString())
                .build();
    }

    private static LedgerPostingRequestedEvent.Builder newBuilder(
            PaymentId paymentId,
            PostingInstructionType postingInstructionType) {

        return LedgerPostingRequestedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setClientTransactionId(paymentId.getIdentifier().toString())
                .setPostingInstructionType(postingInstructionType)
                .setTimestamp(Instant.now().toEpochMilli());
    }
}
