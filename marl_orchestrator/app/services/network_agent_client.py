"""
Network Analysis Agent Client
HTTP client for communication with Network Analysis Agent

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import httpx
import time
from typing import Dict

from ..core.config import settings
from ..core.logging import logger
from ..models.schemas import AgentObservation


class NetworkAgentClient:
    """
    HTTP client for Network Analysis Agent
    
    Handles communication with the network analysis service
    """
    
    def __init__(self):
        """Initialize HTTP client for Network Agent"""
        self.client = httpx.AsyncClient(
            base_url=settings.network_agent_url,
            timeout=settings.agent_timeout
        )
        self.agent_name = "network"
        logger.info(f"✅ {self.agent_name.capitalize()} Agent client initialized")
    
    async def analyze(self, features: Dict, account_id: str = "ORCHESTRATOR_REQUEST") -> AgentObservation:
        """
        Get network analysis from Network Analysis Agent
        
        Args:
            features: Network features for analysis
            account_id: Account identifier (optional)
        
        Returns:
            AgentObservation with analysis results
        
        Raises:
            httpx.HTTPError: If the request fails
        """
        start_time = time.time()
        
        try:
            request_data = {
                "account_id": account_id,
                "features": features
            }
            
            response = await self.client.post(
                "/api/v1/predict",
                json=request_data
            )
            
            response.raise_for_status()
            data = response.json()
            
            response_time = (time.time() - start_time) * 1000
            
            observation = AgentObservation(
                agent_name=self.agent_name,
                is_suspicious=data.get("is_suspicious", False),
                probability=data.get("suspicion_probability", 0.0),
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
        Check health status of Network Agent
        
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
network_agent_client = NetworkAgentClient()
