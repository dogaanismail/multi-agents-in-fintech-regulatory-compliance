package org.banksolution.outbox.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class OutboxTraceContextPropagator {

    private final Tracer tracer;
    private final Propagator propagator;

    public OutboxTraceContextPropagator(
            Tracer tracer,
            Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public Map<String, String> captureCurrentTraceHeaders() {
        Map<String, String> traceHeaders = new HashMap<>();
        if (!isTracingAvailable()) {
            return traceHeaders;
        }

        TraceContext currentTraceContext = tracer.currentTraceContext().context();
        if (currentTraceContext != null) {
            propagator.inject(currentTraceContext, traceHeaders, Map::put);
        }

        return traceHeaders;
    }

    public <T> T callInRestoredTraceContext(
            Map<String, String> traceHeaders,
            String spanName,
            Supplier<T> action) {

        if (!isTracingAvailable() || traceHeaders == null || traceHeaders.isEmpty()) {
            return action.get();
        }

        Span relaySpan = propagator.extract(traceHeaders, Map::get).name(spanName).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(relaySpan)) {
            return action.get();
        } catch (RuntimeException runtimeException) {
            relaySpan.error(runtimeException);
            throw runtimeException;
        } finally {
            relaySpan.end();
        }
    }

    public boolean isTracingAvailable() {
        return tracer != null && propagator != null;
    }
}
