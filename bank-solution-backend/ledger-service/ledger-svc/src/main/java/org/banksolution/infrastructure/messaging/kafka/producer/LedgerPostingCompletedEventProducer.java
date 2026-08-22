package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.ledger.LedgerPostingCompletedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingCompletedEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull LedgerPostingCompletedEvent>
            ledgerPostingCompletedEventKafkaTemplate;

    public void publish(LedgerPostingCompletedEvent event) {
        String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getLedgerPostingCompleted();

        ledgerPostingCompletedEventKafkaTemplate.send(topic, event.getClientTransactionId(), event);

        log.info("Published LedgerPostingCompletedEvent: clientTransactionId:{}, type:{}, success:{}",
                event.getClientTransactionId(),
                event.getPostingInstructionType(),
                event.getSuccess());
    }
}
