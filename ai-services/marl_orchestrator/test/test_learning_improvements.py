"""
Tests for the human-in-the-loop learning improvements:
  - counterfactual rewards and injection
  - escalation vote (no single agent can pin everything to REVIEW)
  - epsilon exploration of BLOCK decisions
  - training step calculation over small buffers
  - probe-set evaluation and receipts

Author: Ismail Dogan
"""

import os
import sys
import uuid
from datetime import datetime, timezone
from unittest.mock import AsyncMock, patch

import pytest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.core.reward_config import RewardConfig
from app.infrastructure.database.models import AgentReplayBufferEntry
from app.models.schemas import AgentObservation
from app.services.experience_buffer_service import ExperienceBufferService
from app.services.fraud_decision_service import FraudDecisionService
from app.services.reward_calculator_service import RewardCalculatorService


def create_reward_calculator() -> RewardCalculatorService:
    return RewardCalculatorService(config=RewardConfig())


def create_agent_observation(is_suspicious: bool) -> AgentObservation:
    return AgentObservation(
        agent_name="transaction",
        is_suspicious=is_suspicious,
        probability=0.9 if is_suspicious else 0.1,
        risk_score=90.0 if is_suspicious else 10.0,
    )


def create_replay_buffer_entry(marl_action: str = "BLOCK") -> AgentReplayBufferEntry:
    return AgentReplayBufferEntry(
        id=uuid.uuid4(),
        payment_id="payment-123",
        state=[0.1, 0.1, 0.2, 0.2, 0.3, 0.3],
        actions={"transaction": 0, "customer": 0, "network": 0},
        next_state=[0.0] * 6,
        done=True,
        automated_reward=-0.15,
        manual_reward=-2.7,
        effective_reward=-2.7,
        reward_source="manual_review",
        officer_decision="APPROVE",
        feedback_type="DECISION_OVERRIDE",
        marl_action=marl_action,
        marl_confidence=0.53,
        marl_q_value=0.1,
        mean_risk_score=0.16,
        is_used_in_training=False,
        created_at=datetime.now(timezone.utc),
        updated_at=datetime.now(timezone.utc),
    )


# ─────────────────────────────────────────────────────────────────────────────
# Counterfactual rewards
# ─────────────────────────────────────────────────────────────────────────────
@pytest.mark.unit
class TestCounterfactualReward:

    def test_approve_rewards_allow_with_manual_multiplier(self):
        reward_calculator = create_reward_calculator()

        reward = reward_calculator.calculate_counterfactual_reward("ALLOW")

        expected = (
                reward_calculator.config.manual_correct_allow
                * reward_calculator.config.manual_weight_multiplier
        )
        assert reward == pytest.approx(expected)
        assert reward > 0

    def test_reject_rewards_block_with_manual_multiplier(self):
        reward_calculator = create_reward_calculator()

        reward = reward_calculator.calculate_counterfactual_reward("BLOCK")

        expected = (
                reward_calculator.config.manual_correct_block
                * reward_calculator.config.manual_weight_multiplier
        )
        assert reward == pytest.approx(expected)
        assert reward > 0

    def test_unknown_action_returns_zero(self):
        reward_calculator = create_reward_calculator()

        assert reward_calculator.calculate_counterfactual_reward("REVIEW") == 0.0


