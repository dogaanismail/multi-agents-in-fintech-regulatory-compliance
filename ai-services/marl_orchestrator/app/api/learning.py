"""
Learning Evidence API — decision-level proof that MADDPG learns from
compliance-officer feedback.

Endpoints:
  GET /learning/summary   — Buffer stats + current probe-set agreement
  GET /learning/curve     — Probe agreement per training run (learning curve)
  GET /learning/receipts  — Per-payment "the policy would now decide X" receipts

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from fastapi import APIRouter, Query

from app.models.schemas.learning_schemas import (
    LearningCurveResponse,
    LearningReceiptsResponse,
    LearningSummaryResponse,
)
from app.services.learning_evidence_service import learning_evidence_service

router = APIRouter(prefix="/learning")


@router.get(
    "/summary",
    response_model=LearningSummaryResponse,
    summary="Learning Evidence Summary",
    description=(
            "Aggregate human-in-the-loop learning metrics: replay buffer "
            "composition, and how often the current policy agrees with every "
            "compliance-officer verdict recorded so far."
    ),
)
async def get_learning_summary() -> LearningSummaryResponse:
    summary = await learning_evidence_service.get_summary()
    return LearningSummaryResponse(**summary)


@router.get(
    "/curve",
    response_model=LearningCurveResponse,
    summary="Human-in-the-Loop Learning Curve",
    description=(
            "Probe-set agreement per completed training run, oldest first. After "
            "each run, every officer-labelled state is replayed through the "
            "updated policy; a rising agreement rate demonstrates that officer "
            "feedback is being learned."
    ),
)
async def get_learning_curve(
        limit: int = Query(default=100, ge=1, le=500),
) -> LearningCurveResponse:
    points = await learning_evidence_service.get_learning_curve(limit=limit)
    return LearningCurveResponse(points=points)


@router.get(
    "/receipts",
    response_model=LearningReceiptsResponse,
    summary="Policy-Flip Receipts",
    description=(
            "For each officer-reviewed payment: the original MARL decision, the "
            "officer's verdict, and what the current policy would decide for the "
            "same state — including Q-values for both actions. Shows officers "
            "their feedback changing individual decisions."
    ),
)
async def get_learning_receipts(
        limit: int = Query(default=50, ge=1, le=200),
) -> LearningReceiptsResponse:
    receipts = await learning_evidence_service.get_receipts(limit=limit)
    return LearningReceiptsResponse(receipts=receipts)
