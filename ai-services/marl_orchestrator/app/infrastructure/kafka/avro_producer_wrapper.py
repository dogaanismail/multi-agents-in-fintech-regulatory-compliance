"""
Reusable Avro Producer Wrapper

Generic producer that can publish any Avro message.
Similar to Spring's KafkaTemplate<K,V>.
"""

from typing import Dict, Any, List, Optional, Tuple
from confluent_kafka.avro import AvroProducer
from confluent_kafka.avro.serializer import SerializerError

from app.core.logging import logger


class AvroProducerWrapper:
    """
    Reusable Avro producer wrapper.
    
    Similar to Spring Boot's KafkaTemplate<K,V>.
    Handles Avro serialization and error handling.
    """
    
    def __init__(self, producer: AvroProducer):
        """
        Initialize producer wrapper.
        
        Args:
            producer: Configured AvroProducer instance from KafkaConfig
        """
        self.producer = producer
    
    def send(
        self,
        topic: str,
        value: Dict[str, Any],
        key: Optional[str] = None,
        callback: Optional[callable] = None,
        headers: Optional[List[Tuple[str, bytes]]] = None
    ) -> bool:
        """
        Send Avro message to Kafka topic.

        Args:
            topic: Kafka topic name
            value: Message value (will be Avro-serialized)
            key: Optional message key
            callback: Optional delivery callback
            headers: Optional Kafka record headers (e.g. W3C trace context)

        Returns:
            True if sent successfully, False otherwise
        """
        try:
            self.producer.produce(
                topic=topic,
                value=value,
                key=key,
                callback=callback or self._default_callback,
                headers=headers
            )
            self.producer.poll(0)  # Trigger callbacks
            return True
            
        except SerializerError as e:
            logger.error(f"Avro serialization error for topic {topic}: {str(e)}")
            return False
        except Exception as e:
            logger.error(f"Error producing to topic {topic}: {str(e)}")
            return False
    
    def flush(self, timeout: float = 10.0):
        """
        Flush pending messages.
        
        Args:
            timeout: Flush timeout in seconds
        """
        remaining = self.producer.flush(timeout)
        if remaining > 0:
            logger.warning(f"{remaining} messages still in queue after flush")
    
    def close(self):
        """Close producer and flush remaining messages."""
        logger.info("Closing Avro producer")
        self.flush()
    
    @staticmethod
    def _default_callback(err, msg):
        """Default delivery callback."""
        if err:
            logger.error(f"Message delivery failed: {err}")
        else:
            logger.debug(f"Message delivered to {msg.topic()} [{msg.partition()}]")
