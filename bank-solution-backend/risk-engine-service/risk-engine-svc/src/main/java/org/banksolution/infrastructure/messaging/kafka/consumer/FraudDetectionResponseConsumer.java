package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.fraud.FraudDetectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionResponseConsumer {

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.fraud-detection-response}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload FraudDetectionResponse event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed FraudDetectionResponse event: paymentId:{}, partition:{}, offset:{}",
                event.getPaymentId(),
                partition,
                offset);

        try {
            // Implement fraud detection response logic here
            acknowledgment.acknowledge();
            log.info("Successfully processed FraudDetectionResponse event for paymentId: {}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to process FraudDetectionResponse event for paymentId: {}", event.getPaymentId(), e);
            throw e;
        }
    }
}
