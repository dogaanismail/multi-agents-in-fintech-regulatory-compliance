"""
Review Queue API — the compliance officer's pending-review worklist.

Endpoints:
  GET /review-queue — Pending REVIEW decisions with reason codes, ordered by
                      exposure (risk x amount), with SLA ageing.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from fastapi import APIRouter, Query

from app.models.schemas.review_queue_schemas import ReviewQueueResponse
from app.services.review_queue_service import review_queue_service

router = APIRouter(prefix="/review-queue")


@router.get(
    "",
    response_model=ReviewQueueResponse,
    summary="Pending Review Worklist",
    description=(
            "Payments the MARL orchestrator escalated to human review that no "
            "compliance officer has decided yet. Ordered by exposure "
            "(risk score x payment amount) so the riskiest, largest payments "
            "surface first; each item carries the structured reason codes recorded "
            "at decision time and an SLA-breach flag based on REVIEW_SLA_MINUTES."
    ),
)
async def get_review_queue(
        limit: int = Query(default=200, ge=1, le=500),
) -> ReviewQueueResponse:
    queue = await review_queue_service.get_queue(limit=limit)
    return ReviewQueueResponse(**queue)