# ─────────────────────────────────────────────────────────────────────────────
# Counterfactual experience injection
# ─────────────────────────────────────────────────────────────────────────────
@pytest.mark.unit
class TestCounterfactualInjection:

    async def test_block_approved_injects_allow_experience(self):
        experience_buffer_service = ExperienceBufferService()
        updated_entry = create_replay_buffer_entry(marl_action="BLOCK")

        with patch(
                "app.services.experience_buffer_service.replay_buffer_repository"
        ) as repository:
            repository.apply_manual_reward = AsyncMock(return_value=updated_entry)
            repository.save = AsyncMock(side_effect=lambda entry: entry)

            applied = await experience_buffer_service.apply_manual_feedback(
                payment_id="payment-123",
                manual_reward=-2.7,
                officer_decision="APPROVE",
                feedback_type="DECISION_OVERRIDE",
                counterfactual_reward=1.0,
                reviewed_by="officer-1",
            )

            assert applied is True
            repository.save.assert_awaited_once()
            counterfactual = repository.save.await_args.args[0]
            assert counterfactual.reward_source == "counterfactual"
            assert counterfactual.marl_action == "ALLOW"
            assert counterfactual.actions == {
                "transaction": 1, "customer": 1, "network": 1
            }
            assert counterfactual.effective_reward == pytest.approx(1.0)
            assert counterfactual.state == updated_entry.state
            assert counterfactual.is_used_in_training is False

    async def test_block_rejected_injects_nothing(self):
        experience_buffer_service = ExperienceBufferService()
        updated_entry = create_replay_buffer_entry(marl_action="BLOCK")
        updated_entry.officer_decision = "REJECT"

        with patch(
                "app.services.experience_buffer_service.replay_buffer_repository"
        ) as repository:
            repository.apply_manual_reward = AsyncMock(return_value=updated_entry)
            repository.save = AsyncMock()

            applied = await experience_buffer_service.apply_manual_feedback(
                payment_id="payment-123",
                manual_reward=6.0,
                officer_decision="REJECT",
                feedback_type="MANUAL_REVIEW",
                counterfactual_reward=2.0,
                reviewed_by="officer-1",
            )

            assert applied is True
            repository.save.assert_not_awaited()

    async def test_review_rejected_injects_block_experience(self):
        experience_buffer_service = ExperienceBufferService()
        # REVIEW is stored as joint action 1 (ALLOW); officer REJECT → BLOCK differs
        updated_entry = create_replay_buffer_entry(marl_action="REVIEW")
        updated_entry.actions = {"transaction": 1, "customer": 1, "network": 1}

        with patch(
                "app.services.experience_buffer_service.replay_buffer_repository"
        ) as repository:
            repository.apply_manual_reward = AsyncMock(return_value=updated_entry)
            repository.save = AsyncMock(side_effect=lambda entry: entry)

            await experience_buffer_service.apply_manual_feedback(
                payment_id="payment-123",
                manual_reward=0.3,
                officer_decision="REJECT",
                feedback_type="MANUAL_REVIEW",
                counterfactual_reward=2.0,
                reviewed_by="officer-1",
            )

            counterfactual = repository.save.await_args.args[0]
            assert counterfactual.marl_action == "BLOCK"
            assert counterfactual.actions == {
                "transaction": 0, "customer": 0, "network": 0
            }

    async def test_missing_entry_returns_false(self):
        experience_buffer_service = ExperienceBufferService()

        with patch(
                "app.services.experience_buffer_service.replay_buffer_repository"
        ) as repository:
            repository.apply_manual_reward = AsyncMock(return_value=None)
            repository.save = AsyncMock()

            applied = await experience_buffer_service.apply_manual_feedback(
                payment_id="missing-payment",
                manual_reward=-2.7,
                officer_decision="APPROVE",
                feedback_type="DECISION_OVERRIDE",
                counterfactual_reward=1.0,
            )

            assert applied is False
            repository.save.assert_not_awaited()


# ─────────────────────────────────────────────────────────────────────────────
# Escalation vote
# ─────────────────────────────────────────────────────────────────────────────
@pytest.mark.unit
class TestEscalationVote:

    def create_observations(self, suspicious_count: int):
        flags = [index < suspicious_count for index in range(3)]
        return {
            "transaction": create_agent_observation(flags[0]),
            "customer": create_agent_observation(flags[1]),
            "network": create_agent_observation(flags[2]),
        }

    def create_confident_allow_decision(self):
        return {"action": "ALLOW", "confidence": 0.95, "q_value": 0.5}

    def test_single_suspicious_agent_does_not_escalate(self):
        fraud_decision_service = FraudDecisionService()

        should_escalate = fraud_decision_service._should_escalate(
            self.create_confident_allow_decision(),
            self.create_observations(suspicious_count=1),
        )

        assert should_escalate is False

    def test_two_suspicious_agents_escalate(self):
        fraud_decision_service = FraudDecisionService()

        should_escalate = fraud_decision_service._should_escalate(
            self.create_confident_allow_decision(),
            self.create_observations(suspicious_count=2),
        )

        assert should_escalate is True

    def test_low_confidence_escalates_without_any_suspicious_agent(self):
        fraud_decision_service = FraudDecisionService()

        should_escalate = fraud_decision_service._should_escalate(
            {"action": "ALLOW", "confidence": 0.1, "q_value": 0.5},
            self.create_observations(suspicious_count=0),
        )

        assert should_escalate is True

    def test_block_decision_is_never_escalated(self):
        fraud_decision_service = FraudDecisionService()

        should_escalate = fraud_decision_service._should_escalate(
            {"action": "BLOCK", "confidence": 0.1, "q_value": 0.5},
            self.create_observations(suspicious_count=3),
        )

        assert should_escalate is False


