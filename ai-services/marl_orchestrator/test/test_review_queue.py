"""
Tests for the compliance review queue and decision reason codes.

Author: Ismail Dogan
"""

import os
import sys
import uuid
from datetime import datetime, timedelta, timezone
from unittest.mock import AsyncMock, patch

import pytest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.infrastructure.database.models import AgentReplayBufferEntry
from app.models.schemas import AgentObservation
from app.services.fraud_decision_service import FraudDecisionService
from app.services.review_queue_service import ReviewQueueService


def create_agent_observation(is_suspicious: bool) -> AgentObservation:
    return AgentObservation(
        agent_name="transaction",
        is_suspicious=is_suspicious,
        probability=0.9 if is_suspicious else 0.1,
        risk_score=90.0 if is_suspicious else 10.0,
    )


def create_pending_review_entry(
        age_minutes: float,
        amount: float = 250.0,
) -> AgentReplayBufferEntry:
    created = datetime.now(timezone.utc) - timedelta(minutes=age_minutes)
    return AgentReplayBufferEntry(
        id=uuid.uuid4(),
        payment_id=f"payment-{uuid.uuid4()}",
        state=[0.1] * 6,
        actions={"transaction": 1, "customer": 1, "network": 1},
        next_state=[0.0] * 6,
        done=True,
        automated_reward=0.0,
        effective_reward=0.0,
        reward_source="automated",
        marl_action="REVIEW",
        marl_confidence=0.55,
        marl_q_value=0.1,
        mean_risk_score=0.4,
        amount=amount,
        currency="GBP",
        decision_reasons=[{"code": "LOW_CONFIDENCE", "detail": "0.55 < 0.60"}],
        is_used_in_training=False,
        created_at=created,
        updated_at=created,
    )


# ─────────────────────────────────────────────────────────────────────────────
# Decision reason codes
# ─────────────────────────────────────────────────────────────────────────────
@pytest.mark.unit
class TestEscalationReasons:

    def create_observations(self, suspicious_count: int):
        flags = [index < suspicious_count for index in range(3)]
        return {
            "transaction": create_agent_observation(flags[0]),
            "customer": create_agent_observation(flags[1]),
            "network": create_agent_observation(flags[2]),
        }

    def test_low_confidence_reason_carries_threshold(self):
        fraud_decision_service = FraudDecisionService()

        reasons = fraud_decision_service._build_escalation_reasons(
            {"action": "ALLOW", "confidence": 0.2, "q_value": 0.1},
            self.create_observations(suspicious_count=0),
        )

        assert [reason["code"] for reason in reasons] == ["LOW_CONFIDENCE"]
        assert "0.20" in reasons[0]["detail"]

    def test_agent_votes_reason_names_the_agents(self):
        fraud_decision_service = FraudDecisionService()

        reasons = fraud_decision_service._build_escalation_reasons(
            {"action": "ALLOW", "confidence": 0.95, "q_value": 0.1},
            self.create_observations(suspicious_count=2),
        )

        assert [reason["code"] for reason in reasons] == ["AGENT_SUSPICIOUS_VOTES"]
        assert "transaction" in reasons[0]["detail"]
        assert "customer" in reasons[0]["detail"]

    def test_both_conditions_yield_both_reasons(self):
        fraud_decision_service = FraudDecisionService()

        reasons = fraud_decision_service._build_escalation_reasons(
            {"action": "ALLOW", "confidence": 0.2, "q_value": 0.1},
            self.create_observations(suspicious_count=3),
        )

        assert {reason["code"] for reason in reasons} == {
            "LOW_CONFIDENCE", "AGENT_SUSPICIOUS_VOTES"
        }

    def test_non_allow_decision_yields_no_reasons(self):
        fraud_decision_service = FraudDecisionService()

        reasons = fraud_decision_service._build_escalation_reasons(
            {"action": "BLOCK", "confidence": 0.2, "q_value": 0.1},
            self.create_observations(suspicious_count=3),
        )

        assert reasons == []


@pytest.mark.unit
class TestAmountExtraction:

    def test_extracts_float_amount(self):
        assert FraudDecisionService._extract_amount({"amount": 1500.5}) == 1500.5

    def test_extracts_numeric_string(self):
        assert FraudDecisionService._extract_amount({"amount": "300.00"}) == 300.0

    def test_missing_or_invalid_amount_returns_none(self):
        assert FraudDecisionService._extract_amount({}) is None
        assert FraudDecisionService._extract_amount({"amount": "n/a"}) is None


# ─────────────────────────────────────────────────────────────────────────────
# Review queue assembly
# ─────────────────────────────────────────────────────────────────────────────
@pytest.mark.unit
class TestReviewQueue:

    async def test_queue_marks_sla_breaches_and_counts(self):
        review_queue_service = ReviewQueueService()
        fresh_entry = create_pending_review_entry(age_minutes=5)
        stale_entry = create_pending_review_entry(age_minutes=120)

        with patch(
                "app.services.review_queue_service.replay_buffer_repository"
        ) as repository:
            repository.find_pending_reviews = AsyncMock(
                return_value=[stale_entry, fresh_entry]
            )

            queue = await review_queue_service.get_queue()

        assert queue["pending_count"] == 2
        assert queue["sla_minutes"] == 60
        assert queue["sla_breached_count"] == 1
        stale_item = next(
            item for item in queue["items"]
            if item["payment_id"] == stale_entry.payment_id
        )
        assert stale_item["sla_breached"] is True
        assert stale_item["age_seconds"] >= 120 * 60 - 5
        assert stale_item["decision_reasons"][0]["code"] == "LOW_CONFIDENCE"

    async def test_queue_tolerates_missing_amount_and_reasons(self):
        review_queue_service = ReviewQueueService()
        legacy_entry = create_pending_review_entry(age_minutes=1, amount=250.0)
        legacy_entry.amount = None
        legacy_entry.currency = None
        legacy_entry.decision_reasons = None

        with patch(
                "app.services.review_queue_service.replay_buffer_repository"
        ) as repository:
            repository.find_pending_reviews = AsyncMock(return_value=[legacy_entry])

            queue = await review_queue_service.get_queue()

        assert queue["pending_count"] == 1
        assert queue["items"][0]["amount"] is None
        assert queue["items"][0]["decision_reasons"] == []
