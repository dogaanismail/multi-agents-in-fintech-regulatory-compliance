package org.banksolution.outbox.fixtures;

import com.aml.payment.FixedSide;
import com.aml.payment.PaymentCreatedEvent;
import com.aml.payment.PaymentScheme;
import com.aml.payment.PaymentType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.banksolution.outbox.entity.OutboxEventEntity;
import org.banksolution.outbox.enums.OutboxEventStatus;

public final class OutboxFixtures {

    public static final Instant FIXED_NOW = Instant.parse("2026-10-04T10:00:00Z");
    public static final String PAYMENT_CREATED_TOPIC = "outbox-test-payment-created";
    public static final String RAW_PAYLOAD_TOPIC = "outbox-test-raw-payload";

    private OutboxFixtures() {
    }

    public static PaymentCreatedEvent createPaymentCreatedEvent(
            UUID paymentId) {

        return PaymentCreatedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(paymentId.toString())
                .setTimestamp(FIXED_NOW.toEpochMilli())
                .setCustomerId(UUID.randomUUID().toString())
                .setPaymentType(PaymentType.TRANSFER_OUT)
                .setPaymentScheme(PaymentScheme.INTERNAL_TRANSFER)
                .setFixedSide(FixedSide.SELL)
                .setIsCrossBorderPayment(false)
                .setSourceAccountId(UUID.randomUUID().toString())
                .setDestinationAccountId(UUID.randomUUID().toString())
                .setAmount("100.00")
                .setFromCurrency("EUR")
                .setToCurrency("EUR")
                .setConvertedAmount("100.00")
                .setAppliedExchangeRate(null)
                .setDescription("outbox test payment")
                .build();
    }

    public static OutboxEventEntity createOutboxEventEntity(
            String destination,
            OutboxEventStatus outboxEventStatus,
            Instant nextAttemptAt) {

        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .referenceId(UUID.randomUUID().toString())
                .destination(destination)
                .payloadType("com.aml.payment.PaymentCreatedEvent")
                .payload("payload".getBytes(StandardCharsets.UTF_8))
                .idempotenceKey("idempotence-" + UUID.randomUUID())
                .headers(Map.of())
                .status(outboxEventStatus)
                .attempts(0)
                .nextAttemptAt(nextAttemptAt)
                .createdAt(nextAttemptAt)
                .build();
    }
}
