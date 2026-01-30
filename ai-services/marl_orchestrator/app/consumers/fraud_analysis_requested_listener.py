"""
Fraud Analysis Requested Listener - Consumes FraudAnalysisRequested from Kafka

Thin Kafka consumer that listens to fraud analysis requests
and delegates processing to FraudAnalysisRequestedHandler.

Uses the new confluent_kafka.schema_registry module for proper Avro deserialization.
"""

import asyncio
from threading import Thread
from confluent_kafka import KafkaError, KafkaException

from app.infrastructure.kafka.kafka_config import kafka_config
from app.handlers.fraud_analysis_requested_handler import fraud_analysis_requested_handler
from app.core.config import settings
from app.core.logging import logger


class FraudAnalysisRequestedListener:
    """
    Kafka consumer for fraud analysis requests.
    
    Runs Kafka polling in a separate thread to avoid blocking the async event loop.
    Async message processing is submitted back to the main event loop.
    
    Uses the new Schema Registry AvroDeserializer for proper message deserialization.
    """
    
    def __init__(self):
        """Initialize listener with Kafka consumer and deserializer."""
        # Get consumer and deserializer from factory
        # Use 'latest' to skip existing messages and only process new ones
        self.consumer, self.deserializer = kafka_config.create_consumer_with_deserializer(
            group_id=settings.kafka_consumer_group,
            auto_offset_reset='latest'
        )
        
        # Inject handler
        self.handler = fraud_analysis_requested_handler
        
        self.topic = settings.fraud_analysis_requested_topic
        self.running = False
        self._thread = None
        self._loop = None  # Reference to main event loop
        
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
                    future = asyncio.run_coroutine_threadsafe(
                        self.handler.handle(deserialized_value),
                        self._loop
                    )
                    
                    future.result(timeout=30)
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
