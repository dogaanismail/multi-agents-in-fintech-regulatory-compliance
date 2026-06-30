"""
Base Avro consumer.

Runs librdkafka polling in a background thread and submits async processing
back to the main event loop. Failure handling is uniform across listeners:

  * deserialization / null payloads      -> dead-letter topic, then commit
  * handler failures                      -> bounded retry with exponential
                                             backoff, then dead-letter topic
  * successful processing                 -> commit

Offsets are committed only once a message has been processed or safely parked
in the dead-letter topic, so a poison message can never deadlock its partition
or spin in a tight infinite retry loop.
"""

import asyncio
import time
from threading import Thread

from confluent_kafka import KafkaError, KafkaException

from opentelemetry import context as otel_context
from opentelemetry import trace
from opentelemetry.trace import SpanKind

from app.core import metrics
from app.core.config import settings
from app.core.logging import logger
from app.core.telemetry import (
    extract_context_from_kafka_headers,
    run_coroutine_with_context,
)
from app.infrastructure.kafka.dead_letter_publisher import dead_letter_publisher
from app.infrastructure.kafka.kafka_config import kafka_config

tracer = trace.get_tracer(__name__)


class AvroConsumerThread:

    def __init__(self, name: str, topic: str, handler, handler_timeout: int = 30):
        self.name = name
        self.topic = topic
        self.handler = handler
        self.handler_timeout = handler_timeout

        self.consumer, self.deserializer = kafka_config.create_consumer_with_deserializer(
            group_id=settings.kafka_consumer_group,
            auto_offset_reset="latest",
            enable_auto_commit=False,
            additional_config={
                "statistics.interval.ms": settings.kafka_statistics_interval_ms,
                "stats_cb": metrics.update_lag_from_stats,
            },
        )

        self.running = False
        self._thread = None
        self._loop = None
        logger.info(f"{self.name} initialized for topic: {self.topic}")

    async def start(self):
        self.consumer.subscribe([self.topic])
        self.running = True
        self._loop = asyncio.get_event_loop()
        logger.info(f"Started listening to topic: {self.topic}")
        self._thread = Thread(target=self._consume_loop, daemon=True)
        self._thread.start()

    def stop(self):
        logger.info(f"Stopping {self.name}...")
        self.running = False

    def _consume_loop(self):
        while self.running:
            try:
                msg = self.consumer.poll(timeout=1.0)

                if msg is None:
                    continue

                if msg.error():
                    if msg.error().code() != KafkaError._PARTITION_EOF:
                        logger.error(f"Kafka error: {msg.error()}")
                    continue

                self._handle_message(msg)

            except KafkaException as error:
                logger.error(f"Kafka exception in consumer loop: {str(error)}")
                continue
            except Exception as error:
                logger.error(f"Unexpected error in consumer loop: {str(error)}")
                continue

        self._cleanup()

    def _handle_message(self, msg):
        metrics.messages_consumed.labels(topic=msg.topic()).inc()
        logger.info(f"Received message from {msg.topic()} partition {msg.partition()} offset {msg.offset()}")

        raw_value = msg.value()
        if raw_value is None:
            logger.warning(
                f"Null-value message at {msg.topic()} [{msg.partition()}] offset {msg.offset()}; routing to dead-letter topic."
            )
            self._route_to_dlt(msg, "message value was null", "null_value")
            return

        try:
            deserialized_value = self.deserializer(raw_value, None)
        except Exception as deser_error:
            logger.warning(
                f"Avro deserialization failed at {msg.topic()} [{msg.partition()}] offset {msg.offset()}: {str(deser_error)}"
            )
            self._route_to_dlt(msg, deser_error, "deserialization_error")
            return

        if deserialized_value is None:
            logger.warning(
                f"Message deserialized to None at {msg.topic()} [{msg.partition()}] offset {msg.offset()}; routing to dead-letter topic."
            )
            self._route_to_dlt(msg, "message deserialized to None", "null_payload")
            return

        if self._process_with_retry(msg, deserialized_value):
            metrics.messages_processed.labels(topic=msg.topic()).inc()
            self._commit(msg)

    def _process_with_retry(self, msg, value) -> bool:
        delay = settings.kafka_retry_backoff_seconds
        last_error = None

        for attempt in range(1, settings.kafka_max_delivery_attempts + 1):
            try:
                self._invoke_handler(msg, value)
                return True
            except Exception as handler_error:
                last_error = handler_error
                logger.error(
                    f"Handler failed for {msg.topic()} [{msg.partition()}] offset {msg.offset()} "
                    f"(attempt {attempt}/{settings.kafka_max_delivery_attempts}): {str(handler_error)}"
                )
                if attempt < settings.kafka_max_delivery_attempts:
                    metrics.processing_retries.labels(topic=msg.topic()).inc()
                    time.sleep(min(delay, settings.kafka_retry_backoff_max_seconds))
                    delay *= 2

        logger.error(
            f"Exhausted {settings.kafka_max_delivery_attempts} delivery attempts for "
            f"{msg.topic()} [{msg.partition()}] offset {msg.offset()}; routing to dead-letter topic."
        )
        self._route_to_dlt(msg, last_error, "processing_error")
        return False

    def _invoke_handler(self, msg, value):
        parent_ctx = extract_context_from_kafka_headers(msg.headers())
        with tracer.start_as_current_span(
            f"{msg.topic()} process",
            context=parent_ctx,
            kind=SpanKind.CONSUMER,
        ) as span:
            span.set_attribute("messaging.system", "kafka")
            span.set_attribute("messaging.destination.name", msg.topic())
            span.set_attribute("messaging.kafka.partition", msg.partition())
            span.set_attribute("messaging.kafka.offset", msg.offset())

            captured_ctx = otel_context.get_current()
            future = asyncio.run_coroutine_threadsafe(
                run_coroutine_with_context(self.handler.handle(value), captured_ctx),
                self._loop,
            )
            future.result(timeout=self.handler_timeout)

    def _route_to_dlt(self, msg, error, reason: str):
        if dead_letter_publisher.publish(msg, error, reason):
            metrics.dlt_messages.labels(topic=msg.topic(), reason=reason).inc()
            self._commit(msg)
        else:
            logger.error(
                f"Could not park message in dead-letter topic for {msg.topic()} "
                f"[{msg.partition()}] offset {msg.offset()}; leaving offset uncommitted for replay."
            )

    def _commit(self, msg):
        try:
            self.consumer.commit(message=msg)
        except KafkaException as commit_error:
            metrics.commit_failures.labels(topic=msg.topic()).inc()
            logger.error(
                f"Offset commit failed for {msg.topic()} [{msg.partition()}] offset {msg.offset()}: {str(commit_error)}"
            )

    def _cleanup(self):
        if self.consumer:
            self.consumer.close()
            logger.info(f"{self.name} consumer closed")
