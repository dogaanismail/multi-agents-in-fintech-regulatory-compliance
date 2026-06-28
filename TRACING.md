# Observability: Logs + Distributed Traces in Grafana

End-to-end **distributed tracing** and **centralised logs** across all Java
microservices and the Python AI services, viewed together in **Grafana**. A single
request (e.g. a payment) can be followed from the first HTTP call through every Kafka
hop and ML-agent call as one trace, and its log lines are correlated to that trace.

Implements GitHub issue
[#80](https://github.com/dogaanismail/multi-agents-in-fintech-regulatory-compliance/issues/80).

## What you get
- **Traces** (Tempo): one trace per request, spanning HTTP **and** Kafka, across Java + Python.
- **Logs** (Loki): every container's logs in one place, each line carrying its trace id.
- **Correlation**: from a trace span jump to its logs, and from a log line jump to its trace.
- One UI: **Grafana → http://localhost:3009** (anonymous admin in dev).

## Stack (added to `bank-solution-backend/docker-compose.yml`)
| Component | Image | Role | Port |
|---|---|---|---|
| **Tempo** | `grafana/tempo` | Receives OTLP spans, stores traces | 3200 (query), 4317/4318 (OTLP) |
| **Loki** | `grafana/loki` | Stores logs | 3100 |
| **Alloy** | `grafana/alloy` | Tails all Docker container logs → Loki | 12345 (UI) |
| **Grafana** | `grafana/grafana` | UI for logs + traces | 3009 |

Config lives in `bank-solution-backend/observability/` (`tempo/`, `loki/`, `alloy/`,
`grafana/provisioning/`). All four attach to both `bank-network` and
`aml-multi-agent-network` so Java and Python services can reach them.

## Architecture

```
            ┌────────────────────── one trace (W3C traceparent) ──────────────────────┐
HTTP →  gateway → payment-svc ─kafka▶ payment-engine ─kafka▶ risk-engine ─┬─Feign HTTP─▶ account / customer-profile / network-topology
                                                                          └─kafka(fraud.analysis.requested)─▶ marl-orchestrator (Python)
                                                                                       │ ├─httpx─▶ transaction-pattern-agent
                                                                                       │ ├─httpx─▶ customer-risk-agent
                                                                                       │ └─httpx─▶ network-analysis-agent
                                                                                       └─kafka(fraud.analysis.completed)─▶ risk-engine ─kafka▶ payment-engine → account-svc …

   spans ─OTLP──▶ Tempo ┐
                         ├──▶ Grafana (correlated on trace id)
   container logs ─▶ Alloy ─▶ Loki ┘
```

## How it works

### Java services (Spring Boot 4)
Spring Boot 4.0 modularized observability, so tracing is enabled via the dedicated
auto-configuration starters (in `:common`, inherited by all services, plus the gateway):
- `org.springframework.boot:spring-boot-starter-opentelemetry` — OpenTelemetry SDK + OTLP exporter.
- `org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry` — Micrometer Tracing → OTel bridge.
- `io.github.openfeign:feign-micrometer` — propagates trace context across Feign HTTP calls.

Properties (per service `application.properties`):
```properties
management.tracing.sampling.probability=${TRACING_SAMPLING_PROBABILITY:1.0}
management.opentelemetry.tracing.export.otlp.endpoint=${OTLP_TRACING_ENDPOINT:http://localhost:4318/v1/traces}
# Only traces are exported over OTLP. Metrics -> Prometheus; logs -> Loki via Alloy.
management.otlp.metrics.export.enabled=false
management.logging.export.otlp.enabled=false
```

**Kafka tracing** is enabled on the *custom* beans (these services define their own
`KafkaTemplate` / listener factories, so Boot properties don't apply):
- producers: `template.setObservationEnabled(true)` in each `KafkaProducerConfig`.
- consumers: `factory.getContainerProperties().setObservationEnabled(true)` in each `KafkaConsumerConfig`.

This writes/reads the W3C `traceparent` in the Kafka record headers.

**Logs**: in Docker, `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` emits JSON logs (incl.
`trace.id`/`span.id`) for clean Loki parsing; local `bootRun` stays human-readable text.

### Python AI services (OpenTelemetry SDK)
- `app/core/telemetry.py` instruments **FastAPI** (inbound) and **httpx** (orchestrator → agents)
  and exports OTLP to `OTEL_EXPORTER_OTLP_ENDPOINT` (Tempo).
- The Java↔Python Kafka boundary is propagated manually: consumers extract `traceparent`
  and continue the trace into the async handler; the publisher injects `traceparent` into
  outbound headers.
- Every log line carries `trace_id` / `span_id` (orchestrator logs are JSON).

## Running it
```bash
# 1) Kafka + Schema Registry (the avro-schema-library compose), then:
cd bank-solution-backend && docker compose up -d     # includes Tempo/Loki/Alloy/Grafana
cd ../ai-services        && docker compose up -d
```
Alloy reads the Docker socket, so it collects logs from **all** containers automatically.

## Verifying end-to-end
1. Trigger a payment (gateway or payment-svc directly), or run `simulation_tests/`.
2. Open Grafana → **http://localhost:3009**.
3. **Explore → Tempo**: Search → open a trace. Spans should span `payment-svc`,
   `payment-engine`, `risk-engine`, `marl-orchestrator`, and the three agents, with the
   Kafka hops linked.
4. **Explore → Loki**: `{job="docker"}` or `{container="risk-engine-svc-app"}`; filter a
   single trace with `{job="docker"} |= "<traceId>"`.
5. **Correlation**: in a Tempo span use "Logs for this span"; on a Loki line click the
   **TraceID** derived field to open the trace.

## Configuration reference
| Setting | Java (env) | Python (env) | Default |
|---|---|---|---|
| OTLP endpoint | `OTLP_TRACING_ENDPOINT` | `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318[/v1/traces]` |
| Sampling | `TRACING_SAMPLING_PROBABILITY` | (always on) | `1.0` |
| JSON logs | `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` (Docker) | (orchestrator JSON already) | text locally |

> Sampling = 1.0 (every request) is for dev/demo so each payment is fully traceable.
> Lower it in production.

## Notes & limitations
- All app instrumentation is OTLP/vendor-neutral — swapping Tempo for another backend
  (or adding an OpenTelemetry Collector) is a compose change, not a code change.
- Axon Framework's internal event-store topic (`payment-domain-events`) is not
  observation-instrumented; the cross-service integration events are.
- Java async / Axon / reactive logs may not carry the trace id (the active span lives on
  the request/listener thread); full coverage needs context-propagation wiring.
- Grafana is on host port **3009** (3001 is commonly taken by other local tooling).
