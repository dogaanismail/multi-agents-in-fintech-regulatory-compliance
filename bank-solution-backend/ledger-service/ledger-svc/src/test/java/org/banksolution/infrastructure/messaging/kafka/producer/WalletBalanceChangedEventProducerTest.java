package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.ledger.WalletBalanceChangedEvent;
import org.banksolution.config.KafkaConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WalletBalanceChangedEventProducerTest {

    private static final String TOPIC = "ledger.wallet.balance.changed";

    private KafkaTemplate<String, WalletBalanceChangedEvent> walletBalanceChangedEventKafkaTemplate;
    private WalletBalanceChangedEventProducer walletBalanceChangedEventProducer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setWalletBalanceChanged(TOPIC);
        walletBalanceChangedEventKafkaTemplate = mock(KafkaTemplate.class);
        walletBalanceChangedEventProducer =
                new WalletBalanceChangedEventProducer(kafkaConfigurationProperties, walletBalanceChangedEventKafkaTemplate);
    }

    @Test
    void shouldPublishTheBalanceKeyedByLedgerAccountIdSoOneWalletStaysOrdered() {
        String ledgerAccountId = UUID.randomUUID().toString();
        WalletBalanceChangedEvent walletBalanceChangedEvent = WalletBalanceChangedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setLedgerAccountId(ledgerAccountId)
                .setCustomerAccountId(UUID.randomUUID().toString())
                .setCurrency("GBP")
                .setPostedBalance("1000.00")
                .setAvailableBalance("750.00")
                .setPendingDebits("250.00")
                .setPendingCredits("0.00")
                .setTimestamp(1L)
                .build();

        walletBalanceChangedEventProducer.publish(walletBalanceChangedEvent);

        verify(walletBalanceChangedEventKafkaTemplate).send(TOPIC, ledgerAccountId, walletBalanceChangedEvent);
    }
}