# ─────────────────────────────────────────────────────────────────────────────
# Exploration
# ─────────────────────────────────────────────────────────────────────────────
@pytest.mark.unit
class TestExploration:

    def test_explores_only_block_decisions(self):
        fraud_decision_service = FraudDecisionService()

        with patch(
                "app.services.fraud_decision_service.dynamic_config.get_float",
                return_value=1.0,
        ):
            assert fraud_decision_service._should_explore(
                {"action": "BLOCK", "confidence": 0.9}
            ) is True
            assert fraud_decision_service._should_explore(
                {"action": "ALLOW", "confidence": 0.9}
            ) is False
            assert fraud_decision_service._should_explore(
                {"action": "REVIEW", "confidence": 0.9}
            ) is False

    def test_zero_epsilon_never_explores(self):
        fraud_decision_service = FraudDecisionService()

        with patch(
                "app.services.fraud_decision_service.dynamic_config.get_float",
                return_value=0.0,
        ):
            explored = [
                fraud_decision_service._should_explore(
                    {"action": "BLOCK", "confidence": 0.9}
                )
                for _ in range(50)
            ]
            assert not any(explored)


# ─────────────────────────────────────────────────────────────────────────────
# Probe-set evaluation
# ─────────────────────────────────────────────────────────────────────────────
@pytest.mark.unit
class TestProbeEvaluation:

    async def test_agreement_and_q_gap_over_probe_set(self):
        from app.services.learning_evidence_service import LearningEvidenceService

        learning_evidence_service = LearningEvidenceService()
        approved_probe = create_replay_buffer_entry(marl_action="BLOCK")
        rejected_probe = create_replay_buffer_entry(marl_action="BLOCK")
        rejected_probe.officer_decision = "REJECT"

        with patch(
                "app.services.learning_evidence_service.replay_buffer_repository"
        ) as repository, patch(
            "maddpg.core.maddpg_coordinator"
        ) as coordinator:
            repository.find_officer_labeled = AsyncMock(
                return_value=[approved_probe, rejected_probe]
            )
            # Policy now says ALLOW everywhere: agrees with APPROVE, not REJECT
            coordinator.decide_from_state.return_value = {
                "action": "ALLOW", "confidence": 0.8, "q_value": 0.4
            }
            coordinator.action_q_values.return_value = {
                "q_block": -0.3, "q_allow": 0.4
            }

            metrics = await learning_evidence_service.evaluate_probe_set()

            assert metrics["probe_count"] == 2
            assert metrics["agreement_rate"] == pytest.approx(0.5)
            # APPROVE probe gap: q_allow - q_block = 0.7; REJECT probe: -0.7
            assert metrics["avg_q_gap"] == pytest.approx(0.0)

    async def test_empty_probe_set_returns_none(self):
        from app.services.learning_evidence_service import LearningEvidenceService

        learning_evidence_service = LearningEvidenceService()

        with patch(
                "app.services.learning_evidence_service.replay_buffer_repository"
        ) as repository:
            repository.find_officer_labeled = AsyncMock(return_value=[])

            assert await learning_evidence_service.evaluate_probe_set() is None


# ─────────────────────────────────────────────────────────────────────────────
# End-to-end learning: policy must flip at inference, not just in training mode
# ─────────────────────────────────────────────────────────────────────────────
@pytest.mark.unit
@pytest.mark.slow
class TestPolicyLearnsFromRewards:

    def test_policy_flips_to_rewarded_action_at_inference(self):
        """
        Regression test for the BatchNorm train/eval inconsistency: gradient
        updates run on batches (BatchNorm active) while decisions run on single
        states, so learning must transfer across that boundary.  Trains on
        experiences where ALLOW is rewarded and BLOCK penalised, then asserts
        the single-state inference path actually decides ALLOW.
        """
        import numpy as np
        from maddpg.core.coordinator import MADDPGCoordinator

        coordinator = MADDPGCoordinator(
            state_dim=6, action_dim=2, num_agents=3,
            hidden_dim=64, learning_rate=0.01, buffer_size=1000,
        )

        rng = np.random.default_rng(42)
        for _ in range(256):
            state = rng.uniform(0.0, 0.4, size=6).astype(np.float32)
            allow = rng.random() < 0.5
            action_index = 1 if allow else 0
            one_hot = np.zeros(2, dtype=np.float32)
            one_hot[action_index] = 1.0
            coordinator.replay_buffer.push(
                state=state,
                actions=[one_hot.copy() for _ in range(3)],
                reward=1.0 if allow else -1.0,
                next_state=np.zeros(6, dtype=np.float32),
                done=True,
            )

        for _ in range(150):
            losses = coordinator.update(batch_size=64)
            assert losses is not None

        observations = {
            "transaction": {"probability": 0.1, "risk_score": 10.0},
            "customer": {"probability": 0.2, "risk_score": 20.0},
            "network": {"probability": 0.15, "risk_score": 15.0},
        }
        decision = coordinator.decide(observations)

        assert decision["action"] == "ALLOW"
        assert decision["confidence"] > 0.6

        state = coordinator.state_manager.observations_to_state(observations)
        q_values = coordinator.action_q_values(state)
        assert q_values["q_allow"] > q_values["q_block"]
