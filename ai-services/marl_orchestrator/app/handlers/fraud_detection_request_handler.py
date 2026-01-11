"""
Fraud Detection Request Handler

Handles incoming fraud detection requests by orchestrating the decision process.
Delegates to FraudDecisionService and publishes responses.
"""

from datetime import datetime
from typing import Dict, Any

from app.services.fraud_decision_service import fraud_decision_service
from app.producers.fraud_detection_response_publisher import fraud_detection_response_publisher
from app.core.logging import logger


class FraudDetectionRequestHandler:
    """
    Handler for fraud detection requests.
    
    Processes FraudDetectionRequest by delegating to service layer
    and publishing the response via FraudDetectionResponsePublisher.
    """
    
    def __init__(self):
        """Initialize handler with service dependencies."""
        self.fraud_decision_service = fraud_decision_service
        self.fraud_response_publisher = fraud_detection_response_publisher
        
        logger.info("FraudDetectionRequestHandler initialized")
    
    async def handle(self, request: Dict[str, Any]) -> bool:
        """
        Handle fraud detection request.
        
        Extracts fields, calls FraudDecisionService, and publishes response.
        
        Args:
            request: Deserialized FraudDetectionRequest (Avro format)
        
        Returns:
            True if handled successfully, False otherwise
        """
        try:
            # Extract request fields
            request_id = request.get('requestId')
            transaction_id = request.get('transactionId')
            transaction_features = request.get('transactionFeatures')
            customer_features = request.get('customerFeatures')
            network_features = request.get('networkFeatures')
            
            logger.info(f"Handling fraud request {request_id} for transaction {transaction_id}")
            
            # Delegate to service layer for decision
            decision_response = await self.fraud_decision_service.make_decision(
                transaction_id=transaction_id,
                transaction_features=self._avro_to_dict(transaction_features),
                customer_features=self._avro_to_dict(customer_features),
                network_features=self._avro_to_dict(network_features)
            )
            
            # Build response with request ID
            response_dict = self._build_response_with_request_id(
                request_id=request_id,
                decision_response=decision_response
            )
            
            # Publish response
            success = self.fraud_response_publisher.publish(
                response=response_dict,
                transaction_id=transaction_id
            )
            
            if success:
                logger.info(
                    f"Successfully handled fraud request {request_id}: "
                    f"action={decision_response.action}"
                )
            else:
                logger.error(f"Failed to publish response for request {request_id}")
            
            return success
            
        except Exception as e:
            logger.error(f"Error handling fraud request: {str(e)}")
            raise
    
    def _avro_to_dict(self, avro_record: Any) -> Dict[str, Any]:
        """
        Convert Avro record to Python dictionary.
        
        Args:
            avro_record: Avro record object or dict
        
        Returns:
            Dictionary representation
        """
        if isinstance(avro_record, dict):
            return avro_record
        
        # If it's an Avro object, convert to dict
        return dict(avro_record)
    
    def _build_response_with_request_id(
        self,
        request_id: str,
        decision_response: Any
    ) -> Dict[str, Any]:
        """
        Build Avro response with request ID.
        
        Args:
            request_id: Original request ID
            decision_response: CoordinatedDecisionResponse
        
        Returns:
            Avro-compatible response dict matching FraudAnalysisCompletedEvent.avsc
        """
        # Convert ISO timestamp string to milliseconds since epoch
        timestamp_ms = int(datetime.fromisoformat(decision_response.timestamp).timestamp() * 1000)
        
        return {
            'requestId': request_id,
            'transactionId': decision_response.transaction_id,
            'action': decision_response.action.value,  # ALLOW/BLOCK/REVIEW
            'confidence': decision_response.confidence,
            'maddpgQValue': decision_response.maddpg_q_value,
            'transactionAgentObservation': self._observation_to_avro(decision_response.transaction_agent_observation),
            'customerAgentObservation': self._observation_to_avro(decision_response.customer_agent_observation),
            'networkAgentObservation': self._observation_to_avro(decision_response.network_agent_observation),
            'agentContributions': decision_response.agent_contributions,
            'processingTimeMs': decision_response.processing_time_ms,
            'timestamp': timestamp_ms,
            'mode': decision_response.mode
        }
    
    def _observation_to_avro(self, observation: Any) -> Dict[str, Any]:
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


# Singleton instance for dependency injection
fraud_detection_request_handler = FraudDetectionRequestHandler()
