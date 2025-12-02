"""
Kafka infrastructure package
"""

from .kafka_config import kafka_config, KafkaConfig
from .avro_producer_wrapper import AvroProducerWrapper

__all__ = [
    'kafka_config',
    'KafkaConfig',
    'AvroProducerWrapper',
]
