"""
ORM model for the agent_training_runs table.

Audit log of every offline batch training cycle.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import uuid
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import Boolean, DateTime, Float, Integer, String, Text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.infrastructure.database.database import Base


def _utcnow() -> datetime:
    return datetime.now(timezone.utc)


class AgentTrainingRun(Base):
    """
    Audit log of every offline batch training cycle.

    Tracks losses, number of experiences consumed, and whether
    updated model weights were saved to disk.
    """

    __tablename__ = "agent_training_runs"

    # ── Primary key ───────────────────────────────────────────────────────────
    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )

    # ── Status ────────────────────────────────────────────────────────────────
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="running",
        comment="running | completed | failed"
    )

    # ── Batch metadata ────────────────────────────────────────────────────────
    experiences_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    train_steps_completed: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    batch_size: Mapped[int] = mapped_column(Integer, nullable=False)

    # ── Losses ────────────────────────────────────────────────────────────────
    critic_loss: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    actor_transaction_loss: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    actor_customer_loss: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    actor_network_loss: Mapped[Optional[float]] = mapped_column(Float, nullable=True)

    # ── Model persistence ─────────────────────────────────────────────────────
    model_saved: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)

    # ── Error info ────────────────────────────────────────────────────────────
    error_message: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # ── Timestamps ────────────────────────────────────────────────────────────
    started_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow
    )
    completed_at: Mapped[Optional[datetime]] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    def __repr__(self) -> str:
        return (
            f"<AgentTrainingRun id={self.id} status={self.status} "
            f"experiences={self.experiences_count} steps={self.train_steps_completed}>"
        )
