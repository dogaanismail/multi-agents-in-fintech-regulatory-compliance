package org.banksolution.infrastructure.messaging.kafka;

import com.aml.ledger.WalletBalanceChangedEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.common.kafka.KafkaTestClients;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.AccountFixtures.createWalletBalanceChangedEvent;

class WalletBalanceChangedDeadLetterTest extends BaseIntegrationTest {

    /**
     * The error handler retries 3 times with exponential backoff (1s/2s/4s) before
     * parking the message, so the DLT assertion needs a generous timeout.
     */
    private static final Duration DEAD_LETTER_TIMEOUT = Duration.ofSeconds(60);
    private static final String DEAD_LETTER_TOPIC_SUFFIX = ".DLT";

    @Value("${spring.kafka.topics.incoming.wallet-balance-changed}")
    private String walletBalanceChangedTopic;

    @Test
    void shouldParkAnEventWithAMalformedLedgerAccountIdOnTheDeadLetterTopicAfterRetries()
            throws ExecutionException, InterruptedException {

        String malformedLedgerAccountId = "not-a-uuid-" + UUID.randomUUID();
        WalletBalanceChangedEvent poisonWalletBalanceChangedEvent =
                createWalletBalanceChangedEvent(malformedLedgerAccountId, "750.00", "650.00");

        try (KafkaProducer<String, WalletBalanceChangedEvent> producer = KafkaTestClients.createAvroProducer()) {
            producer.send(new ProducerRecord<>(
                    walletBalanceChangedTopic, malformedLedgerAccountId, poisonWalletBalanceChangedEvent)).get();
        }

        WalletBalanceChangedEvent parkedWalletBalanceChangedEvent = KafkaTestClients.awaitMatchingEvent(
                walletBalanceChangedTopic + DEAD_LETTER_TOPIC_SUFFIX,
                DEAD_LETTER_TIMEOUT,
                deadLetteredEvent -> malformedLedgerAccountId.equals(deadLetteredEvent.getLedgerAccountId()));

        assertThat(parkedWalletBalanceChangedEvent.getEventId()).isEqualTo(poisonWalletBalanceChangedEvent.getEventId());
    }
}
