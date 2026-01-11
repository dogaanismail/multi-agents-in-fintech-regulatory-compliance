package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.risk.RiskAssessmentRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.exception.RiskAssessmentRequestedEventException;
import org.banksolution.service.RiskAssessmentRequestService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentRequestedEventConsumer {

    private final RiskAssessmentRequestService riskAssessmentRequestService;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.risk-assessment-requested}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload RiskAssessmentRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed RiskAssessmentRequestedEvent event: paymentId:{}, partition:{}, offset:{}",
                event.getPaymentId(),
                partition,
                offset);

        try {
            riskAssessmentRequestService.processRiskAssessmentRequest(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed RiskAssessmentRequestedEvent event for paymentId: {}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to process RiskAssessmentRequestedEvent event for paymentId: {}", event.getPaymentId(), e);
            throw new RiskAssessmentRequestedEventException("Failed to process RiskAssessmentRequestedEvent event for paymentId: %s", e, event.getPaymentId());
        }
    }
}
