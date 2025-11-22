"""
HTTP Client for Detection Agents
Communicates with Transaction, Customer, and Network Analysis agents

Author: Ismail Dogan
"""

import httpx
import asyncio
from typing import Dict, List
import time

from ..core.config import settings
from ..core.logging import logger
from ..models.schemas import (
    TransactionFeatures,
    CustomerFeatures,
    NetworkFeatures,
    AgentObservation
)

class DetectionAgentClient:
    """
    Async HTTP client for detection agents
    Makes parallel calls to all 3 agents
    """
    
    def __init__(self):
        """Initialize HTTP clients"""
        self.transaction_client = httpx.AsyncClient(
            base_url=settings.transaction_agent_url,
            timeout=settings.agent_timeout
        )
        self.customer_client = httpx.AsyncClient(
            base_url=settings.customer_agent_url,
            timeout=settings.agent_timeout
        )
        self.network_client = httpx.AsyncClient(
            base_url=settings.network_agent_url,
            timeout=settings.agent_timeout
        )
        
        logger.info("Detection agent clients initialized")
    
    async def get_all_observations(
        self,
        transaction: Dict,
        customer: Dict,
        network: Dict
    ) -> Dict[str, AgentObservation]:
        """
        Get observations from all 3 detection agents in parallel
        
        Args:
            transaction: Transaction features
            customer: Customer features  
            network: Network features
        
        Returns:
            Dict with observations from transaction, customer, network agents
        """
        # Call all agents in parallel
        start_time = time.time()
        
        tasks = [
            self._query_transaction_agent(transaction),
            self._query_customer_agent(customer),
            self._query_network_agent(network)
        ]
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        total_time = (time.time() - start_time) * 1000  # ms
        logger.info(f"All agents responded in {total_time:.2f}ms")
        
        # Parse results
        observations = {
            'transaction': results[0] if not isinstance(results[0], Exception) else self._get_fallback_observation("transaction"),
            'customer': results[1] if not isinstance(results[1], Exception) else self._get_fallback_observation("customer"),
            'network': results[2] if not isinstance(results[2], Exception) else self._get_fallback_observation("network")
        }
        
        return observations
    
    async def _query_transaction_agent(self, features: Dict) -> AgentObservation:
        """Query Transaction Pattern Agent"""
        start_time = time.time()
        
        try:
            response = await self.transaction_client.post(
                "/api/v1/predict",
                json=features
            )
            
            response.raise_for_status()
            data = response.json()
            
            response_time = (time.time() - start_time) * 1000
            
            return AgentObservation(
                agent_name="transaction",
                is_suspicious=data.get("is_suspicious", False),
                probability=data.get("fraud_probability", 0.0),
                risk_score=data.get("risk_score", 0.0),
                confidence=data.get("confidence"),
                response_time_ms=response_time
            )
        
        except Exception as e:
            logger.error(f"Transaction agent error: {str(e)}")
            raise
    
    async def _query_customer_agent(self, features: Dict) -> AgentObservation:
        """Query Customer Risk Agent"""
        start_time = time.time()
        
        try:
           
            request_data = {
                "customer_id": "ORCHESTRATOR_REQUEST",
                "features": features
            }
            
            response = await self.customer_client.post(
                "/api/v1/assess-risk", 
                json=request_data
            )
            
            response.raise_for_status()
            data = response.json()
            
            response_time = (time.time() - start_time) * 1000
            
            return AgentObservation(
                agent_name="customer",
                is_suspicious=data.get("is_high_risk", False),
                probability=data.get("risk_probability", 0.0),
                risk_score=data.get("risk_score", 0.0),
                confidence=data.get("risk_level"),
                response_time_ms=response_time
            )
        
        except Exception as e:
            logger.error(f"Customer agent error: {str(e)}")
            raise
    
    async def _query_network_agent(self, features: Dict) -> AgentObservation:
        """Query Network Analysis Agent"""
        start_time = time.time()
        
        try:
    
            request_data = {
                "account_id": "ORCHESTRATOR_REQUEST",
                "features": features
            }
            
            response = await self.network_client.post(
                "/api/v1/predict",
                json=request_data
            )
            
            response.raise_for_status()
            data = response.json()
            
            response_time = (time.time() - start_time) * 1000
            
            return AgentObservation(
                agent_name="network",
                is_suspicious=data.get("is_suspicious", False),
                probability=data.get("suspicion_probability", 0.0),  # Changed from risk_probability
                risk_score=data.get("risk_score", 0.0),
                confidence=data.get("risk_level"),  # Added confidence
                response_time_ms=response_time
            )
        
        except Exception as e:
            logger.error(f"Network agent error: {str(e)}")
            raise
    
    def _get_fallback_observation(self, agent_name: str) -> AgentObservation:
        """Return fallback observation when agent fails"""
        return AgentObservation(
            agent_name=agent_name,
            is_suspicious=False,
            probability=0.0,
            risk_score=0.0,
            confidence="LOW",
            response_time_ms=0.0
        )
    
    async def check_agents_health(self) -> Dict[str, str]:
        """Check health status of all detection agents"""
        results = {}
        
        try:
            response = await self.transaction_client.get("/api/v1/health")
            results["transaction"] = "healthy" if response.status_code == 200 else "unhealthy"
        except:
            results["transaction"] = "unreachable"
        
        try:
            response = await self.customer_client.get("/api/v1/health")
            results["customer"] = "healthy" if response.status_code == 200 else "unhealthy"
        except:
            results["customer"] = "unreachable"
        
        try:
            response = await self.network_client.get("/api/v1/health")
            results["network"] = "healthy" if response.status_code == 200 else "unhealthy"
        except:
            results["network"] = "unreachable"
        
        return results
    
    async def close(self):
        """Close all HTTP clients"""
        await self.transaction_client.aclose()
        await self.customer_client.aclose()
        await self.network_client.aclose()


# Global instance
detection_client = DetectionAgentClient()
