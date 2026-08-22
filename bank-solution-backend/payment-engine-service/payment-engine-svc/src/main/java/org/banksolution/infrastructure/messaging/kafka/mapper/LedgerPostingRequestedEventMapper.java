package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.ledger.LedgerPostingRequestedEvent;
import com.aml.ledger.PostingInstructionType;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.event.LedgerAuthorisationInitiatedEvent;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.enums.PaymentType;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class LedgerPostingRequestedEventMapper {

    public static LedgerPostingRequestedEvent toAuthorisationEvent(LedgerAuthorisationInitiatedEvent event) {
        PaymentType paymentType = PaymentType.valueOf(event.paymentType());

        return switch (paymentType) {
            case DEPOSIT, TRANSFER_IN -> toInboundAuthorisation(event);
            case WITHDRAWAL -> toOutboundAuthorisation(event);
            case TRANSFER_OUT -> event.destinationAccountId() == null
                    ? toOutboundAuthorisation(event)
                    : toInternalTransferAuthorisation(event);
        };
    }

    public static LedgerPostingRequestedEvent toSettlementEvent(PaymentId paymentId) {
        return newBuilder(paymentId, PostingInstructionType.SETTLEMENT).build();
    }

    public static LedgerPostingRequestedEvent toReleaseEvent(PaymentId paymentId) {
        return newBuilder(paymentId, PostingInstructionType.RELEASE).build();
    }

    private static LedgerPostingRequestedEvent toInboundAuthorisation(LedgerAuthorisationInitiatedEvent event) {
        return newBuilder(event.paymentId(), PostingInstructionType.INBOUND_AUTHORISATION)
                .setAmount(event.amount().toPlainString())
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
