package org.banksolution.infrastructure.messaging.kafka.service;

import com.aml.payment.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.banksolution.domain.payment.event.*;
import org.banksolution.domain.payment.event.PaymentBlockedEvent;
import org.banksolution.domain.payment.event.PaymentCompletedEvent;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.infrastructure.messaging.kafka.mapper.RiskAssessmentSnapshotMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.axonframework.eventhandling.GenericEventMessage.asEventMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSnapshotService {

    private final EventStore eventStore;
    private final EventBus eventBus;

    public void publishSnapshot(PaymentId paymentId, String eventTrigger) {
        log.info("Building payment snapshot for paymentId: {}, trigger: {}", paymentId, eventTrigger);

        try {
            PaymentSnapshotEvent snapshot = buildSnapshotFromEvents(paymentId, eventTrigger);
            eventBus.publish(asEventMessage(snapshot));

            log.info("Payment snapshot published for paymentId: {}, version: {}",
                    paymentId,
                    snapshot.getVersion());
        } catch (Exception e) {
            log.error("Error building payment snapshot for paymentId: {}", paymentId, e);
        }
    }

    private PaymentSnapshotEvent buildSnapshotFromEvents(PaymentId paymentId, String eventTrigger) {
        AtomicInteger version = new AtomicInteger(0);
        AtomicReference<PaymentInitiatedEvent> initiatedEvent = new AtomicReference<>();
        AtomicReference<RiskCheckRequestedEvent> riskRequestedEvent = new AtomicReference<>();
        AtomicReference<RiskCheckCompletedEvent> riskCompletedEvent = new AtomicReference<>();
        AtomicReference<PaymentBlockedEvent> blockedEvent = new AtomicReference<>();
        AtomicReference<ManualReviewRequestedEvent> reviewEvent = new AtomicReference<>();
        AtomicReference<PaymentCompletedEvent> completedEvent = new AtomicReference<>();

        eventStore.readEvents(paymentId.toString()).asStream()
                .forEach(domainEventMessage -> {
                    version.incrementAndGet();
                    Object payload = domainEventMessage.getPayload();

                    if (payload instanceof PaymentInitiatedEvent paymentInitiatedEvent) {
                        initiatedEvent.set(paymentInitiatedEvent);
                    } else if (payload instanceof RiskCheckRequestedEvent riskCheckRequestedEvent) {
                        riskRequestedEvent.set(riskCheckRequestedEvent);
                    } else if (payload instanceof RiskCheckCompletedEvent riskCheckCompletedEvent) {
                        riskCompletedEvent.set(riskCheckCompletedEvent);
                    } else if (payload instanceof PaymentBlockedEvent paymentBlockedEvent) {
                        blockedEvent.set(paymentBlockedEvent);
                    } else if (payload instanceof ManualReviewRequestedEvent manualReviewRequestedEvent) {
                        reviewEvent.set(manualReviewRequestedEvent);
                    } else if (payload instanceof PaymentCompletedEvent paymentCompletedEvent) {
                        completedEvent.set(paymentCompletedEvent);
                    }
                });

        PaymentSnapshotEvent.Builder builder = PaymentSnapshotEvent.newBuilder();

        if (initiatedEvent.get() != null) {
            PaymentInitiatedEvent initiated = initiatedEvent.get();
            builder.setPaymentId(initiated.getPaymentId().toString())
                    .setExternalPaymentId(initiated.getExternalPaymentId().toString())
                    .setReferenceNumber(initiated.getReferenceNumber())
                    .setCustomerId(initiated.getCustomerId().toString())
                    .setSourceAccountId(initiated.getSourceAccountId().toString())
                    .setDestinationAccountId(initiated.getDestinationAccountId().toString())
                    .setAmount(initiated.getAmount().toString())
                    .setCurrency(initiated.getCurrency())
                    .setPaymentType(initiated.getPaymentType())
                    .setDescription(initiated.getDescription())
                    .setInitiatedAt(Instant.now().toEpochMilli());
        }

        if (riskCompletedEvent.get() != null) {
            RiskAssessmentSnapshot riskSnapshot = RiskAssessmentSnapshotMapper.toSnapshot(
                    riskCompletedEvent.get().getRiskAssessment()
            );
            builder.setRiskAssessment(riskSnapshot).setRiskCheckCompletedAt(Instant.now().toEpochMilli());
        }

        if (riskRequestedEvent.get() != null) {
            builder.setRiskCheckRequestedAt(Instant.now().toEpochMilli());
        }

        if (completedEvent.get() != null) {
            builder.setStatus(PaymentStatus.COMPLETED)
                    .setFraudStatus(FraudCheckStatus.APPROVED)
                    .setCompletedAt(Instant.now().toEpochMilli());
        } else if (blockedEvent.get() != null) {
            builder.setStatus(PaymentStatus.BLOCKED)
                    .setFraudStatus(FraudCheckStatus.BLOCKED)
                    .setBlockedAt(Instant.now().toEpochMilli());
        } else if (reviewEvent.get() != null) {
            builder.setStatus(PaymentStatus.MANUAL_REVIEW_REQUIRED)
                    .setFraudStatus(FraudCheckStatus.REVIEW_REQUIRED)
                    .setManualReviewRequestedAt(Instant.now().toEpochMilli());
        } else if (riskRequestedEvent.get() != null) {
            builder.setStatus(PaymentStatus.FRAUD_CHECK_PENDING)
                    .setFraudStatus(FraudCheckStatus.PENDING);
        } else {
            builder.setStatus(PaymentStatus.INITIATED)
                    .setFraudStatus(FraudCheckStatus.PENDING);
        }

        builder.setVersion(version.get())
                .setSnapshotTimestamp(Instant.now().toEpochMilli())
                .setEventTrigger(eventTrigger);

        return builder.build();
    }
}
