"""
Reward Calculator Service

Stateful singleton that holds a RewardConfig and exposes all reward-calculation
logic for MADDPG training.  Decoupled from RewardConfig (data) and from the
inference path so reward logic can evolve independently.

The service keeps its config fresh by calling refresh() at the start of every
training cycle, pulling the latest values from the dynamic_config cache
(which itself is backed by configuration-service).

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from typing import Dict, Optional

from app.core.dynamic_config import dynamic_config
from app.core.logging import logger
from app.core.reward_config import RewardConfig


class RewardCalculatorService:
    """
    Calculates scalar rewards for MADDPG training experiences.

    Decoupled from the decision path so that reward logic can evolve
    independently without touching inference code.

    Refresh cycle
    ─────────────
    Call refresh() after dynamic_config.refresh_if_stale() at the top of
    each training cycle.  This rebuilds self.config from the latest cache
    values so compliance officers' reward-weight changes take effect within
    one polling interval (≤ 5 min) without a container restart.
    """

    def __init__(self, config: Optional[RewardConfig] = None):
        self.config = config or RewardConfig()

    # ──────────────────────────────────────────────────────────────────────────
    # Config hot-reload
    # ──────────────────────────────────────────────────────────────────────────

    def refresh(self) -> None:
        """
        Recreate RewardConfig from the latest dynamic_config values.

        Call this after ``await dynamic_config.refresh()`` to ensure the
        reward calculator uses the most recent weights set by compliance officers.
        """
        self.config = RewardConfig()
        logger.info(
            "🔄 RewardCalculatorService config refreshed from dynamic_config "
            f"(auto_high_risk_block={self.config.auto_high_risk_block}, "
            f"manual_correct_block={self.config.manual_correct_block})"
        )

    # ──────────────────────────────────────────────────────────────────────────
    # Automated reward (called at decision time)
    # ──────────────────────────────────────────────────────────────────────────

    def calculate_automated_reward(
        self,
        marl_action: str,
        mean_risk_score: float,
        confidence: float,
    ) -> float:
        """
        Heuristic reward based on action / risk-score alignment.

        Logic:
          - BLOCK + high risk  → positive reward  (action matches signal)
          - ALLOW + low risk   → positive reward  (action matches signal)
          - BLOCK + low risk   → conflict penalty (over-blocking)
          - ALLOW + high risk  → conflict penalty (dangerous miss)

        If confidence weighting is enabled, the base reward is scaled by the
        model's confidence so that uncertain decisions receive smaller rewards.

        Args:
            marl_action:     "BLOCK" | "ALLOW" | "REVIEW"
            mean_risk_score: Average of the three agent risk scores (0-1)
            confidence:      Overall MARL confidence (0-1)

        Returns:
            Scalar reward value.
        """
        threshold = self.config.auto_risk_threshold

        if marl_action == "BLOCK":
            base = (
                self.config.auto_high_risk_block
                if mean_risk_score >= threshold
                else self.config.auto_conflict
            )
        elif marl_action == "ALLOW":
            base = (
                self.config.auto_low_risk_allow
                if mean_risk_score < threshold
                else self.config.auto_conflict
            )
        else:
            # REVIEW / ESCALATE — reward is determined later by manual feedback
            base = 0.0

        if self.config.use_confidence_weighting:
            base = base * confidence

        logger.debug(
            f"Automated reward: action={marl_action}, risk={mean_risk_score:.3f}, "
            f"confidence={confidence:.3f} → reward={base:.4f}"
        )
        return float(base)

    # ──────────────────────────────────────────────────────────────────────────
    # Manual review reward (called when compliance officer acts)
    # ──────────────────────────────────────────────────────────────────────────

    def calculate_manual_reward(
        self,
        marl_action: str,
        officer_decision: str,
        agent_confidence_scores: Optional[Dict[str, float]] = None,
    ) -> float:
        """
        Reward (or penalty) based on the compliance officer's decision.

        The base reward is multiplied by `manual_weight_multiplier` so that
        verified expert judgement has a stronger training signal than the
        automated heuristic.

        If confidence weighting is enabled and `agent_confidence_scores` is
        provided, the reward is the weighted average across agents using their
        individual confidence scores as weights.

        Args:
            marl_action:             MARL orchestrator's original decision.
            officer_decision:        "APPROVE" (legitimate) or "REJECT" (fraud).
            agent_confidence_scores: Optional dict {"transaction": 0.9, ...}

        Returns:
            Weighted scalar reward (already multiplied by manual_weight_multiplier).
        """
        if marl_action == "BLOCK" and officer_decision == "REJECT":
            base = self.config.manual_correct_block
        elif marl_action == "ALLOW" and officer_decision == "APPROVE":
            base = self.config.manual_correct_allow
        elif marl_action == "BLOCK" and officer_decision == "APPROVE":
            base = self.config.manual_wrong_block
        elif marl_action == "ALLOW" and officer_decision == "REJECT":
            base = self.config.manual_wrong_allow
        elif marl_action == "REVIEW":
            base = self._calculate_escalation_reward(officer_decision)
        else:
            base = 0.0

        if self.config.use_confidence_weighting and agent_confidence_scores:
            scores = list(agent_confidence_scores.values())
            if scores:
                avg_confidence = sum(scores) / len(scores)
                base = base * avg_confidence

        weighted_reward = base * self.config.manual_weight_multiplier

        logger.info(
            f"Manual reward: marl_action={marl_action}, officer={officer_decision}, "
            f"base={base:.3f}, multiplier={self.config.manual_weight_multiplier} "
            f"→ weighted_reward={weighted_reward:.4f}"
        )
        return float(weighted_reward)

    def _calculate_escalation_reward(self, officer_decision: str) -> float:
        """
        Reward for REVIEW/ESCALATE decisions based on configured escalation mode.

        Modes:
          - "none"           : always 0
          - "positive"       : always a small positive reward
          - "final_decision" : positive if officer found fraud, small positive if legitimate
        """
        mode = self.config.escalation_mode

        if mode == "none":
            return 0.0
        elif mode == "positive":
            return self.config.escalation_positive_reward
        elif mode == "final_decision":
            if officer_decision == "REJECT":
                return self.config.escalation_positive_reward
            else:
                return self.config.escalation_positive_reward * 0.5
        else:
            logger.warning(f"Unknown escalation_mode='{mode}', defaulting to 0")
            return 0.0

    def should_auto_escalate(self, confidence: float) -> bool:
        """
        Return True if the MARL decision confidence is below the configured
        threshold, meaning it should be escalated to manual review.

        Args:
            confidence: Overall MARL confidence score (0-1)

        Returns:
            True if escalation is recommended.
        """
        return confidence < self.config.escalation_confidence_threshold

    # ──────────────────────────────────────────────────────────────────────────
    # Override reward (called when compliance officer reverses a terminal decision)
    # ──────────────────────────────────────────────────────────────────────────

    def calculate_override_reward(
        self,
        original_marl_action: str,
        officer_decision: str,
        agent_confidence_scores: Optional[Dict[str, float]] = None,
    ) -> float:
        """
        Penalty applied when a compliance officer overrides a terminal decision.

        Override scenarios:
          - BLOCK → APPROVE  : agent over-blocked a legitimate payment  (override_block_to_approve)
          - ALLOW → REJECT   : agent missed fraud on a completed payment (override_allow_to_reject)

        The base penalty is multiplied by ``override_multiplier`` (default 3x) to
        emphasise that reversing a terminal decision is a stronger signal than
        a routine manual review correction.

        If confidence weighting is enabled, the penalty is further scaled by the
        average agent confidence so that high-confidence wrong decisions receive
        the strongest penalty.

        Args:
            original_marl_action:    MARL decision that led to the terminal state ("BLOCK" | "ALLOW").
            officer_decision:        Compliance officer final call ("APPROVE" | "REJECT").
            agent_confidence_scores: Optional dict {"transaction": 0.9, ...}

        Returns:
            Weighted scalar penalty (already multiplied by override_multiplier).
        """
        if original_marl_action == "BLOCK" and officer_decision == "APPROVE":
            base = self.config.override_block_to_approve
        elif original_marl_action == "ALLOW" and officer_decision == "REJECT":
            base = self.config.override_allow_to_reject
        else:
            base = 0.0

        if self.config.use_confidence_weighting and agent_confidence_scores:
            scores = list(agent_confidence_scores.values())
            if scores:
                avg_confidence = sum(scores) / len(scores)
                base = base * avg_confidence

        weighted_penalty = base * self.config.override_multiplier

        logger.info(
            f"Override reward: marl_action={original_marl_action}, officer={officer_decision}, "
            f"base={base:.3f}, multiplier={self.config.override_multiplier} "
            f"→ weighted_penalty={weighted_penalty:.4f}"
        )
        return float(weighted_penalty)


# ─────────────────────────────────────────────────────────────────────────────
# Singleton
# ─────────────────────────────────────────────────────────────────────────────
reward_calculator_service = RewardCalculatorService()
