"""
Compliance Agent Manual Feedback Listener - Consumes ComplianceAgentManualFeedbackEvent from Kafka

Thin Kafka consumer that listens to compliance officer feedback events
and delegates processing to ComplianceAgentManualFeedbackHandler.
"""

import asyncio
from threading import Thread
from confluent_kafka import KafkaError, KafkaException

from opentelemetry import context as otel_context
from opentelemetry import trace
from opentelemetry.trace import SpanKind

from app.infrastructure.kafka.kafka_config import kafka_config
from app.handlers.compliance_agent_manual_feedback_handler import compliance_agent_manual_feedback_handler
from app.core.config import settings
from app.core.logging import logger
from app.core.telemetry import (
    extract_context_from_kafka_headers,
    run_coroutine_with_context,
)

tracer = trace.get_tracer(__name__)


class ComplianceAgentManualFeedbackListener:

    def __init__(self):
        self.consumer, self.deserializer = kafka_config.create_consumer_with_deserializer(
            group_id=settings.kafka_consumer_group,
            auto_offset_reset='latest',
            enable_auto_commit=False
        )
        self.handler = compliance_agent_manual_feedback_handler
        self.topic = settings.agent_manual_feedback_topic
        self.running = False
        self._thread = None
        self._loop = None
        logger.info(f"ComplianceAgentManualFeedbackListener initialized for topic: {self.topic}")

    async def start(self):
        self.consumer.subscribe([self.topic])
        self.running = True
        self._loop = asyncio.get_event_loop()
        logger.info(f"Started listening to topic: {self.topic}")
        self._thread = Thread(target=self._consume_loop, daemon=True)
        self._thread.start()

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

                logger.info(f"Received message from {msg.topic()} partition {msg.partition()} offset {msg.offset()}")

                raw_value = msg.value()
                if raw_value is None:
                    logger.warning(
                        f"Skipping message with null value at {msg.topic()} "
                        f"[{msg.partition()}] offset {msg.offset()}."
                    )
                    continue

                try:
                    deserialized_value = self.deserializer(raw_value, None)
                except Exception as deser_error:
                    logger.warning(
                        f"Skipping message with Avro deserialization error at {msg.topic()} "
                        f"[{msg.partition()}] offset {msg.offset()}: {str(deser_error)}."
                    )
                    continue

                if deserialized_value is None:
                    logger.warning(
                        f"Skipping message with failed deserialization at {msg.topic()} "
                        f"[{msg.partition()}] offset {msg.offset()}."
                    )
                    continue

                try:
                    # Continue the distributed trace by extracting the W3C trace
                    # context from the Kafka headers (set by the upstream producer).
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
                            run_coroutine_with_context(
                                self.handler.handle(deserialized_value), captured_ctx
                            ),
                            self._loop
                        )
                        future.result(timeout=30)
                        self.consumer.commit(message=msg)
                except Exception as e:
                    logger.error(f"Error processing message: {str(e)}")

            except KafkaException as e:
                logger.error(f"Kafka exception in consumer loop: {str(e)}")
                continue
            except Exception as e:
                logger.error(f"Unexpected error in consumer loop: {str(e)}")
                continue

        self._cleanup()

    def stop(self):
        logger.info("Stopping compliance agent manual feedback listener...")
        self.running = False

    def _cleanup(self):
        if self.consumer:
            self.consumer.close()
            logger.info("ComplianceAgentManualFeedbackListener consumer closed")


compliance_agent_manual_feedback_listener = ComplianceAgentManualFeedbackListener()
