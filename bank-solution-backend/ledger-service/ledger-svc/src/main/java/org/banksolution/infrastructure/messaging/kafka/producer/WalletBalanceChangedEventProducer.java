package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.ledger.WalletBalanceChangedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletBalanceChangedEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull WalletBalanceChangedEvent> walletBalanceChangedEventKafkaTemplate;

    public void publish(WalletBalanceChangedEvent event) {
        String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getWalletBalanceChanged();

        walletBalanceChangedEventKafkaTemplate.send(topic, event.getLedgerAccountId(), event);

        log.info("Published WalletBalanceChangedEvent: ledgerAccountId:{}, currency:{}, availableBalance:{}",
                event.getLedgerAccountId(),
                event.getCurrency(),
                event.getAvailableBalance());
    }
}
