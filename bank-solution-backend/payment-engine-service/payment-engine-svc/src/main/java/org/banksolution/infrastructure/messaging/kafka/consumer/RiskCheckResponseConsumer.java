package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.risk.RiskCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.infrastructure.messaging.kafka.handler.RiskCheckResponseHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskCheckResponseConsumer {

    private final RiskCheckResponseHandler riskCheckResponseHandler;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.risk-check-response}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload RiskCheckResponse response,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed RiskCheckResponse: requestId:{}, paymentId:{}, action:{}, partition:{}, offset:{}",
                response.getRequestId(),
                response.getPaymentId(),
                response.getAction(),
                partition,
                offset);

        try {
            riskCheckResponseHandler.handle(response);
            acknowledgment.acknowledge();
            log.info("Successfully processed RiskCheckResponse: {}", response.getRequestId());
        } catch (Exception e) {
            log.error("Failed to process RiskCheckResponse: {}", response.getRequestId(), e);
            throw e;
        }
    }
}
