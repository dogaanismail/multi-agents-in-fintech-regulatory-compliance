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
    
    def _transform_features(self, features: Dict) -> Dict:
        """
        Transform Avro camelCase features to snake_case for agent API.
        Also handles edge cases where values need to be validated.
        
        Args:
            features: Avro-formatted features with camelCase keys
            
        Returns:
            Transformed features with snake_case keys and valid values
        """
        # Map camelCase to snake_case
        transformed = {
            'in_degree': max(features.get('inDegree', 0), 0),
            'out_degree': max(features.get('outDegree', 0), 0),
            'degree_centrality': min(max(features.get('degreeCentrality', 0.0), 0.0), 1.0),
            'in_degree_centrality': min(max(features.get('inDegreeCentrality', 0.0), 0.0), 1.0),
            'out_degree_centrality': min(max(features.get('outDegreeCentrality', 0.0), 0.0), 1.0),
            'betweenness_centrality': min(max(features.get('betweennessCentrality', 0.0), 0.0), 1.0),
            'closeness_centrality': min(max(features.get('closenessCentrality', 0.0), 0.0), 1.0),
            'pagerank': max(features.get('pagerank', 0.0), 0.0),
            'eigenvector_centrality': max(features.get('eigenvectorCentrality', 0.0), 0.0),
            'clustering_coefficient': min(max(features.get('clusteringCoefficient', 0.0), 0.0), 1.0),
            'community': max(features.get('community', 0), 0),
        }
        return transformed
    
    async def analyze(self, features: Dict, account_id: str = "ORCHESTRATOR_REQUEST") -> AgentObservation:
        """
        Get network analysis from Network Analysis Agent
        
        Args:
            features: Network features for analysis (Avro camelCase format)
            account_id: Account identifier (optional)
        
        Returns:
            AgentObservation with analysis results
        
        Raises:
            httpx.HTTPError: If the request fails
        """
        start_time = time.time()
        
        try:
            # Transform Avro camelCase to API snake_case
            transformed_features = self._transform_features(features)
            
            # Get account_id from features if available
            actual_account_id = features.get('accountId', account_id)
            
            request_data = {
                "account_id": actual_account_id,
                "features": transformed_features
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
