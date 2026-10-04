package org.banksolution.outbox.relay;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.banksolution.outbox.enums.OutboxEventFailType;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;

class OutboxDeliveryFailureClassifierTest {

    @Test
    void shouldTreatABrokerTimeoutAsTransient() {
        OutboxDeliveryException outboxDeliveryException = new OutboxDeliveryException("send failed", new TimeoutException("no ack"));

        assertThat(OutboxDeliveryFailureClassifier.classifyOutboxDeliveryFailure(outboxDeliveryException))
                .isEqualTo(OutboxEventFailType.TRANSIENT);
    }

    @Test
    void shouldFindAPermanentCauseNestedUnderSpringAndFutureWrappers() {
        ExecutionException executionException = new ExecutionException(new KafkaException("send failed", new RecordTooLargeException("too big")));
        OutboxDeliveryException outboxDeliveryException = new OutboxDeliveryException("send failed", executionException);

        assertThat(OutboxDeliveryFailureClassifier.classifyOutboxDeliveryFailure(outboxDeliveryException))
                .isEqualTo(OutboxEventFailType.PERMANENT);
    }

    @Test
    void shouldTreatInvalidTopicsAndMissingPermissionsAsPermanent() {
        assertThat(OutboxDeliveryFailureClassifier.classifyOutboxDeliveryFailure(new InvalidTopicException("bad name")))
                .isEqualTo(OutboxEventFailType.PERMANENT);
        assertThat(OutboxDeliveryFailureClassifier.classifyOutboxDeliveryFailure(new TopicAuthorizationException("denied")))
                .isEqualTo(OutboxEventFailType.PERMANENT);
    }
}
