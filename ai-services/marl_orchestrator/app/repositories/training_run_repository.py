"""
Training Run Repository — all database operations for agent_training_runs.

Responsibility: pure DB CRUD for the training audit log, nothing else.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import uuid
from typing import List

from sqlalchemy import desc, select, update as sa_update

from app.infrastructure.database.database import AsyncSessionLocal
from app.infrastructure.database.models import AgentTrainingRun


class TrainingRunRepository:
    """
    CRUD layer for the agent_training_runs table.

    Methods are intentionally minimal: create a run record, update it,
    and fetch recent history for the API.
    """

    async def create(self, run: AgentTrainingRun) -> AgentTrainingRun:
        """Persist a new training run record (status='running')."""
        async with AsyncSessionLocal() as session:
            session.add(run)
            await session.commit()
            await session.refresh(run)
        return run

    async def update(self, run_id: uuid.UUID, **kwargs) -> None:
        """Update arbitrary fields on an existing training run record."""
        async with AsyncSessionLocal() as session:
            await session.execute(
                sa_update(AgentTrainingRun)
                .where(AgentTrainingRun.id == run_id)
                .values(**kwargs)
            )
            await session.commit()

    async def get_recent(self, limit: int = 20) -> List[AgentTrainingRun]:
        """Return the most recent training run records, newest first."""
        async with AsyncSessionLocal() as session:
            result = await session.execute(
                select(AgentTrainingRun)
                .order_by(desc(AgentTrainingRun.started_at))
                .limit(limit)
            )
            return list(result.scalars().all())


# Singleton
training_run_repository = TrainingRunRepository()
