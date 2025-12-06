package org.banksolution.kafka.consumer;

import com.aml.fraud.FraudDetectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.handler.FraudDetectionResponseHandler;
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

    private final FraudDetectionResponseHandler handler;

    @KafkaListener(
            topics = "${kafka.topics.fraud-detection-response}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload FraudDetectionResponse response,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed FraudDetectionResponse: requestId:{}, transactionId:{}, action:{}, partition:{}, offset:{}",
                response.getRequestId(),
                response.getTransactionId(),
                response.getAction(),
                partition,
                offset);

        try {
            handler.handle(response);
            acknowledgment.acknowledge();
            log.info("Successfully processed FraudDetectionResponse: {}", response.getRequestId());
        } catch (Exception e) {
            log.error("Failed to process FraudDetectionResponse: {}", response.getRequestId(), e);
            throw e;
        }
    }
}
