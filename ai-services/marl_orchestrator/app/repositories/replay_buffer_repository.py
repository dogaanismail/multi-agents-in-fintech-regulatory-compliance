"""
Replay Buffer Repository — all database operations for agent_replay_buffer.

Responsibility: pure DB CRUD, nothing else.
No business logic. No numpy. No reward calculation.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import uuid
from datetime import datetime, timezone
from typing import List, Optional

from sqlalchemy import func, select, update

from app.core.logging import logger
from app.infrastructure.database.database import AsyncSessionLocal
from app.infrastructure.database.models import AgentReplayBufferEntry


class ReplayBufferRepository:
    """
    CRUD layer for the agent_replay_buffer table.

    All methods open and close their own session so they are safe to
    call from both the HTTP request path and background scheduler tasks.
    """

    async def save(self, entry: AgentReplayBufferEntry) -> AgentReplayBufferEntry:
        """Persist a new experience entry and return the saved object."""
        async with AsyncSessionLocal() as session:
            session.add(entry)
            await session.commit()
            await session.refresh(entry)
        return entry

    async def count_unused(self) -> int:
        """Count experiences not yet consumed by a training run."""
        async with AsyncSessionLocal() as session:
            result = await session.execute(
                select(func.count(AgentReplayBufferEntry.id)).where(
                    AgentReplayBufferEntry.is_used_in_training == False  # noqa: E712
                )
            )
            return result.scalar_one()

    async def count_all(self) -> int:
        """Count all stored experiences."""
        async with AsyncSessionLocal() as session:
            result = await session.execute(
                select(func.count(AgentReplayBufferEntry.id))
            )
            return result.scalar_one()

    async def sample_batch(
        self,
        batch_size: int,
        include_used: bool = False,
    ) -> List[AgentReplayBufferEntry]:
        """
        Sample a random mini-batch using PostgreSQL's ORDER BY random().

        Args:
            batch_size:   Maximum number of entries to return.
            include_used: If False (default), only return unused entries.
        """
        async with AsyncSessionLocal() as session:
            query = select(AgentReplayBufferEntry)
            if not include_used:
                query = query.where(
                    AgentReplayBufferEntry.is_used_in_training == False  # noqa: E712
                )
            query = query.order_by(func.random()).limit(batch_size)
            result = await session.execute(query)
            return list(result.scalars().all())

    async def mark_as_used(
        self,
        entry_ids: List[uuid.UUID],
        training_run_id: uuid.UUID,
    ) -> None:
        """Bulk-mark entries as consumed by a training run."""
        if not entry_ids:
            return
        async with AsyncSessionLocal() as session:
            await session.execute(
                update(AgentReplayBufferEntry)
                .where(AgentReplayBufferEntry.id.in_(entry_ids))
                .values(
                    is_used_in_training=True,
                    training_run_id=training_run_id,
                    updated_at=datetime.now(timezone.utc),
                )
            )
            await session.commit()
        logger.debug(
            f"✅ Marked {len(entry_ids)} experiences as used (run={training_run_id})"
        )

    async def apply_manual_reward(
        self,
        payment_id: str,
        manual_reward: float,
        reviewed_by: Optional[str] = None,
    ) -> bool:
        """
        Override the reward for a payment with a compliance officer's decision.

        Resets is_used_in_training=False so the entry is re-sampled in the
        next training cycle with the corrected reward.

        Returns True if the entry was found and updated, False otherwise.
        """
        async with AsyncSessionLocal() as session:
            result = await session.execute(
                select(AgentReplayBufferEntry)
                .where(AgentReplayBufferEntry.payment_id == payment_id)
                .order_by(AgentReplayBufferEntry.created_at.desc())
                .limit(1)
            )
            entry = result.scalar_one_or_none()

            if entry is None:
                logger.warning(f"⚠️  No replay buffer entry for payment_id={payment_id}")
                return False

            entry.manual_reward = manual_reward
            entry.effective_reward = manual_reward
            entry.reward_source = "manual_review"
            entry.is_used_in_training = False
            entry.training_run_id = None
            entry.updated_at = datetime.now(timezone.utc)
            await session.commit()

        logger.info(
            f"🧑‍⚖️  Manual reward applied: payment={payment_id} "
            f"reward={manual_reward:.4f} reviewed_by={reviewed_by or 'unknown'}"
        )
        return True


# Singleton
replay_buffer_repository = ReplayBufferRepository()
