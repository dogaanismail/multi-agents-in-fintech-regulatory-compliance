package org.banksolution.outbox.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class OutboxTraceContextPropagatorTest {

    private final OutboxTraceContextPropagator outboxTraceContextPropagatorWithoutTracing = new OutboxTraceContextPropagator(null, null);

    @Test
    void shouldCaptureNoHeadersWhenTracingIsNotConfigured() {
        assertThat(outboxTraceContextPropagatorWithoutTracing.captureCurrentTraceHeaders()).isEmpty();
        assertThat(outboxTraceContextPropagatorWithoutTracing.isTracingAvailable()).isFalse();
    }

    @Test
    void shouldStillRunTheActionWhenTracingIsNotConfigured() {
        String result = outboxTraceContextPropagatorWithoutTracing.callInRestoredTraceContext(
                Map.of("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"),
                "outbox relay test",
                () -> "delivered");

        assertThat(result).isEqualTo("delivered");
    }
}
