"""
OpenTelemetry distributed tracing setup.

Exports spans over OTLP/HTTP to Jaeger (configurable via OTEL_EXPORTER_OTLP_ENDPOINT)
and propagates the W3C trace context across:
  - FastAPI inbound requests (auto-instrumented),
  - httpx outbound calls (auto-instrumented),
  - Kafka messages (manual header inject/extract).

This lets a single payment be traced end-to-end across the Java backend and the
Python AI services in one Jaeger trace.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import logging
import os
from typing import List, Optional, Tuple

from opentelemetry import context as otel_context
from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.propagate import extract, inject
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

_initialized = False


def setup_telemetry(service_name: str, app=None, instrument_httpx: bool = False) -> None:
    """Initialise the global tracer provider and instrument FastAPI / httpx.

    Safe to call once per process; repeat calls only (re)instrument the app.

    Args:
        service_name: Logical service name shown in Jaeger (e.g. "marl-orchestrator").
        app: Optional FastAPI app to instrument for inbound requests.
        instrument_httpx: Whether to instrument outbound httpx calls (orchestrator only).
    """
    global _initialized

    if not _initialized:
        endpoint = os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4318").rstrip("/")
        traces_endpoint = f"{endpoint}/v1/traces"

        resource = Resource.create({"service.name": service_name})
        provider = TracerProvider(resource=resource)
        provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter(endpoint=traces_endpoint)))
        trace.set_tracer_provider(provider)

        if instrument_httpx:
            from opentelemetry.instrumentation.httpx import HTTPXClientInstrumentor
            HTTPXClientInstrumentor().instrument()

        _initialized = True

    if app is not None:
        from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
        FastAPIInstrumentor.instrument_app(app)


def get_tracer(name: str):
    """Convenience accessor for a named tracer."""
    return trace.get_tracer(name)


# ── Kafka W3C trace-context propagation ──────────────────────────────────────
# confluent-kafka represents headers as a list of (key, value-bytes) tuples.

def extract_context_from_kafka_headers(headers: Optional[List[Tuple[str, bytes]]]):
    """Build an OTel context from inbound Kafka headers so the consumer span
    continues the producer's trace."""
    carrier = {}
    for key, value in headers or []:
        if isinstance(value, (bytes, bytearray)):
            value = value.decode("utf-8", errors="replace")
        carrier[key] = value
    return extract(carrier)


def inject_context_into_kafka_headers() -> List[Tuple[str, bytes]]:
    """Serialise the active trace context into Kafka headers for a produced
    message, so the downstream (Java) consumer continues this trace."""
    carrier: dict = {}
    inject(carrier)
    return [(key, value.encode("utf-8")) for key, value in carrier.items()]


async def run_coroutine_with_context(coro, ctx):
    """Await ``coro`` with ``ctx`` attached as the current OTel context.

    The Kafka consumer span is created on the polling thread; attaching its
    context inside the asyncio handler makes the downstream work (httpx calls to
    the agents, the produced completed-event) link to that consumer span.
    """
    token = otel_context.attach(ctx)
    try:
        return await coro
    finally:
        otel_context.detach(token)
