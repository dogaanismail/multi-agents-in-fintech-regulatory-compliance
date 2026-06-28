"""
Fraud Analysis Requested Listener - Consumes FraudAnalysisRequested from Kafka

Thin Kafka consumer that listens to fraud analysis requests
and delegates processing to FraudAnalysisRequestedHandler.

Uses the new confluent_kafka.schema_registry module for proper Avro deserialization.
"""

import asyncio
from threading import Thread
from confluent_kafka import KafkaError, KafkaException

from opentelemetry import context as otel_context
from opentelemetry import trace
from opentelemetry.trace import SpanKind

from app.infrastructure.kafka.kafka_config import kafka_config
from app.handlers.fraud_analysis_requested_handler import fraud_analysis_requested_handler
from app.core.config import settings
from app.core.logging import logger
from app.core.telemetry import (
    extract_context_from_kafka_headers,
    run_coroutine_with_context,
)

tracer = trace.get_tracer(__name__)


class FraudAnalysisRequestedListener:
    """
    Kafka consumer for fraud analysis requests.
    
    Runs Kafka polling in a separate thread to avoid blocking the async event loop.
    Async message processing is submitted back to the main event loop.
    
    Uses the new Schema Registry AvroDeserializer for proper message deserialization.
    """
    
    def __init__(self):
        """Initialize listener with Kafka consumer and deserializer."""
        self.consumer, self.deserializer = kafka_config.create_consumer_with_deserializer(
            group_id=settings.kafka_consumer_group,
            auto_offset_reset='latest',
            enable_auto_commit=False
        )
        
        self.handler = fraud_analysis_requested_handler
        
        self.topic = settings.fraud_analysis_requested_topic
        self.running = False
        self._thread = None
        self._loop = None 
        
        logger.info(f"FraudAnalysisRequestedListener initialized for topic: {self.topic}")
    
    async def start(self):
        """Start listening to Kafka topic in a separate thread."""
        # Subscribe to topic
        self.consumer.subscribe([self.topic])
        
        self.running = True
        
        # Store reference to the current event loop
        self._loop = asyncio.get_event_loop()
        
        logger.info(f"Started listening to topic: {self.topic}")
        
        # Run blocking Kafka consumer in a separate thread
        self._thread = Thread(target=self._consume_loop, daemon=True)
        self._thread.start()
    
    def _consume_loop(self):
        """Blocking loop that runs in separate thread."""
        while self.running:
            try:
                msg = self.consumer.poll(timeout=1.0)
                
                if msg is None:
                    continue
                
                if msg.error():
                    if msg.error().code() != KafkaError._PARTITION_EOF:
                        logger.error(f"Kafka error: {msg.error()}")
                    continue
                
                # Log raw message info for debugging
                logger.info(f"Received message from {msg.topic()} partition {msg.partition()} offset {msg.offset()}")
                
                # Get raw bytes and deserialize using Schema Registry deserializer
                raw_value = msg.value()
                if raw_value is None:
                    logger.warning(
                        f"Skipping message with null value at {msg.topic()} "
                        f"[{msg.partition()}] offset {msg.offset()}."
                    )
                    continue
                
                # Deserialize Avro message using Schema Registry
                try:
                    deserialized_value = self.deserializer(raw_value, None)
                except Exception as deser_error:
                    logger.warning(
                        f"Skipping message with Avro deserialization error at {msg.topic()} "
                        f"[{msg.partition()}] offset {msg.offset()}: {str(deser_error)}. "
                        f"Ensure all producers are using Avro serialization with Schema Registry."
                    )
                    continue
                
                if deserialized_value is None:
                    logger.warning(
                        f"Skipping message with failed deserialization at {msg.topic()} "
                        f"[{msg.partition()}] offset {msg.offset()}."
                    )
                    continue
                
                try:
                    # Continue the distributed trace started upstream (risk-engine)
                    # by extracting the W3C trace context from the Kafka headers.
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

                        # Carry the consumer span into the asyncio handler so the
                        # agent HTTP calls and the produced event link to this trace.
                        captured_ctx = otel_context.get_current()
                        future = asyncio.run_coroutine_threadsafe(
                            run_coroutine_with_context(
                                self.handler.handle(deserialized_value), captured_ctx
                            ),
                            self._loop
                        )
                        future.result(timeout=30)
                        # Commit offset only after successful processing
                        # (enable_auto_commit=False, so we control exactly-once commits)
                        self.consumer.commit(message=msg)
                except Exception as e:
                    logger.error(f"Error processing message: {str(e)}")
                    logger.debug(f"Message details - Topic: {msg.topic()}, Partition: {msg.partition()}, Offset: {msg.offset()}")
                    # TODO: Publish to DLQ or retry queue
                    
            except KafkaException as e:
                logger.error(f"Kafka exception in consumer loop: {str(e)}")
                continue
            except Exception as e:
                logger.error(f"Unexpected error in consumer loop: {str(e)}")
                # Don't break the loop, continue processing
                continue
        
        # Cleanup after loop exits
        self._cleanup()
    
    def stop(self):
        """Stop the listener gracefully."""
        logger.info("Stopping fraud analysis request listener...")
        self.running = False
    
    def _cleanup(self):
        """Clean up resources."""
        if self.consumer:
            self.consumer.close()
            logger.info("Consumer closed")

fraud_analysis_requested_listener = FraudAnalysisRequestedListener()
