"""
Agent Orchestrator Service
Coordinates parallel calls to multiple detection agents

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import asyncio
import time
from typing import Dict

from ..core.logging import logger
from ..models.schemas import AgentObservation
from .transaction_agent_client import transaction_agent_client
from .customer_agent_client import customer_agent_client
from .network_agent_client import network_agent_client

class AgentOrchestrator:
    """
    Orchestrates parallel calls to multiple detection agents
    
    Uses individual agent clients for flexible, maintainable service integration
    """
    
    def __init__(self):
        """Initialize orchestrator with individual agent clients"""
        self.transaction_client = transaction_agent_client
        self.customer_client = customer_agent_client
        self.network_client = network_agent_client
        logger.info("🎯 Agent Orchestrator initialized")
    
    async def get_all_observations(
        self,
        transaction_features: Dict,
        customer_features: Dict,
        network_features: Dict
    ) -> Dict[str, AgentObservation]:
        """
        Get observations from all detection agents in parallel
        
        Args:
            transaction_features: Features for transaction pattern analysis
            customer_features: Features for customer risk assessment
            network_features: Features for network analysis
        
        Returns:
            Dict mapping agent names to their observations
            
        Example:
            {
                'transaction': AgentObservation(...),
                'customer': AgentObservation(...),
                'network': AgentObservation(...)
            }
        """
        start_time = time.time()
        
        # Call all agents in parallel
        tasks = [
            self.transaction_client.predict(transaction_features),
            self.customer_client.assess_risk(customer_features),
            self.network_client.analyze(network_features)
        ]
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        total_time = (time.time() - start_time) * 1000  # ms
        logger.info(f"✅ All agents responded in {total_time:.2f}ms")
        
        # Parse results with fallback for failures
        observations = {
            'transaction': results[0] if not isinstance(results[0], Exception) 
                          else self._get_fallback_observation("transaction", results[0]),
            'customer': results[1] if not isinstance(results[1], Exception) 
                       else self._get_fallback_observation("customer", results[1]),
            'network': results[2] if not isinstance(results[2], Exception) 
                      else self._get_fallback_observation("network", results[2])
        }
        
        return observations
        
    async def check_all_agents_health(self) -> Dict[str, str]:
        """
        Check health status of all detection agents
        
        Returns:
            Dict mapping agent names to health status (healthy/unhealthy/unreachable)
        """
        tasks = [
            self.transaction_client.health_check(),
            self.customer_client.health_check(),
            self.network_client.health_check()
        ]
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        health_status = {
            "transaction": self._parse_health_status(results[0]),
            "customer": self._parse_health_status(results[1]),
            "network": self._parse_health_status(results[2])
        }
        
        return health_status
    
    def _parse_health_status(self, result) -> str:
        """Parse health check result into status string"""
        if isinstance(result, Exception):
            return "unreachable"
        return "healthy" if result else "unhealthy"
    
    def _get_fallback_observation(self, agent_name: str, error: Exception = None) -> AgentObservation:
        """
        Return fallback observation when agent fails
        
        Args:
            agent_name: Name of the failed agent
            error: The exception that occurred
        
        Returns:
            Safe fallback observation with minimal risk
        """
        if error:
            logger.warning(f"Using fallback observation for {agent_name}: {str(error)}")
        
        return AgentObservation(
            agent_name=agent_name,
            is_suspicious=False,
            probability=0.0,
            risk_score=0.0,
            confidence="UNKNOWN",
            response_time_ms=0.0
        )
    
    async def close(self):
        """Close all agent clients"""
        await asyncio.gather(
            self.transaction_client.close(),
            self.customer_client.close(),
            self.network_client.close()
        )
        logger.info("🔒 Agent Orchestrator closed")


# Global instance
agent_orchestrator = AgentOrchestrator()
