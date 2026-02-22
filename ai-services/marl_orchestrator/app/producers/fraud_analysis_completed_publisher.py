"""
Fraud Analysis Completed Publisher - Publishes FraudAnalysisCompleted to Kafka

Responsible for publishing MADDPG fraud analysis decisions back to Kafka.
"""

from typing import Any
import requests
from confluent_kafka import avro

from app.infrastructure.kafka.kafka_config import kafka_config
from app.infrastructure.kafka.avro_producer_wrapper import AvroProducerWrapper
from app.core.config import settings
from app.core.logging import logger


class FraudAnalysisCompletedPublisher:
    """
    Publisher for fraud analysis completed events.
    
    Publishes FraudAnalysisCompletedEvent (Avro) to Kafka after MADDPG makes a decision.
    Uses dependency injection pattern with singleton instances.
    """
    
    def __init__(self):
        """Declare publisher — actual Kafka/Schema Registry connection is deferred to first use."""
        self.value_schema = None
        self.key_schema = None
        self.producer_wrapper = None
        self.topic = settings.fraud_analysis_completed_topic
        self._initialized = False
        logger.info(f"FraudAnalysisCompletedPublisher created (lazy) for topic: {self.topic}")

    def _ensure_initialized(self) -> bool:
        """Connect to Schema Registry and create producer on first use."""
        if self._initialized:
            return True
        try:
            self.value_schema = self._load_schema_from_registry()
            self.key_schema = avro.loads('{"type": "string"}')
            producer = kafka_config.create_avro_producer_with_schema(
                value_schema=self.value_schema,
                key_schema=self.key_schema
            )
            self.producer_wrapper = AvroProducerWrapper(producer)
            self._initialized = True
            logger.info(f"✅ FraudAnalysisCompletedPublisher connected to Schema Registry and Kafka")
            return True
        except Exception as e:
            logger.warning(
                f"⚠️ FraudAnalysisCompletedPublisher not yet available "
                f"(Schema Registry unreachable): {e}"
            )
            return False
    
    def _load_schema_from_registry(self):
        """Load FraudAnalysisCompletedEvent schema from Schema Registry."""
        try:
            schema_url = f"{settings.schema_registry_url}/subjects/fraud.analysis.completed-value/versions/latest"
            response = requests.get(schema_url)
            response.raise_for_status()
            schema_str = response.json()['schema']
            logger.info("Loaded FraudAnalysisCompletedEvent schema from registry")
            return avro.loads(schema_str)
        except Exception as e:
            logger.error(f"Failed to load schema from registry: {e}")
            raise
    
    def publish(
        self,
        response: Any,
        payment_id: str = None
    ) -> bool:
        """
        Publish fraud analysis completed event to Kafka.
        
        Args:
            response: CoordinatedDecisionResponse or Avro dict
            payment_id: Optional payment ID for logging
        
        Returns:
            True if published successfully
        """
        if not self._ensure_initialized():
            logger.warning(
                f"Cannot publish fraud analysis completed event for payment {payment_id}: "
                f"Schema Registry / Kafka not available yet"
            )
            return False

        try:
            # Convert to Avro format if needed
            avro_response = self._to_avro_format(response)
            
            # Publish to Kafka
            success = self.producer_wrapper.send(
                topic=self.topic,
                value=avro_response,
                key=payment_id
            )
            
            if success:
                logger.info(
                    f"Published fraud analysis completed event for payment {payment_id} "
                    f"to topic {self.topic}"
                )
            else:
                logger.error(f"Failed to publish fraud analysis completed event for payment {payment_id}")
            
            return success
            
        except Exception as e:
            logger.error(f"Error publishing fraud analysis completed event: {str(e)}")
            logger.exception("Full traceback:")
            return False
    
    def _to_avro_format(self, response: Any) -> dict:
        """
        Convert CoordinatedDecisionResponse to Avro FraudAnalysisCompletedEvent format.
        
        Args:
            response: CoordinatedDecisionResponse object or dict (pre-formatted by handler)
        
        Returns:
            Avro-compatible dictionary matching FraudAnalysisCompletedEvent.avsc
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

fraud_analysis_completed_publisher = FraudAnalysisCompletedPublisher()
