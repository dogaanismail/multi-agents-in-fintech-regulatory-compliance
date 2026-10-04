package org.banksolution.outbox.relay;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.banksolution.outbox.enums.OutboxEventFailType;

public class OutboxEventMetrics {

    private final MeterRegistry meterRegistry;

    public OutboxEventMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordOutboxEventPublished(String destination) {
        incrementCounter("outbox.events.published", destination, null);
    }

    public void recordOutboxEventRetryScheduled(String destination) {
        incrementCounter("outbox.events.retried", destination, null);
    }

    public void recordOutboxEventFailed(
            String destination,
            OutboxEventFailType outboxEventFailType) {

        incrementCounter("outbox.events.failed", destination, outboxEventFailType);
    }

    private void incrementCounter(
            String counterName,
            String destination,
            OutboxEventFailType outboxEventFailType) {

        if (meterRegistry == null) {
            return;
        }

        Counter.Builder counterBuilder = Counter.builder(counterName).tag("destination", destination);
        if (outboxEventFailType != null) {
            counterBuilder.tag("fail_type", outboxEventFailType.name());
        }

        counterBuilder.register(meterRegistry).increment();
    }
}
