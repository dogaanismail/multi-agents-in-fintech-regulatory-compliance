"""
Kafka Configuration - Factory for creating Kafka clients

Provides reusable, pre-configured Kafka consumer and producer instances
with Avro serialization and Schema Registry integration.
"""

from typing import Dict, Any
from confluent_kafka.avro import AvroConsumer, AvroProducer
from app.core.config import settings
from app.core.logging import logger


class KafkaConfig:
    """
    Kafka client factory for creating consumers and producers.
    
    Centralizes Kafka configuration and provides factory methods
    for creating Avro-enabled Kafka clients.
    """
    
    @staticmethod
    def create_avro_consumer(
        group_id: str,
        auto_offset_reset: str = 'latest',
        enable_auto_commit: bool = True,
        additional_config: Dict[str, Any] = None
    ) -> AvroConsumer:
        """
        Create pre-configured Avro consumer with Schema Registry support.
        
        Args:
            group_id: Consumer group identifier
            auto_offset_reset: Where to start consuming (earliest/latest)
            enable_auto_commit: Whether to auto-commit offsets
            additional_config: Additional Kafka consumer configuration
        
        Returns:
            Configured AvroConsumer instance
        """
        consumer_config = {
            'bootstrap.servers': settings.kafka_bootstrap_servers,
            'group.id': group_id,
            'auto.offset.reset': auto_offset_reset,
            'enable.auto.commit': enable_auto_commit,
            'schema.registry.url': settings.schema_registry_url,
            # Performance tuning
            'session.timeout.ms': 30000,
            'heartbeat.interval.ms': 10000,
            'max.poll.interval.ms': 300000,
        }
        
        # Merge additional config
        if additional_config:
            consumer_config.update(additional_config)
        
        logger.info(f"Creating Avro consumer with group_id: {group_id}")
        return AvroConsumer(consumer_config)
    
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
            'acks': 'all',  # Wait for all replicas
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


# Singleton factory instance
kafka_config = KafkaConfig()
