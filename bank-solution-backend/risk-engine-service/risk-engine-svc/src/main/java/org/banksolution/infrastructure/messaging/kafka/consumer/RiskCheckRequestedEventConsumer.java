package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.risk.RiskCheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.service.RiskCheckService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskCheckRequestedEventConsumer {

    private final RiskCheckService riskCheckService;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.risk-check-request}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload RiskCheckRequest event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed RiskCheckRequested event: paymentId:{}, partition:{}, offset:{}",
                event.getPaymentId(),
                partition,
                offset);

        try {
            riskCheckService.processRiskCheckRequest(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed RiskCheckRequested event for paymentId: {}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to process RiskCheckRequested event for paymentId: {}", event.getPaymentId(), e);
            throw e;
        }
    }
}
