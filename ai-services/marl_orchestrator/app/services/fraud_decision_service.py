"""
Fraud Decision Service - Business logic for coordinated fraud detection

This service encapsulates the core business logic for making fraud decisions.
Can be called from both REST endpoints and Kafka consumers.
"""

import time
from datetime import datetime
from typing import Dict, Any

from ..models.schemas import (
    CoordinatedDecisionResponse,
    ActionType,
    AgentObservation
)
from .agent_orchestrator import agent_orchestrator
from maddpg.core import maddpg_coordinator
from ..core.logging import logger
from ..core.config import settings


class FraudDecisionService:
    """
    Service for making coordinated fraud decisions using MADDPG.
    
    Responsibilities:
    - Orchestrate agent observations
    - Invoke MADDPG coordinator
    - Build decision response
    - Handle business logic and error handling
    """
    
    def __init__(self):
        self.agent_orchestrator = agent_orchestrator
        self.maddpg_coordinator = maddpg_coordinator
    
    async def make_decision(
        self,
        transaction_id: str,
        transaction_features: Dict[str, Any],
        customer_features: Dict[str, Any],
        network_features: Dict[str, Any]
    ) -> CoordinatedDecisionResponse:
        """
        Make coordinated fraud detection decision.
        
        Workflow:
        1. Get observations from all 3 detection agents (parallel)
        2. Prepare state vector from observations
        3. MADDPG coordinator makes decision
        4. Build and return response
        
        Args:
            transaction_id: Unique transaction identifier
            transaction_features: Transaction feature dict
            customer_features: Customer feature dict
            network_features: Network feature dict
        
        Returns:
            CoordinatedDecisionResponse with action, confidence, and details
            
        Raises:
            Exception: If decision making fails
        """
        start_time = time.time()
        
        try:
            # Step 1: Get observations from all 3 detection agents (parallel)
            observations = await self._get_agent_observations(
                transaction_features,
                customer_features,
                network_features
            )
            
            # Step 2: Prepare state for MADDPG
            state = self._prepare_maddpg_state(observations)
            
            # Step 3: MADDPG makes coordinated decision
            decision = self._make_maddpg_decision(state)
            
            # Step 4: Calculate processing time
            processing_time = (time.time() - start_time) * 1000  # ms
            
            # Step 5: Build response
            response = self._build_decision_response(
                transaction_id=transaction_id,
                decision=decision,
                observations=observations,
                processing_time=processing_time
            )
            
            logger.info(
                f"Decision made for transaction {transaction_id}: "
                f"{response.action} (confidence: {response.confidence:.3f}, "
                f"time: {processing_time:.2f}ms)"
            )
            
            return response
            
        except Exception as e:
            logger.error(f"Error making decision for transaction {transaction_id}: {str(e)}")
            raise
    
    async def _get_agent_observations(
        self,
        transaction_features: Dict[str, Any],
        customer_features: Dict[str, Any],
        network_features: Dict[str, Any]
    ) -> Dict[str, AgentObservation]:
        """
        Query all detection agents in parallel.
        
        Args:
            transaction_features: Transaction features
            customer_features: Customer features
            network_features: Network features
        
        Returns:
            Dictionary with observations from all 3 agents
        """
        observations = await self.agent_orchestrator.get_all_observations(
            transaction_features=transaction_features,
            customer_features=customer_features,
            network_features=network_features
        )
        
        logger.info("Received observations from all agents")
        logger.info(f"  Transaction: prob={observations['transaction'].probability:.3f}, "
                   f"risk={observations['transaction'].risk_score:.3f}")
        logger.info(f"  Customer: prob={observations['customer'].probability:.3f}, "
                   f"risk={observations['customer'].risk_score:.3f}")
        logger.info(f"  Network: prob={observations['network'].probability:.3f}, "
                   f"risk={observations['network'].risk_score:.3f}")
        
        return observations
    
    def _prepare_maddpg_state(
        self,
        observations: Dict[str, AgentObservation]
    ) -> Dict[str, Dict[str, float]]:
        """
        Convert agent observations to MADDPG state representation.
        
        Args:
            observations: Agent observations
        
        Returns:
            State dictionary for MADDPG
        """
        return {
            'transaction': {
                'probability': observations['transaction'].probability,
                'risk_score': observations['transaction'].risk_score
            },
            'customer': {
                'probability': observations['customer'].probability,
                'risk_score': observations['customer'].risk_score
            },
            'network': {
                'probability': observations['network'].probability,
                'risk_score': observations['network'].risk_score
            }
        }
    
    def _make_maddpg_decision(
        self,
        state: Dict[str, Dict[str, float]]
    ) -> Dict[str, Any]:
        """
        Invoke MADDPG coordinator to make decision.
        
        Args:
            state: Prepared state vector
        
        Returns:
            Decision dictionary with action, confidence, q_value, contributions
        """
        decision = self.maddpg_coordinator.decide(state)
        
        logger.info(
            f"MADDPG Decision: {decision['action']} "
            f"(confidence: {decision['confidence']:.3f}, "
            f"q_value: {decision['q_value']:.4f})"
        )
        
        return decision
    
    def _build_decision_response(
        self,
        transaction_id: str,
        decision: Dict[str, Any],
        observations: Dict[str, AgentObservation],
        processing_time: float
    ) -> CoordinatedDecisionResponse:
        """
        Build coordinated decision response.
        
        Args:
            transaction_id: Transaction identifier
            decision: MADDPG decision result
            observations: Agent observations
            processing_time: Processing time in milliseconds
        
        Returns:
            CoordinatedDecisionResponse
        """
        return CoordinatedDecisionResponse(
            transaction_id=transaction_id,
            action=ActionType(decision['action']),
            confidence=decision['confidence'],
            maddpg_q_value=decision['q_value'],
            transaction_agent_observation=observations['transaction'],
            customer_agent_observation=observations['customer'],
            network_agent_observation=observations['network'],
            agent_contributions=decision['contributions'],
            processing_time_ms=processing_time,
            timestamp=datetime.now().isoformat(),
            mode=settings.maddpg_mode
        )


# Singleton instance
fraud_decision_service = FraudDecisionService()
