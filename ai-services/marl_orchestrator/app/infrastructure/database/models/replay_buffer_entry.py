"""
ORM model for the agent_replay_buffer table.

Stores one (s, a, r, s', done) experience tuple per payment decision.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import uuid
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import Boolean, DateTime, Float, String
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.infrastructure.database.database import Base


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


class AgentReplayBufferEntry(Base):
    """
    Persisted experience tuple for offline MADDPG training.

    State vector (6 floats):
        [txn_prob, txn_score/100, cust_prob, cust_score/100, net_prob, net_score/100]

    Actions (JSONB):
        {"transaction": 0|1, "customer": 0|1, "network": 0|1}  — 0=BLOCK, 1=ALLOW

    Reward lifecycle:
        - automated_reward  : heuristic set at decision time
        - manual_reward     : override set by compliance officer (Issue #54)
        - effective_reward  : whichever is active — used in training
    """

    __tablename__ = "agent_replay_buffer"

    # ── Primary key ───────────────────────────────────────────────────────────
    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )

    # ── Payment reference ─────────────────────────────────────────────────────
    payment_id: Mapped[str] = mapped_column(String(255), nullable=False, index=True)

    # ── MADDPG transition tuple (s, a, r, s', done) ───────────────────────────
    state: Mapped[dict] = mapped_column(JSONB, nullable=False)
    actions: Mapped[dict] = mapped_column(JSONB, nullable=False)
    next_state: Mapped[dict] = mapped_column(JSONB, nullable=False)
    done: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)

    # ── Rewards ───────────────────────────────────────────────────────────────
    automated_reward: Mapped[float] = mapped_column(Float, nullable=False)
    manual_reward: Mapped[Optional[float]] = mapped_column(Float, nullable=True, default=None)
    effective_reward: Mapped[float] = mapped_column(Float, nullable=False)
    reward_source: Mapped[str] = mapped_column(
        String(50), nullable=False, default="automated",
        comment="automated | manual_review"
    )

    # ── Decision metadata ─────────────────────────────────────────────────────
    marl_action: Mapped[str] = mapped_column(String(20), nullable=False)
    marl_confidence: Mapped[float] = mapped_column(Float, nullable=False)
    marl_q_value: Mapped[float] = mapped_column(Float, nullable=False)
    mean_risk_score: Mapped[float] = mapped_column(Float, nullable=False)

    # ── Training lifecycle ────────────────────────────────────────────────────
    is_used_in_training: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=False, index=True
    )
    training_run_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        UUID(as_uuid=True), nullable=True, default=None
    )

    # ── Timestamps ────────────────────────────────────────────────────────────
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow, onupdate=_utcnow
    )

    def __repr__(self) -> str:
        return (
            f"<AgentReplayBufferEntry id={self.id} payment={self.payment_id} "
            f"action={self.marl_action} reward={self.effective_reward:.3f} "
            f"source={self.reward_source}>"
        )
