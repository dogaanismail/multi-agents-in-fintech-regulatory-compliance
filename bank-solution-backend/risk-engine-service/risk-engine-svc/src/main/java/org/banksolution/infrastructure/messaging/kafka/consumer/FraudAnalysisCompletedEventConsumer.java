package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.exception.FraudAnalysisCompletedEventException;
import org.banksolution.service.FraudAnalysisCompleteService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisCompletedEventConsumer {

    private final FraudAnalysisCompleteService fraudAnalysisCompleteService;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.fraud-analysis-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload FraudAnalysisCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed FraudAnalysisCompletedEventConsumer event: paymentId:{}, partition:{}, offset:{}",
                event.getPaymentId(),
                partition,
                offset);

        try {
            fraudAnalysisCompleteService.processFraudAnalysisCompleted(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed FraudAnalysisCompletedEventConsumer event for paymentId: {}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to process FraudAnalysisCompletedEventConsumer event for paymentId: {}", event.getPaymentId(), e);
            throw new FraudAnalysisCompletedEventException(
                    "Failed to process FraudAnalysisCompletedEvent for paymentId: %s, riskCheckRequestId: %s",
                    e,
                    event.getPaymentId(),
                    event.getRiskCheckRequestId()
            );
        }
    }
}
