"""
Training schemas — Pydantic models for the Training Management API.

Covers: offline training status, manual trigger, training run history,
and replay buffer statistics.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from typing import Dict, List, Optional

from pydantic import BaseModel


class TrainingStatusResponse(BaseModel):
    """Current state of the offline MADDPG training scheduler."""
    scheduler_running: bool
    is_training: bool
    training_interval_seconds: int
    min_experiences_required: int
    unused_experiences: int
    total_experiences: int
    last_training_run_id: Optional[str] = None
    last_training_at: Optional[str] = None
    total_training_runs: int
    total_experiences_trained: int


class TriggerTrainingResponse(BaseModel):
    """Response to a manual training trigger request."""
    triggered: bool
    reason: Optional[str] = None
    available_experiences: Optional[int] = None
    batch_size: Optional[int] = None


class TrainingRunResponse(BaseModel):
    """Single training run record from the audit log."""
    id: str
    status: str
    experiences_count: int
    train_steps_completed: int
    batch_size: int
    critic_loss: Optional[float] = None
    actor_transaction_loss: Optional[float] = None
    actor_customer_loss: Optional[float] = None
    actor_network_loss: Optional[float] = None
    model_saved: bool
    error_message: Optional[str] = None
    started_at: str
    completed_at: Optional[str] = None


class BufferStatsResponse(BaseModel):
    """Current replay buffer statistics."""
    total_experiences: int
    unused_experiences: int
    used_experiences: int


class ExperienceEntryResponse(BaseModel):
    """Single experience tuple from the agent replay buffer."""
    id: str
    payment_id: str
    marl_action: str
    marl_confidence: float
    marl_q_value: float
    mean_risk_score: float
    automated_reward: float
    manual_reward: Optional[float] = None
    effective_reward: float
    reward_source: str
    is_used_in_training: bool
    training_run_id: Optional[str] = None
    created_at: str
    updated_at: str


class ReplayBufferAggStats(BaseModel):
    """Aggregate analytics over the full replay buffer."""
    total_experiences: int
    manual_review_count: int
    automated_count: int
    used_in_training_count: int
    avg_effective_reward: Optional[float] = None
    avg_confidence: Optional[float] = None
    avg_risk_score: Optional[float] = None
    action_counts: Dict[str, int] = {}
