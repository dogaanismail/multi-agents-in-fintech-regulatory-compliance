"""
Review Queue Service — the compliance officer's worklist.

Builds the queue of payments awaiting a human verdict: REVIEW decisions with
no officer feedback yet, enriched with the structured reason codes recorded at
decision time, exposure (risk x amount) ordering, and SLA ageing.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from datetime import datetime, timezone
from typing import Dict, List

from app.core.config import settings
from app.core.dynamic_config import dynamic_config
from app.repositories.replay_buffer_repository import replay_buffer_repository


class ReviewQueueService:
    """Assembles the pending-review worklist for the backoffice."""

    async def get_queue(self, limit: int = 200) -> Dict:
        """
        Return pending reviews ordered by exposure, each with SLA state.

        A payment breaches the SLA once it has waited longer than
        REVIEW_SLA_MINUTES (dynamic config, officer-tunable at runtime).
        """
        sla_minutes = dynamic_config.get_int(
            "REVIEW_SLA_MINUTES", settings.review_sla_minutes
        )
        entries = await replay_buffer_repository.find_pending_reviews(limit=limit)
        now = datetime.now(timezone.utc)

        items: List[Dict] = []
        for entry in entries:
            age_seconds = max(0, int((now - entry.created_at).total_seconds()))
            items.append({
                "payment_id": entry.payment_id,
                "decided_at": entry.created_at.isoformat(),
                "age_seconds": age_seconds,
                "sla_breached": age_seconds > sla_minutes * 60,
                "amount": entry.amount,
                "currency": entry.currency,
                "mean_risk_score": entry.mean_risk_score,
                "marl_confidence": entry.marl_confidence,
                "decision_reasons": entry.decision_reasons or [],
            })

        return {
            "pending_count": len(items),
            "sla_minutes": sla_minutes,
            "sla_breached_count": sum(1 for item in items if item["sla_breached"]),
            "items": items,
        }


# ─────────────────────────────────────────────────────────────────────────────
# Singleton
# ─────────────────────────────────────────────────────────────────────────────
review_queue_service = ReviewQueueService()
