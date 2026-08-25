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
            @Payload RiskAssessmentCompletedEvent riskAssessmentCompletedEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed RiskAssessmentCompletedEvent: riskCheckRequestId:{}, paymentId:{}, action:{}, partition:{}, offset:{}",
                riskAssessmentCompletedEvent.getRiskCheckRequestId(),
                riskAssessmentCompletedEvent.getPaymentId(),
                riskAssessmentCompletedEvent.getAction(),
                partition,
                offset);

        try {
            riskAssessmentCompletedEventHandler.handle(riskAssessmentCompletedEvent);
            acknowledgment.acknowledge();
            log.info("Successfully processed RiskAssessmentCompletedEvent for paymentId:{} and riskCheckRequestId:{}",
                    riskAssessmentCompletedEvent.getPaymentId(),
                    riskAssessmentCompletedEvent.getRiskCheckRequestId());
        } catch (Exception exception) {
            log.error("Failed to process RiskAssessmentCompletedEvent for paymentId:{} and riskCheckRequestId:{}",
                    riskAssessmentCompletedEvent.getPaymentId(),
                    riskAssessmentCompletedEvent.getRiskCheckRequestId(),
                    exception);
            throw new RiskAssessmentCompletedEventException("Failed to process RiskAssessmentCompletedEvent for paymentId: %s and riskCheckRequestId: %s",
                    exception,
                    riskAssessmentCompletedEvent.getPaymentId(),
                    riskAssessmentCompletedEvent.getRiskCheckRequestId());
        }
    }
}
