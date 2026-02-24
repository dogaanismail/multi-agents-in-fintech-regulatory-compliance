"""
Training Management API — monitoring and control for offline MADDPG training.

Endpoints:
  GET  /training/status   — Current scheduler and buffer status
  POST /training/trigger  — Manually trigger a training cycle
  GET  /training/history  — Last N completed training runs

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import uuid
from typing import List, Optional

from fastapi import APIRouter, HTTPException, Query

from app.core.logging import logger
from app.models.schemas.training_schemas import (
    BufferStatsResponse,
    ExperienceEntryResponse,
    ReplayBufferAggStats,
    TrainingRunResponse,
    TrainingStatusResponse,
    TriggerTrainingResponse,
)
from app.repositories.replay_buffer_repository import replay_buffer_repository
from app.repositories.training_run_repository import training_run_repository
from app.services.experience_buffer_service import experience_buffer_service
from app.services.offline_trainer_service import offline_trainer_service

router = APIRouter(prefix="/training")


# ─────────────────────────────────────────────────────────────────────────────
# GET /training/status
# ─────────────────────────────────────────────────────────────────────────────
@router.get(
    "/status",
    response_model=TrainingStatusResponse,
    summary="Offline Training Status",
    description=(
        "Returns the current state of the offline MADDPG training scheduler, "
        "including how many new experiences are waiting in the replay buffer "
        "and statistics from the last completed training run."
    ),
)
async def get_training_status() -> TrainingStatusResponse:
    """Return offline trainer and replay buffer status."""
    status = await offline_trainer_service.get_status()
    return TrainingStatusResponse(**status)


# ─────────────────────────────────────────────────────────────────────────────
# POST /training/trigger
# ─────────────────────────────────────────────────────────────────────────────
@router.post(
    "/trigger",
    response_model=TriggerTrainingResponse,
    summary="Manually Trigger Training",
    description=(
        "Immediately schedules a training cycle outside the regular interval. "
        "Returns instantly; the training itself runs in a background task. "
        "Returns 'triggered: false' if training is already in progress or "
        "there are not enough experiences."
    ),
)
async def trigger_training() -> TriggerTrainingResponse:
    """Manually trigger a training cycle."""
    result = await offline_trainer_service.trigger_now()
    logger.info(f"Manual training trigger: {result}")
    return TriggerTrainingResponse(**result)


# ─────────────────────────────────────────────────────────────────────────────
# GET /training/history
# ─────────────────────────────────────────────────────────────────────────────
@router.get(
    "/history",
    response_model=List[TrainingRunResponse],
    summary="Training Run History",
    description=(
        "Returns the last N training run records ordered by start time descending. "
        "Each record includes loss values, number of experiences used, and whether "
        "the updated model was saved to disk."
    ),
)
async def get_training_history(
    limit: int = Query(default=20, ge=1, le=100, description="Number of runs to return"),
) -> List[TrainingRunResponse]:
    """Return recent training run audit records."""
    runs = await training_run_repository.get_recent(limit=limit)
    return [
        TrainingRunResponse(
            id=str(run.id),
            status=run.status,
            experiences_count=run.experiences_count,
            train_steps_completed=run.train_steps_completed,
            batch_size=run.batch_size,
            critic_loss=run.critic_loss,
            actor_transaction_loss=run.actor_transaction_loss,
            actor_customer_loss=run.actor_customer_loss,
            actor_network_loss=run.actor_network_loss,
            model_saved=run.model_saved,
            error_message=run.error_message,
            started_at=run.started_at.isoformat(),
            completed_at=run.completed_at.isoformat() if run.completed_at else None,
        )
        for run in runs
    ]


# ─────────────────────────────────────────────────────────────────────────────
# GET /training/buffer
# ─────────────────────────────────────────────────────────────────────────────
@router.get(
    "/buffer",
    response_model=BufferStatsResponse,
    summary="Replay Buffer Stats",
    description="Returns current replay buffer statistics.",
)
async def get_buffer_stats() -> BufferStatsResponse:
    """Return replay buffer entry counts."""
    unused = await experience_buffer_service.count_unused_experiences()
    total = await experience_buffer_service.count_all_experiences()
    return BufferStatsResponse(
        total_experiences=total,
        unused_experiences=unused,
        used_experiences=total - unused,
    )


# ─────────────────────────────────────────────────────────────────────────────
# GET /training/experiences
# ─────────────────────────────────────────────────────────────────────────────
@router.get(
    "/experiences",
    response_model=List[ExperienceEntryResponse],
    summary="Replay Buffer Experience Log",
    description=(
        "Returns the most recent experience tuples stored in the agent replay "
        "buffer, newest first. Each row represents one payment decision with "
        "its associated reward signal and training status."
    ),
)
async def list_experiences(
    limit: int = Query(default=50, ge=1, le=200, description="Max entries to return"),
    offset: int = Query(default=0, ge=0, description="Pagination offset"),
) -> List[ExperienceEntryResponse]:
    """Return paginated experience entries from the replay buffer."""
    entries = await replay_buffer_repository.list_recent(limit=limit, offset=offset)
    return [
        ExperienceEntryResponse(
            id=str(e.id),
            payment_id=e.payment_id,
            marl_action=e.marl_action,
            marl_confidence=e.marl_confidence,
            marl_q_value=e.marl_q_value,
            mean_risk_score=e.mean_risk_score,
            automated_reward=e.automated_reward,
            manual_reward=e.manual_reward,
            effective_reward=e.effective_reward,
            reward_source=e.reward_source,
            is_used_in_training=e.is_used_in_training,
            training_run_id=str(e.training_run_id) if e.training_run_id else None,
            created_at=e.created_at.isoformat(),
            updated_at=e.updated_at.isoformat(),
        )
        for e in entries
    ]


# ─────────────────────────────────────────────────────────────────────────────
# GET /training/experiences/stats
# ─────────────────────────────────────────────────────────────────────────────
@router.get(
    "/experiences/stats",
    response_model=ReplayBufferAggStats,
    summary="Replay Buffer Aggregate Statistics",
    description=(
        "Returns aggregate analytics over the full replay buffer: action "
        "distribution, reward averages, manual vs automated split, and "
        "training coverage percentage."
    ),
)
async def get_experience_stats() -> ReplayBufferAggStats:
    """Return aggregate analytics over the replay buffer."""
    stats = await replay_buffer_repository.get_aggregate_stats()
    return ReplayBufferAggStats(**stats)
