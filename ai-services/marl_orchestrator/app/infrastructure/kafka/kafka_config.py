"""
Kafka Configuration - Factory for creating Kafka clients

Provides reusable, pre-configured Kafka consumer and producer instances
with Avro serialization and Schema Registry integration.

Uses the new confluent_kafka.schema_registry module (replacing deprecated confluent_kafka.avro).
"""

from typing import Dict, Any, Tuple
from confluent_kafka import Consumer
from confluent_kafka.avro import AvroProducer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroDeserializer
from app.core.config import settings
from app.core.logging import logger


class KafkaConfig:
    """
    Kafka client factory for creating consumers and producers.
    
    Centralizes Kafka configuration and provides factory methods
    for creating Avro-enabled Kafka clients.
    """
    
    _schema_registry_client = None
    _avro_deserializer = None
    
    @classmethod
    def get_schema_registry_client(cls) -> SchemaRegistryClient:
        """Get or create a singleton Schema Registry client."""
        if cls._schema_registry_client is None:
            schema_registry_conf = {'url': settings.schema_registry_url}
            cls._schema_registry_client = SchemaRegistryClient(schema_registry_conf)
            logger.info(f"Schema Registry client created for: {settings.schema_registry_url}")
        return cls._schema_registry_client
    
    @classmethod
    def get_avro_deserializer(cls) -> AvroDeserializer:
        """Get or create a singleton Avro deserializer."""
        if cls._avro_deserializer is None:
            cls._avro_deserializer = AvroDeserializer(cls.get_schema_registry_client())
            logger.info("Avro deserializer created")
        return cls._avro_deserializer
    
    @staticmethod
    def create_consumer_with_deserializer(
        group_id: str,
        auto_offset_reset: str = 'latest',
        enable_auto_commit: bool = False,
        additional_config: Dict[str, Any] = None
    ) -> Tuple[Consumer, AvroDeserializer]:
        """
        Create pre-configured Kafka consumer with Schema Registry Avro deserializer.
        
        Uses the new confluent_kafka.schema_registry module which properly handles
        the Schema Registry wire format (magic byte + schema ID + Avro binary data).
        
        Args:
            group_id: Consumer group identifier
            auto_offset_reset: Where to start consuming (earliest/latest)
            enable_auto_commit: Whether to auto-commit offsets
            additional_config: Additional Kafka consumer configuration
        
        Returns:
            Tuple of (Consumer, AvroDeserializer) for manual deserialization
        """
        consumer_config = {
            'bootstrap.servers': settings.kafka_bootstrap_servers,
            'group.id': group_id,
            'auto.offset.reset': auto_offset_reset,
            'enable.auto.commit': enable_auto_commit,
            'session.timeout.ms': 30000,
            'heartbeat.interval.ms': 10000,
            'max.poll.interval.ms': 300000,
        }
        
        # Merge additional config
        if additional_config:
            consumer_config.update(additional_config)
        
        logger.info(f"Creating Kafka consumer with group_id: {group_id}")
        consumer = Consumer(consumer_config)
        deserializer = KafkaConfig.get_avro_deserializer()
        
        return consumer, deserializer
    
    @staticmethod
    def create_avro_producer(
        additional_config: Dict[str, Any] = None
    ) -> AvroProducer:
        """
        Create pre-configured Avro producer with Schema Registry support.
        
        Includes reliability settings (acks=all, retries) and performance tuning.
        
        Args:
            additional_config: Additional Kafka producer configuration
        
        Returns:
            Configured AvroProducer instance
        """
        producer_config = {
            'bootstrap.servers': settings.kafka_bootstrap_servers,
            'schema.registry.url': settings.schema_registry_url,
            # Reliability settings
            'acks': 'all',
            'retries': 3,
            'max.in.flight.requests.per.connection': 5,
            # Performance tuning
            'compression.type': 'snappy',
            'linger.ms': 10,
            'batch.size': 16384,
        }
        
        # Merge additional config
        if additional_config:
            producer_config.update(additional_config)
        
        logger.info("Creating Avro producer")
        return AvroProducer(producer_config)
    
    @staticmethod
    def create_avro_producer_with_schema(
        value_schema,
        key_schema=None,
        additional_config: Dict[str, Any] = None
    ) -> AvroProducer:
        """
        Create pre-configured Avro producer with explicit schemas.
        
        Args:
            value_schema: Avro schema for message values
            key_schema: Optional Avro schema for message keys
            additional_config: Additional Kafka producer configuration
        
        Returns:
            Configured AvroProducer instance with schemas
        """
        producer_config = {
            'bootstrap.servers': settings.kafka_bootstrap_servers,
            'schema.registry.url': settings.schema_registry_url,
            # Reliability settings
            'acks': 'all',
            'retries': 3,
            'max.in.flight.requests.per.connection': 5,
            # Performance tuning
            'compression.type': 'snappy',
            'linger.ms': 10,
            'batch.size': 16384,
        }
        
        # Merge additional config
        if additional_config:
            producer_config.update(additional_config)
        
        logger.info("Creating Avro producer with explicit schemas")
        return AvroProducer(
            producer_config,
            default_value_schema=value_schema,
            default_key_schema=key_schema
        )

kafka_config = KafkaConfig()
