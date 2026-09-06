package org.banksolution.infrastructure.messaging.kafka.producer;

import org.banksolution.exception.KafkaPublicationException;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class KafkaDeliveryAwaiter {

    private KafkaDeliveryAwaiter() {
    }

    public static <K, V> SendResult<K, V> awaitDelivery(
            CompletableFuture<SendResult<K, V>> delivery,
            String topic,
            K messageKey) {

        try {
            return delivery.get();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new KafkaPublicationException(
                    "Interrupted while publishing to topic %s with key %s", interruptedException, topic, messageKey);
        } catch (ExecutionException executionException) {
            throw new KafkaPublicationException(
                    "Failed to publish to topic %s with key %s", executionException.getCause(), topic, messageKey);
        }
    }
}
