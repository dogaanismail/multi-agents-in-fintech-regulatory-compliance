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

    public static LedgerPostingRequestedEvent toAuthorisationEvent(LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent) {
        PaymentScheme paymentScheme = PaymentScheme.valueOf(ledgerAuthorisationInitiatedEvent.paymentScheme());

        return switch (paymentScheme) {
            case INTERNAL_TRANSFER -> crossesCurrencies(ledgerAuthorisationInitiatedEvent)
                    ? toCrossCurrencyTransferAuthorisation(ledgerAuthorisationInitiatedEvent)
                    : toInternalTransferAuthorisation(ledgerAuthorisationInitiatedEvent);
            case EXTERNAL_OUTBOUND -> toOutboundAuthorisation(ledgerAuthorisationInitiatedEvent);
            case EXTERNAL_INBOUND -> toInboundAuthorisation(ledgerAuthorisationInitiatedEvent);
        };
    }

    public static LedgerPostingRequestedEvent toSettlementEvent(PaymentId paymentId) {
        return newBuilder(paymentId, PostingInstructionType.SETTLEMENT).build();
    }

    public static LedgerPostingRequestedEvent toReleaseEvent(PaymentId paymentId) {
        return newBuilder(paymentId, PostingInstructionType.RELEASE).build();
    }

    private static boolean crossesCurrencies(LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent) {
        return !ledgerAuthorisationInitiatedEvent.fromCurrency().equals(ledgerAuthorisationInitiatedEvent.toCurrency());
    }

    private static LedgerPostingRequestedEvent toInboundAuthorisation(LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent) {
        return newBuilder(ledgerAuthorisationInitiatedEvent.paymentId(), PostingInstructionType.INBOUND_AUTHORISATION)
                .setAmount(ledgerAuthorisationInitiatedEvent.convertedAmount().toPlainString())
                .setCurrency(ledgerAuthorisationInitiatedEvent.toCurrency())
                .setCustomerAccountId(ledgerAuthorisationInitiatedEvent.destinationAccountId().toString())
                .build();
    }

    private static LedgerPostingRequestedEvent toOutboundAuthorisation(LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent) {
        return newBuilder(ledgerAuthorisationInitiatedEvent.paymentId(), PostingInstructionType.OUTBOUND_AUTHORISATION)
                .setAmount(ledgerAuthorisationInitiatedEvent.amount().toPlainString())
                .setCurrency(ledgerAuthorisationInitiatedEvent.fromCurrency())
                .setCustomerAccountId(ledgerAuthorisationInitiatedEvent.sourceAccountId().toString())
                .build();
    }

    private static LedgerPostingRequestedEvent toInternalTransferAuthorisation(
            LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent) {

        return newBuilder(ledgerAuthorisationInitiatedEvent.paymentId(), PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION)
                .setAmount(ledgerAuthorisationInitiatedEvent.amount().toPlainString())
                .setCurrency(ledgerAuthorisationInitiatedEvent.fromCurrency())
                .setCustomerAccountId(ledgerAuthorisationInitiatedEvent.sourceAccountId().toString())
                .setCounterpartyCustomerAccountId(ledgerAuthorisationInitiatedEvent.destinationAccountId().toString())
                .build();
    }

    private static LedgerPostingRequestedEvent toCrossCurrencyTransferAuthorisation(
            LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent) {

        return newBuilder(ledgerAuthorisationInitiatedEvent.paymentId(), PostingInstructionType.CROSS_CURRENCY_TRANSFER_AUTHORISATION)
                .setAmount(ledgerAuthorisationInitiatedEvent.amount().toPlainString())
                .setCurrency(ledgerAuthorisationInitiatedEvent.fromCurrency())
                .setBuyAmount(ledgerAuthorisationInitiatedEvent.convertedAmount().toPlainString())
                .setBuyCurrency(ledgerAuthorisationInitiatedEvent.toCurrency())
                .setCustomerAccountId(ledgerAuthorisationInitiatedEvent.sourceAccountId().toString())
                .setCounterpartyCustomerAccountId(ledgerAuthorisationInitiatedEvent.destinationAccountId().toString())
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
