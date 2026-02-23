"""
Fraud Decision Service - Business logic for coordinated fraud analysis

This service encapsulates the core business logic for making fraud decisions.
Can be called from both REST endpoints and Kafka consumers.
"""

import asyncio
import time
from datetime import datetime
from typing import Dict, Any

import numpy as np

from ..models.schemas import (
    CoordinatedDecisionResponse,
    ActionType,
    AgentObservation
)
from .agent_orchestrator import agent_orchestrator
from .experience_buffer_service import experience_buffer_service
from app.services.reward_calculator_service import reward_calculator_service
from maddpg.core import maddpg_coordinator
from ..core.logging import logger
from ..core.config import settings
from ..core.dynamic_config import dynamic_config


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
        self.experience_buffer = experience_buffer_service
        self.reward_calculator = reward_calculator_service
    
    async def make_decision(
        self,
        payment_id: str,
        transaction_features: Dict[str, Any],
        customer_features: Dict[str, Any],
        network_features: Dict[str, Any]
    ) -> CoordinatedDecisionResponse:
        """
        Make coordinated fraud analysis decision.
        
        Workflow:
        1. Get observations from all 3 detection agents (parallel)
        2. Prepare state vector from observations
        3. MADDPG coordinator makes decision
        4. Build and return response
        
        Args:
            payment_id: Unique payment identifier
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
                payment_id=payment_id,
                decision=decision,
                observations=observations,
                processing_time=processing_time
            )
            
            # Step 6: Persist experience to replay buffer (async, non-blocking)
            # Fire-and-forget: do not await so inference latency is unaffected
            asyncio.create_task(
                self._store_experience(
                    payment_id=payment_id,
                    state=state,
                    observations=observations,
                    decision=decision,
                )
            )
            
            logger.info(
                f"Decision made for payment {payment_id}: "
                f"{response.action} (confidence: {response.confidence:.3f}, "
                f"time: {processing_time:.2f}ms)"
            )
            
            return response
            
        except Exception as e:
            logger.error(f"Error making decision for payment {payment_id}: {str(e)}")
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

        Each agent's probability and risk_score are scaled by the
        dynamically-configurable trust weight for that agent so that
        compliance officers can increase/decrease the influence of a
        specific detection model at runtime.

        Args:
            observations: Agent observations

        Returns:
            State dictionary for MADDPG
        """
        w_tx  = dynamic_config.get_float("AGENT_WEIGHT_TRANSACTION", settings.agent_weight_transaction)
        w_cx  = dynamic_config.get_float("AGENT_WEIGHT_CUSTOMER", settings.agent_weight_customer)
        w_net = dynamic_config.get_float("AGENT_WEIGHT_NETWORK", settings.agent_weight_network)

        return {
            'transaction': {
                'probability': observations['transaction'].probability * w_tx,
                'risk_score':  observations['transaction'].risk_score  * w_tx,
            },
            'customer': {
                'probability': observations['customer'].probability * w_cx,
                'risk_score':  observations['customer'].risk_score  * w_cx,
            },
            'network': {
                'probability': observations['network'].probability * w_net,
                'risk_score':  observations['network'].risk_score  * w_net,
            },
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
        payment_id: str,
        decision: Dict[str, Any],
        observations: Dict[str, AgentObservation],
        processing_time: float
    ) -> CoordinatedDecisionResponse:
        """
        Build coordinated decision response.
        
        Args:
            payment_id: Payment identifier
            decision: MADDPG decision result
            observations: Agent observations
            processing_time: Processing time in milliseconds
        
        Returns:
            CoordinatedDecisionResponse
        """
        return CoordinatedDecisionResponse(
            payment_id=payment_id,
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

    async def _store_experience(
        self,
        payment_id: str,
        state: Dict[str, Dict[str, float]],
        observations: Dict[str, AgentObservation],
        decision: Dict[str, Any],
    ) -> None:
        """
        Persist the current (s, a, r, s', done) tuple to the DB replay buffer.

        Runs as a fire-and-forget asyncio task after each decision so it
        never adds latency to the inference path.

        The MADDPG state vector is computed from `state` (the observations dict).
        next_state is set to zeros because each payment is a single-step episode.

        Args:
            payment_id:   Unique payment identifier.
            state:        Raw observations dict used to compute the state vector.
            observations: Full AgentObservation objects (for confidence/risk scores).
            decision:     MADDPG decision dict (action, confidence, q_value, contributions).
        """
        try:
            # ── Build state vector ────────────────────────────────────────────
            state_vector = self.maddpg_coordinator.state_manager.observations_to_state(state)

            # ── Joint actions from per-agent decisions ────────────────────────
            # agent_actions = {name: int} where 0=BLOCK, 1=ALLOW
            # Falls back to the joint action if somehow missing
            joint_action_int = 0 if decision["action"] == "BLOCK" else 1
            agent_actions = decision.get("agent_actions", {})
            actions_dict = {
                name: agent_actions.get(name, joint_action_int)
                for name in ["transaction", "customer", "network"]
            }

            # ── Zero next_state (single-step episode) ─────────────────────────
            next_state_vector = np.zeros_like(state_vector)

            # ── Mean risk score across agents (weighted) ─────────────────────────────
            # Weights are the same values used in _prepare_maddpg_state so the
            # stored reward signal is consistent with the state representation.
            w_tx  = dynamic_config.get_float("AGENT_WEIGHT_TRANSACTION", settings.agent_weight_transaction)
            w_cx  = dynamic_config.get_float("AGENT_WEIGHT_CUSTOMER",    settings.agent_weight_customer)
            w_net = dynamic_config.get_float("AGENT_WEIGHT_NETWORK",     settings.agent_weight_network)
            total_weight = w_tx + w_cx + w_net or 1.0  # guard against zero-sum
            mean_risk_score = (
                observations["transaction"].risk_score / 100.0 * w_tx
                + observations["customer"].risk_score  / 100.0 * w_cx
                + observations["network"].risk_score   / 100.0 * w_net
            ) / total_weight

            # ── Automated reward ──────────────────────────────────────────────
            automated_reward = self.reward_calculator.calculate_automated_reward(
                marl_action=decision["action"],
                mean_risk_score=mean_risk_score,
                confidence=decision["confidence"],
            )

            # ── Persist ───────────────────────────────────────────────────────
            await self.experience_buffer.save_experience(
                payment_id=payment_id,
                state=state_vector.tolist(),
                actions=actions_dict,
                automated_reward=automated_reward,
                next_state=next_state_vector.tolist(),
                done=True,  # Each payment is a single-step episode
                marl_action=decision["action"],
                marl_confidence=decision["confidence"],
                marl_q_value=float(decision["q_value"]),
                mean_risk_score=mean_risk_score,
            )

        except Exception as exc:
            # Never let storage errors surface to the caller
            logger.error(
                f"⚠️  Failed to store experience for payment {payment_id}: {exc}",
                exc_info=True
            )

fraud_decision_service = FraudDecisionService()
