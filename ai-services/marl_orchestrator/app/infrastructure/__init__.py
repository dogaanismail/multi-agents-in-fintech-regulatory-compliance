"""
Infrastructure layer - External integrations and technical concerns
"""

from .kafka import kafka_config, KafkaConfig, AvroProducerWrapper

__all__ = [
    'kafka_config',
    'KafkaConfig',
    'AvroProducerWrapper',
]
