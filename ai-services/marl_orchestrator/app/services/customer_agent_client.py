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
    
    async def assess_risk(self, features: Dict, customer_id: str = "ORCHESTRATOR_REQUEST") -> AgentObservation:
        """
        Get risk assessment from Customer Risk Agent
        
        Args:
            features: Customer features for risk assessment
            customer_id: Customer identifier (optional)
        
        Returns:
            AgentObservation with risk assessment results
        
        Raises:
            httpx.HTTPError: If the request fails
        """
        start_time = time.time()
        
        try:
            request_data = {
                "customer_id": customer_id,
                "features": features
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
