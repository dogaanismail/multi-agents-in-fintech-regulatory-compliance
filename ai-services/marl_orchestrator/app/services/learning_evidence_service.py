"""
Learning Evidence Service — decision-level proof that MADDPG learns from
compliance officers.

Training losses alone cannot show a compliance officer that their feedback
changes the model's behaviour.  This service produces evidence at the decision
level instead:

  - Probe-set evaluation: every officer-labelled payment keeps its stored state
    vector; replaying those states through the *current* policy yields an
    agreement rate — the human-in-the-loop learning curve.
  - Receipts: per overridden payment, what the policy would decide *now*
    (with Q-values for both actions), so officers see their feedback flip
    individual decisions.
  - Summary: rolling operational metrics (action distribution, override rate,
    average manual reward).

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import asyncio
from typing import Dict, List, Optional

from app.core.logging import logger
from app.repositories.replay_buffer_repository import replay_buffer_repository
from app.repositories.training_run_repository import training_run_repository


def _correct_action(officer_decision: str) -> str:
    return "ALLOW" if officer_decision == "APPROVE" else "BLOCK"


class LearningEvidenceService:
    """Computes probe-set metrics, receipts, and learning summaries."""

    # ──────────────────────────────────────────────────────────────────────────
    # Probe-set evaluation (called after every training run)
    # ──────────────────────────────────────────────────────────────────────────
    async def evaluate_probe_set(self) -> Optional[Dict]:
        """
        Replay all officer-labelled states through the current policy.

        Returns:
            {"agreement_rate": float, "probe_count": int, "avg_q_gap": float}
            or None when no officer-labelled probes exist yet.

        avg_q_gap is the mean of Q(correct action) - Q(other action): positive
        and growing means the critic increasingly prefers what officers chose.
        """
        probes = await replay_buffer_repository.find_officer_labeled()
        if not probes:
            return None

        def _evaluate() -> Dict:
            from maddpg.core import maddpg_coordinator

            agreements = 0
            q_gaps: List[float] = []
            for probe in probes:
                decision = maddpg_coordinator.decide_from_state(probe.state)
                q_values = maddpg_coordinator.action_q_values(probe.state)

                correct = _correct_action(probe.officer_decision)
                if decision["action"] == correct:
                    agreements += 1

                if correct == "ALLOW":
                    q_gaps.append(q_values["q_allow"] - q_values["q_block"])
                else:
                    q_gaps.append(q_values["q_block"] - q_values["q_allow"])

            return {
                "agreement_rate": agreements / len(probes),
                "probe_count": len(probes),
                "avg_q_gap": sum(q_gaps) / len(q_gaps),
            }

        loop = asyncio.get_event_loop()
        metrics = await loop.run_in_executor(None, _evaluate)
        logger.info(
            f"🎓 Probe-set evaluation: agreement={metrics['agreement_rate']:.1%} "
            f"over {metrics['probe_count']} officer-labelled states, "
            f"avg_q_gap={metrics['avg_q_gap']:.4f}"
        )
        return metrics

    # ──────────────────────────────────────────────────────────────────────────
    # Learning curve (probe metrics across training runs)
    # ──────────────────────────────────────────────────────────────────────────
    async def get_learning_curve(self, limit: int = 100) -> List[Dict]:
        """
        Return probe metrics per completed training run, oldest first,
        so the frontend can chart agreement over time.
        """
        runs = await training_run_repository.get_recent(limit=limit)
        curve = [
            {
                "training_run_id": str(run.id),
                "completed_at": run.completed_at.isoformat() if run.completed_at else None,
                "experiences_count": run.experiences_count,
                "train_steps_completed": run.train_steps_completed,
                "critic_loss": run.critic_loss,
                "probe_agreement_rate": run.probe_agreement_rate,
                "probe_count": run.probe_count,
                "probe_avg_q_gap": run.probe_avg_q_gap,
            }
            for run in runs
            if run.status == "completed" and run.probe_agreement_rate is not None
        ]
        curve.reverse()  # get_recent returns newest first
        return curve

    # ──────────────────────────────────────────────────────────────────────────
    # Receipts (per-payment policy flips)
    # ──────────────────────────────────────────────────────────────────────────
    async def get_receipts(self, limit: int = 50) -> List[Dict]:
        """
        For each officer-reviewed payment: the original decision, the officer's
        verdict, and what the current policy would decide for the same state.
        """
        probes = await replay_buffer_repository.find_officer_labeled(limit=limit)
        if not probes:
            return []

        def _evaluate() -> List[Dict]:
            from maddpg.core import maddpg_coordinator

            receipts = []
            for probe in probes:
                decision = maddpg_coordinator.decide_from_state(probe.state)
                q_values = maddpg_coordinator.action_q_values(probe.state)
                correct = _correct_action(probe.officer_decision)

                receipts.append({
                    "payment_id": probe.payment_id,
                    "decided_at": probe.created_at.isoformat(),
                    "reviewed_at": probe.updated_at.isoformat(),
                    "original_action": probe.marl_action,
                    "original_confidence": probe.marl_confidence,
                    "officer_decision": probe.officer_decision,
                    "feedback_type": probe.feedback_type,
                    "correct_action": correct,
                    "current_action": decision["action"],
                    "current_confidence": decision["confidence"],
                    "q_block": q_values["q_block"],
                    "q_allow": q_values["q_allow"],
                    "policy_flipped": decision["action"] != probe.marl_action,
                    "agrees_with_officer": decision["action"] == correct,
                })
            return receipts

        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(None, _evaluate)

    # ──────────────────────────────────────────────────────────────────────────
    # Summary (rolling operational metrics)
    # ──────────────────────────────────────────────────────────────────────────
    async def get_summary(self) -> Dict:
        """Aggregate learning metrics for the compliance dashboard."""
        stats = await replay_buffer_repository.get_aggregate_stats()
        probe_metrics = await self.evaluate_probe_set()

        runs = await training_run_repository.get_recent(limit=1000)
        completed_runs = [r for r in runs if r.status == "completed"]

        return {
            "buffer": stats,
            "current_probe_evaluation": probe_metrics,
            "completed_training_runs": len(completed_runs),
            "total_gradient_steps": sum(r.train_steps_completed for r in completed_runs),
        }


# ─────────────────────────────────────────────────────────────────────────────
# Singleton
# ─────────────────────────────────────────────────────────────────────────────
learning_evidence_service = LearningEvidenceService()
