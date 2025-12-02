"""
Fraud Detection Response Publisher - Publishes FraudDetectionResponse to Kafka

Responsible for publishing MADDPG fraud detection decisions back to Kafka.
"""

from typing import Any
import requests
from confluent_kafka import avro

from app.infrastructure.kafka.kafka_config import kafka_config
from app.infrastructure.kafka.avro_producer_wrapper import AvroProducerWrapper
from app.core.config import settings
from app.core.logging import logger


class FraudDetectionResponsePublisher:
    """
    Publisher for fraud detection responses.
    
    Publishes FraudDetectionResponse (Avro) to Kafka after MADDPG makes a decision.
    Uses dependency injection pattern with singleton instances.
    """
    
    def __init__(self):
        """Initialize publisher with Kafka producer from factory."""
        # Fetch response schema from Schema Registry
        self.value_schema = self._load_schema_from_registry()
        
        # Define string schema for key (transaction ID)
        self.key_schema = avro.loads('{"type": "string"}')
        
        # Create producer with schemas
        producer = kafka_config.create_avro_producer_with_schema(
            value_schema=self.value_schema,
            key_schema=self.key_schema
        )
        
        self.producer_wrapper = AvroProducerWrapper(producer)
        self.topic = settings.fraud_response_topic
        
        logger.info(f"FraudDetectionResponsePublisher initialized for topic: {self.topic}")
    
    def _load_schema_from_registry(self):
        """Load FraudDetectionResponse schema from Schema Registry."""
        try:
            schema_url = f"{settings.schema_registry_url}/subjects/fraud.detection.response-value/versions/latest"
            response = requests.get(schema_url)
            response.raise_for_status()
            schema_str = response.json()['schema']
            logger.info("Loaded FraudDetectionResponse schema from registry")
            return avro.loads(schema_str)
        except Exception as e:
            logger.error(f"Failed to load schema from registry: {e}")
            raise
    
    def publish(
        self,
        response: Any,
        transaction_id: str = None
    ) -> bool:
        """
        Publish fraud detection response to Kafka.
        
        Args:
            response: CoordinatedDecisionResponse or Avro dict
            transaction_id: Optional transaction ID for logging
        
        Returns:
            True if published successfully
        """
        try:
            # Convert to Avro format if needed
            avro_response = self._to_avro_format(response)
            
            # Publish to Kafka
            success = self.producer_wrapper.send(
                topic=self.topic,
                value=avro_response,
                key=transaction_id
            )
            
            if success:
                logger.info(
                    f"Published fraud response for transaction {transaction_id} "
                    f"to topic {self.topic}"
                )
            else:
                logger.error(f"Failed to publish fraud response for transaction {transaction_id}")
            
            return success
            
        except Exception as e:
            logger.error(f"Error publishing fraud response: {str(e)}")
            logger.exception("Full traceback:")
            return False
    
    def _to_avro_format(self, response: Any) -> dict:
        """
        Convert CoordinatedDecisionResponse to Avro FraudDetectionResponse format.
        
        Args:
            response: CoordinatedDecisionResponse object or dict (pre-formatted by handler)
        
        Returns:
            Avro-compatible dictionary matching FraudDetectionResponse.avsc
        """
        # If already a dict (from handler), return as-is
        if isinstance(response, dict):
            return response
        
        # Convert CoordinatedDecisionResponse to Avro format
        from datetime import datetime
        timestamp_ms = int(datetime.fromisoformat(response.timestamp).timestamp() * 1000)
        
        return {
            'requestId': getattr(response, 'request_id', None),
            'transactionId': response.transaction_id,
            'action': response.action.value, 
            'confidence': response.confidence,
            'maddpgQValue': response.maddpg_q_value,
            'transactionAgentObservation': self._observation_to_avro(response.transaction_agent_observation),
            'customerAgentObservation': self._observation_to_avro(response.customer_agent_observation),
            'networkAgentObservation': self._observation_to_avro(response.network_agent_observation),
            'agentContributions': response.agent_contributions,
            'processingTimeMs': response.processing_time_ms,
            'timestamp': timestamp_ms,
            'mode': response.mode
        }
    
    def _observation_to_avro(self, observation: Any) -> dict:
        """
        Convert AgentObservation to Avro format.
        
        Args:
            observation: AgentObservation object
        
        Returns:
            Avro-compatible observation dict matching AgentObservation.avsc
        """
        return {
            'agentName': observation.agent_name if hasattr(observation, 'agent_name') else 'unknown',
            'isSuspicious': observation.is_suspicious,
            'probability': observation.probability,
            'riskScore': observation.risk_score,
            'confidence': str(observation.confidence),
            'responseTimeMs': observation.response_time_ms
        }
    
    def close(self):
        """Close producer and flush messages."""
        self.producer_wrapper.close()

fraud_detection_response_publisher = FraudDetectionResponsePublisher()
