"""
Learning Evidence API schemas.

Decision-level proof that MADDPG learns from compliance-officer feedback:
learning curve points, per-payment receipts, and rolling summaries.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from typing import Dict, List, Optional

from pydantic import BaseModel


class LearningCurvePoint(BaseModel):
    """Probe-set metrics for one completed training run."""
    training_run_id: str
    completed_at: Optional[str] = None
    experiences_count: int
    train_steps_completed: int
    critic_loss: Optional[float] = None
    probe_agreement_rate: Optional[float] = None
    probe_count: Optional[int] = None
    probe_avg_q_gap: Optional[float] = None


class LearningReceipt(BaseModel):
    """
    One officer-reviewed payment, re-evaluated by the current policy.

    `policy_flipped` is True when the policy now decides differently from the
    original decision; `agrees_with_officer` is True when the current decision
    matches the officer's verdict.
    """
    payment_id: str
    decided_at: str
    reviewed_at: str
    original_action: str
    original_confidence: float
    officer_decision: str
    feedback_type: Optional[str] = None
    correct_action: str
    current_action: str
    current_confidence: float
    q_block: float
    q_allow: float
    policy_flipped: bool
    agrees_with_officer: bool


class ProbeEvaluation(BaseModel):
    """Current-policy agreement over all officer-labelled states."""
    agreement_rate: float
    probe_count: int
    avg_q_gap: float


class LearningSummaryResponse(BaseModel):
    """Aggregate learning metrics for the compliance dashboard."""
    buffer: Dict
    current_probe_evaluation: Optional[ProbeEvaluation] = None
    completed_training_runs: int
    total_gradient_steps: int


class LearningCurveResponse(BaseModel):
    """Learning curve across training runs, oldest first."""
    points: List[LearningCurvePoint]


class LearningReceiptsResponse(BaseModel):
    """Receipts for officer-reviewed payments, newest first."""
    receipts: List[LearningReceipt]
