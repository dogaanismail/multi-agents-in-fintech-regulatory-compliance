package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.risk.RiskAssessmentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.exception.RiskAssessmentCompletedEventException;
import org.banksolution.infrastructure.messaging.kafka.handler.RiskAssessmentCompletedEventHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentCompletedEventConsumer {

    private final RiskAssessmentCompletedEventHandler riskAssessmentCompletedEventHandler;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.risk-assessment-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload RiskAssessmentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed RiskAssessmentCompletedEvent: riskCheckRequestId:{}, paymentId:{}, action:{}, partition:{}, offset:{}",
                event.getRiskCheckRequestId(),
                event.getPaymentId(),
                event.getAction(),
                partition,
                offset);

        try {
            riskAssessmentCompletedEventHandler.handle(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed RiskAssessmentCompletedEvent for paymentId:{} and riskCheckRequestId:{}",
                    event.getPaymentId(),
                    event.getRiskCheckRequestId());
        } catch (Exception e) {
            log.error("Failed to process RiskAssessmentCompletedEvent for paymentId:{} and riskCheckRequestId:{}",
                    event.getRiskCheckRequestId(),
                    event.getPaymentId(),
                    e);
            throw new RiskAssessmentCompletedEventException("Failed to process RiskAssessmentCompletedEvent for paymentId: %s and riskCheckRequestId: %s",
                    e,
                    event.getPaymentId(),
                    event.getRiskCheckRequestId());
        }
    }
}
