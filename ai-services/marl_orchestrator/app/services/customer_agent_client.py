"""
Customer Risk Agent Client
HTTP client for communication with Customer Risk Assessment Agent

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import httpx
import time
from typing import Dict

from ..core.config import settings
from ..core.logging import logger
from ..models.schemas import AgentObservation


class CustomerAgentClient:
    """
    HTTP client for Customer Risk Agent
    
    Handles communication with the customer risk assessment service
    """
    
    def __init__(self):
        """Initialize HTTP client for Customer Agent"""
        
        self.client = httpx.AsyncClient(
            base_url=settings.customer_agent_url,
            timeout=settings.agent_timeout
        )
        
        self.agent_name = "customer"
        
        logger.info(f"✅ {self.agent_name.capitalize()} Agent client initialized")
    
    def _transform_features(self, features: Dict) -> Dict:
        """
        Transform Avro camelCase features to snake_case for agent API.
        Also handles edge cases where values are 0 (provides minimum valid values).
        
        Args:
            features: Avro-formatted features with camelCase keys
            
        Returns:
            Transformed features with snake_case keys and valid values
        """
        # Map camelCase to snake_case
        transformed = {
            'transaction_count': max(features.get('transactionCount', 0), 1),  # gt=0 constraint
            'total_amount': max(features.get('totalAmount', 0.0), 0.01),  # gt=0 constraint
            'avg_amount': max(features.get('avgAmount', 0.0), 0.01),  # gt=0 constraint
            'median_amount': max(features.get('medianAmount', 0.0), 0.01),  # gt=0 constraint
            'max_amount': max(features.get('maxAmount', 0.0), 0.01),  # gt=0 constraint
            'min_amount': max(features.get('minAmount', 0.0), 0.01),  # gt=0 constraint
            'std_amount': max(features.get('stdAmount', 0.0), 0.0),  # ge=0 constraint
            'active_days': max(features.get('activeDays', 0), 1),  # gt=0 constraint
            'transactions_per_day': max(features.get('transactionsPerDay', 0.0), 0.01),  # gt=0 constraint
            'cross_border_ratio': min(max(features.get('crossBorderRatio', 0.0), 0.0), 1.0),
            'cash_transaction_ratio': min(max(features.get('cashTransactionRatio', 0.0), 0.0), 1.0),
            'amount_consistency': max(features.get('amountConsistency', 0.0), 0.0),
            'large_transaction_ratio': min(max(features.get('largeTransactionRatio', 0.0), 0.0), 1.0),
            'unique_receivers': max(features.get('uniqueReceivers', 0), 0),
            'unique_receiver_countries': max(features.get('uniqueReceiverCountries', 0), 0),
            'receiver_diversity': min(max(features.get('receiverDiversity', 0.0), 0.0), 1.0),
            'night_transaction_ratio': min(max(features.get('nightTransactionRatio', 0.0), 0.0), 1.0),
            'weekend_transaction_ratio': min(max(features.get('weekendTransactionRatio', 0.0), 0.0), 1.0),
            'unique_currencies': max(features.get('uniqueCurrencies', 0), 0),
        }
        return transformed
    
    async def assess_risk(self, features: Dict, customer_id: str = "ORCHESTRATOR_REQUEST") -> AgentObservation:
        """
        Get risk assessment from Customer Risk Agent
        
        Args:
            features: Customer features for risk assessment (Avro camelCase format)
            customer_id: Customer identifier (optional)
        
        Returns:
            AgentObservation with risk assessment results
        
        Raises:
            httpx.HTTPError: If the request fails
        """
        start_time = time.time()
        
        try:
            # Transform Avro camelCase to API snake_case
            transformed_features = self._transform_features(features)
            
            # Get customer_id from features if available
            actual_customer_id = features.get('customerId', customer_id)
            
            request_data = {
                "customer_id": actual_customer_id,
                "features": transformed_features
            }
            
            response = await self.client.post(
                "/api/v1/assess-risk",
                json=request_data
            )
            
            response.raise_for_status()
            data = response.json()
            
            response_time = (time.time() - start_time) * 1000
            
            observation = AgentObservation(
                agent_name=self.agent_name,
                is_suspicious=data.get("is_high_risk", False),
                probability=data.get("risk_probability", 0.0),
                risk_score=data.get("risk_score", 0.0),
                confidence=data.get("risk_level"),
                response_time_ms=response_time
            )
            
            logger.debug(f"{self.agent_name} agent responded in {response_time:.2f}ms")
            return observation
        
        except httpx.HTTPError as e:
            logger.error(f"{self.agent_name} agent error: {str(e)}")
            raise
    
    async def health_check(self) -> bool:
        """
        Check health status of Customer Agent
        
        Returns:
            True if agent is healthy, False otherwise
        """
        try:
            response = await self.client.get("/api/v1/health")
            return response.status_code == 200
        except Exception as e:
            logger.warning(f"{self.agent_name} agent health check failed: {str(e)}")
            return False
    
    async def close(self):
        """Close HTTP client"""
        await self.client.aclose()
        logger.info(f"{self.agent_name} agent client closed")


# Global instance
customer_agent_client = CustomerAgentClient()
