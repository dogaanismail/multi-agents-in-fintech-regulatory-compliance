package org.banksolution.infrastructure.messaging.kafka.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.banksolution.exception.KafkaPublicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaDeliveryAwaiterTest {

    private static final String TOPIC = "payment-snapshot-events";
    private static final String MESSAGE_KEY = "payment-1";

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void shouldReturnTheBrokerAcknowledgementOnceDeliveryCompletes() {
        SendResult<String, String> sendResult = new SendResult<>(new ProducerRecord<>(TOPIC, MESSAGE_KEY, "payload"), null);

        SendResult<String, String> awaitedSendResult =
                KafkaDeliveryAwaiter.awaitDelivery(CompletableFuture.completedFuture(sendResult), TOPIC, MESSAGE_KEY);

        assertThat(awaitedSendResult).isSameAs(sendResult);
    }

    @Test
    void shouldUnwrapTheBrokerFailureAndNameTheTopicAndKey() {
        IllegalStateException brokerFailure = new IllegalStateException("not enough in-sync replicas");
        CompletableFuture<SendResult<String, String>> failedDelivery = CompletableFuture.failedFuture(brokerFailure);

        assertThatThrownBy(() -> KafkaDeliveryAwaiter.awaitDelivery(failedDelivery, TOPIC, MESSAGE_KEY))
                .isInstanceOf(KafkaPublicationException.class)
                .hasMessage("Failed to publish to topic %s with key %s", TOPIC, MESSAGE_KEY)
                .hasCause(brokerFailure);
    }

    @Test
    void shouldRestoreTheInterruptFlagWhenTheWaitIsInterrupted() {
        CompletableFuture<SendResult<String, String>> pendingDelivery = new CompletableFuture<>();
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> KafkaDeliveryAwaiter.awaitDelivery(pendingDelivery, TOPIC, MESSAGE_KEY))
                .isInstanceOf(KafkaPublicationException.class)
                .hasMessage("Interrupted while publishing to topic %s with key %s", TOPIC, MESSAGE_KEY)
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}
