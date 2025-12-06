package org.banksolution.eventhandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.banksolution.event.*;
import org.banksolution.kafka.producer.FraudDetectionRequestProducer;
import org.banksolution.kafka.producer.PaymentBlockedEventProducer;
import org.banksolution.kafka.producer.PaymentCompletedEventProducer;
import org.banksolution.kafka.producer.RiskCheckRequestProducer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventHandler {

    private final RiskCheckRequestProducer riskCheckRequestProducer;
    private final FraudDetectionRequestProducer fraudDetectionRequestProducer;
    private final PaymentCompletedEventProducer paymentCompletedEventProducer;
    private final PaymentBlockedEventProducer paymentBlockedEventProducer;

    @EventHandler
    public void on(RiskCheckRequestedEvent event) {
        log.info("Publishing risk check request for payment: {}", event.getPaymentId());
        riskCheckRequestProducer.publishRiskCheckRequest(event);
    }

    @EventHandler
    public void on(FraudCheckRequestedEvent event) {
        log.info("Publishing fraud detection request for payment: {}", event.getPaymentId());
        fraudDetectionRequestProducer.publishFraudCheckRequest(event);
    }

    @EventHandler
    public void on(FraudCheckApprovedEvent event) {
        log.info("Publishing payment completed event for payment: {}", event.getPaymentId());
        paymentCompletedEventProducer.publishPaymentCompleted(event);
    }

    @EventHandler
    public void on(PaymentBlockedEvent event) {
        log.info("Publishing payment blocked event for payment: {}", event.getPaymentId());
        paymentBlockedEventProducer.publishPaymentBlocked(event);
    }

    @EventHandler
    public void on(PaymentCompletedEvent event) {
        log.info("Payment processing completed: {}", event.getPaymentId());
    }
}
