"""
Review Queue API schemas.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from typing import Dict, List, Optional

from pydantic import BaseModel


class DecisionReason(BaseModel):
    """One structured reason code recorded at decision time."""
    code: str
    detail: str


class ReviewQueueItem(BaseModel):
    """One payment awaiting a compliance officer's verdict."""
    payment_id: str
    decided_at: str
    age_seconds: int
    sla_breached: bool
    amount: Optional[float] = None
    currency: Optional[str] = None
    mean_risk_score: float
    marl_confidence: float
    decision_reasons: List[DecisionReason]


class ReviewQueueResponse(BaseModel):
    """The pending-review worklist, ordered by exposure (risk x amount)."""
    pending_count: int
    sla_minutes: int
    sla_breached_count: int
    items: List[ReviewQueueItem]
