"""
Fraud Decision Service - Business logic for coordinated fraud analysis

This service encapsulates the core business logic for making fraud decisions.
Can be called from both REST endpoints and Kafka consumers.
"""

import asyncio
import random
import time
from datetime import datetime
from typing import Any, Dict, Optional

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

            # Structured reason codes: every decision carries a machine-readable
            # explanation of how it was reached, persisted with the experience so
            # compliance officers see *why* a payment landed in their queue.
            decision_reasons = [{
                "code": f"POLICY_{decision['action']}",
                "detail": f"MADDPG policy chose {decision['action']} "
                          f"with confidence {decision['confidence']:.2f}",
            }]

            # Step 3b: Post-decision escalation override
            # MADDPG only outputs ALLOW or BLOCK; this rule catches the case where
            # MADDPG says ALLOW but the model is uncertain (low confidence) or
            # enough specialist agents flagged the payment as suspicious.
            # Those payments are routed to human review instead of being auto-approved.
            escalation_reasons = self._build_escalation_reasons(decision, observations)
            if escalation_reasons:
                logger.warning(
                    f"Escalation override for payment {payment_id}: ALLOW → REVIEW "
                    f"(reasons={[reason['code'] for reason in escalation_reasons]})"
                )
                decision['action'] = 'REVIEW'
                decision_reasons.extend(escalation_reasons)

            # Step 3c: Exploration — route a small fraction of BLOCK decisions
            # to human review instead.  The officer's verdict flows back as
            # manual feedback, giving the replay buffer the labelled ALLOW/BLOCK
            # evidence a deterministic always-block policy can never generate.
            if self._should_explore(decision):
                logger.warning(
                    f"Exploration override for payment {payment_id}: "
                    f"BLOCK → REVIEW (epsilon-driven, officer adjudicates)"
                )
                decision['action'] = 'REVIEW'
                decision_reasons.append({
                    "code": "EXPLORATION",
                    "detail": "BLOCK routed to human review by the exploration "
                              "policy so an officer verdict can teach the model",
                })

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
                    decision_reasons=decision_reasons,
                    amount=self._extract_amount(transaction_features),
                    currency=transaction_features.get("paymentCurrency"),
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
    
    def _should_escalate(
        self,
        decision: Dict[str, Any],
        observations: Dict[str, AgentObservation]
    ) -> bool:
        """
        Determine whether a post-MADDPG escalation override is warranted.

        MADDPG only ever emits ALLOW or BLOCK.  This method catches the gap:
        payments where the network says ALLOW but either the model is uncertain
        (confidence below threshold) or enough specialist agents flagged the
        payment as suspicious — a conflicted signal that warrants human eyes.

        The suspicious-agent condition is a configurable vote
        (ESCALATION_SUSPICIOUS_VOTES, default 2 of 3) rather than any single
        agent: one over-flagging specialist model must not be able to pin every
        payment to REVIEW and starve the policy of ALLOW outcomes to learn from.

        Both conditions are OR-ed so that either one alone is enough to trigger
        escalation, and both are independently observable in the logs so
        compliance officers can understand the reason.

        The threshold and vote count are read from the dynamic config service
        first so compliance officers can adjust them at runtime without a
        deploy; the pydantic settings values act as in-process fallbacks.

        Args:
            decision:     MADDPG decision dict (action, confidence, …).
            observations: Agent observations dict keyed by agent name.

        Returns:
            True if the ALLOW decision should be upgraded to REVIEW.
        """
        return bool(self._build_escalation_reasons(decision, observations))

    def _build_escalation_reasons(
            self,
            decision: Dict[str, Any],
            observations: Dict[str, AgentObservation]
    ) -> list:
        """
        Evaluate the escalation conditions and return one structured reason per
        triggered condition (empty list = no escalation).  The reasons are
        persisted with the experience and shown in the officer's review queue.
        """
        if decision['action'] != 'ALLOW':
            return []  # Only ALLOW decisions can be escalated

        threshold = dynamic_config.get_float(
            "ESCALATION_CONFIDENCE_THRESHOLD",
            settings.escalation_confidence_threshold
        )
        votes_required = max(1, dynamic_config.get_int(
            "ESCALATION_SUSPICIOUS_VOTES",
            settings.escalation_suspicious_votes
        ))

        reasons = []

        # Condition 1: MADDPG is uncertain — confidence is below the threshold
        if decision['confidence'] < threshold:
            reasons.append({
                "code": "LOW_CONFIDENCE",
                "detail": f"Policy confidence {decision['confidence']:.2f} is below "
                          f"the escalation threshold {threshold:.2f}",
            })

        # Condition 2: Conflicted signal — enough specialist agents flagged
        suspicious_agents = [
            name for name, obs in observations.items() if obs.is_suspicious
        ]
        if len(suspicious_agents) >= votes_required:
            reasons.append({
                "code": "AGENT_SUSPICIOUS_VOTES",
                "detail": f"{len(suspicious_agents)} of {len(observations)} specialist "
                          f"agents flagged suspicious ({', '.join(suspicious_agents)}); "
                          f"{votes_required} votes trigger review",
            })

        return reasons

    @staticmethod
    def _extract_amount(transaction_features: Dict[str, Any]) -> Any:
        amount = transaction_features.get("amount")
        try:
            return float(amount) if amount is not None else None
        except (TypeError, ValueError):
            return None

    def _should_explore(self, decision: Dict[str, Any]) -> bool:
        """
        Decide whether this BLOCK decision is sacrificed to exploration.

        With probability EXPLORATION_EPSILON a BLOCK is downgraded to REVIEW so
        a compliance officer adjudicates it.  Exploration never auto-ALLOWs a
        payment the policy wanted to block — the human stays in the loop — but
        it breaks the feedback deadlock where an always-block policy only ever
        generates BLOCK experiences and can never discover that ALLOW pays.
        """
        if decision['action'] != 'BLOCK':
            return False

        epsilon = dynamic_config.get_float(
            "EXPLORATION_EPSILON", settings.exploration_epsilon
        )
        return epsilon > 0 and random.random() < epsilon

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
            decision_reasons: Optional[list] = None,
            amount: Optional[float] = None,
            currency: Optional[str] = None,
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
            # MADDPG only knows BLOCK(0) / ALLOW(1); REVIEW is a post-decision
            # business override of ALLOW, so map it to 1 for the replay buffer.
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
                decision_reasons=decision_reasons,
                amount=amount,
                currency=currency,
            )

        except Exception as exc:
            # Never let storage errors surface to the caller
            logger.error(
                f"⚠️  Failed to store experience for payment {payment_id}: {exc}",
                exc_info=True
            )

fraud_decision_service = FraudDecisionService()
