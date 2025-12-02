"""
Fraud Detection Request Listener - Consumes FraudDetectionRequest from Kafka

Thin Kafka consumer that listens to fraud detection requests
and delegates processing to FraudDetectionRequestHandler.
"""

import asyncio
from threading import Thread
from confluent_kafka import KafkaError

from app.infrastructure.kafka.kafka_config import kafka_config
from app.handlers.fraud_detection_request_handler import fraud_detection_request_handler
from app.core.config import settings
from app.core.logging import logger


class FraudDetectionRequestListener:
    """
    Kafka consumer for fraud detection requests.
    
    Runs Kafka polling in a separate thread to avoid blocking the async event loop.
    Async message processing is submitted back to the main event loop.
    """
    
    def __init__(self):
        """Initialize listener with Kafka consumer and handler."""
        # Get consumer from factory
        self.consumer = kafka_config.create_avro_consumer(
            group_id=settings.kafka_consumer_group
        )
        
        # Inject handler
        self.handler = fraud_detection_request_handler
        
        self.topic = settings.fraud_request_topic
        self.running = False
        self._thread = None
        self._loop = None  # Reference to main event loop
        
        logger.info(f"FraudDetectionRequestListener initialized for topic: {self.topic}")
    
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
        try:
            while self.running:
                msg = self.consumer.poll(timeout=1.0)
                
                if msg is None:
                    continue
                
                if msg.error():
                    if msg.error().code() != KafkaError._PARTITION_EOF:
                        logger.error(f"Kafka error: {msg.error()}")
                    continue
                
                try:
                    # Log raw message info for debugging
                    logger.info(f"Received message from {msg.topic()} partition {msg.partition()} offset {msg.offset()}")
                    
                    future = asyncio.run_coroutine_threadsafe(
                        self.handler.handle(msg.value()),
                        self._loop
                    )
                    
                    future.result(timeout=30)
                except Exception as e:
                    logger.error(f"Error processing message: {str(e)}")
                    logger.debug(f"Message details - Topic: {msg.topic()}, Partition: {msg.partition()}, Offset: {msg.offset()}")
                    # TODO: Publish to DLQ or retry queue
        except Exception as e:
            logger.error(f"Error in consumer loop: {str(e)}")
        finally:
            self._cleanup()
    
    def stop(self):
        """Stop the listener gracefully."""
        logger.info("Stopping fraud request listener...")
        self.running = False
    
    def _cleanup(self):
        """Clean up resources."""
        if self.consumer:
            self.consumer.close()
            logger.info("Consumer closed")

fraud_detection_request_listener = FraudDetectionRequestListener()
