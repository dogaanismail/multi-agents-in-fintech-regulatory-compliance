package org.banksolution.infrastructure.messaging.kafka.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.banksolution.domain.payment.event.*;
import org.banksolution.infrastructure.messaging.kafka.producer.PaymentBlockedEventProducer;
import org.banksolution.infrastructure.messaging.kafka.producer.PaymentCompletedEventProducer;
import org.banksolution.infrastructure.messaging.kafka.producer.RiskCheckRequestProducer;
import org.banksolution.infrastructure.messaging.kafka.service.PaymentSnapshotService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventHandler {

    private final RiskCheckRequestProducer riskCheckRequestProducer;
    private final PaymentCompletedEventProducer paymentCompletedEventProducer;
    private final PaymentBlockedEventProducer paymentBlockedEventProducer;
    private final PaymentSnapshotService paymentSnapshotService;

    @EventHandler
    public void on(PaymentInitiatedEvent event) {
        log.info("Payment initiated, publishing snapshot: {}", event.getPaymentId());
        paymentSnapshotService.publishSnapshot(event.getPaymentId(), "PAYMENT_INITIATED");
    }

    @EventHandler
    public void on(RiskCheckRequestedEvent event) {
        log.info("Publishing risk check request for payment: {}", event.getPaymentId());
        riskCheckRequestProducer.publishRiskCheckRequest(event);
        paymentSnapshotService.publishSnapshot(event.getPaymentId(), "RISK_CHECK_REQUESTED");
    }

    @EventHandler
    public void on(RiskCheckCompletedEvent event) {
        log.info("Risk check completed, publishing snapshot: {}", event.getPaymentId());
        paymentSnapshotService.publishSnapshot(event.getPaymentId(), "RISK_CHECK_COMPLETED");
    }

    @EventHandler
    public void on(PaymentBlockedEvent event) {
        log.info("Publishing payment blocked event for payment: {}", event.getPaymentId());
        paymentBlockedEventProducer.publishPaymentBlocked(event);
        paymentSnapshotService.publishSnapshot(event.getPaymentId(), "PAYMENT_BLOCKED");
    }

    @EventHandler
    public void on(ManualReviewRequestedEvent event) {
        log.info("Manual review requested, publishing snapshot: {}", event.getPaymentId());
        paymentSnapshotService.publishSnapshot(event.getPaymentId(), "MANUAL_REVIEW_REQUESTED");
    }

    @EventHandler
    public void on(PaymentCompletedEvent event) {
        log.info("Payment completed, publishing Kafka event and snapshot: {}", event.getPaymentId());
        paymentCompletedEventProducer.publishPaymentCompleted(event.getPaymentId());
        paymentSnapshotService.publishSnapshot(event.getPaymentId(), "PAYMENT_COMPLETED");
    }
}
