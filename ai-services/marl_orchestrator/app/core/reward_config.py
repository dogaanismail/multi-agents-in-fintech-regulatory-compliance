"""
Configurable Reward Structure for MADDPG Offline Learning.

Implements the reward mechanism recommended by Prof. Chunyan Mu:
  - Configurable reward mapping (not hard-coded values)
  - Optional confidence-score weighting per agent
  - Separate automated (heuristic) and manual-review reward paths
  - Configurable escalation reward strategy
  - Manual review rewards carry a higher weight multiplier

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from dataclasses import dataclass, field
from typing import Dict, Optional

from app.core.config import settings
from app.core.logging import logger


# ─────────────────────────────────────────────────────────────────────────────
# Reward Config Dataclass
# ─────────────────────────────────────────────────────────────────────────────
@dataclass
class RewardConfig:
    """
    Fully configurable reward structure for MADDPG training.

    All values are loaded from environment variables (via Settings) so
    they can be tuned without code changes.

    Reward Sources
    ─────────────
    1. automated  : Heuristic reward calculated at decision time from risk scores.
                    Used as a placeholder until a compliance officer reviews the payment.
    2. manual     : Override reward set when a compliance officer approves/rejects.
                    Carries a higher weight (manual_weight_multiplier) because it
                    represents verified expert judgement.

    Escalation Modes (reward_escalation_mode)
    ─────────────────────────────────────────
    - "none"           : Escalated payments receive 0 reward.
    - "positive"       : Small positive reward for correctly identifying edge cases.
    - "final_decision" : Reward aligned with the officer's final decision outcome.
    """

    # ── Automated heuristic rewards ───────────────────────────────────────────
    auto_high_risk_block: float = field(
        default_factory=lambda: settings.reward_auto_high_risk_block
    )
    auto_low_risk_allow: float = field(
        default_factory=lambda: settings.reward_auto_low_risk_allow
    )
    auto_conflict: float = field(
        default_factory=lambda: settings.reward_auto_conflict
    )
    auto_risk_threshold: float = field(
        default_factory=lambda: settings.reward_auto_risk_threshold
    )

    # ── Manual review rewards (compliance officer decisions) ──────────────────
    manual_correct_block: float = field(
        default_factory=lambda: settings.reward_manual_correct_block
    )
    manual_correct_allow: float = field(
        default_factory=lambda: settings.reward_manual_correct_allow
    )
    manual_wrong_block: float = field(
        default_factory=lambda: settings.reward_manual_wrong_block
    )
    manual_wrong_allow: float = field(
        default_factory=lambda: settings.reward_manual_wrong_allow
    )
    manual_weight_multiplier: float = field(
        default_factory=lambda: settings.reward_manual_weight_multiplier
    )

    # ── Escalation rewards ────────────────────────────────────────────────────
    escalation_mode: str = field(
        default_factory=lambda: settings.reward_escalation_mode
    )
    escalation_positive_reward: float = field(
        default_factory=lambda: settings.reward_escalation_positive
    )

    # ── Confidence weighting ──────────────────────────────────────────────────
    use_confidence_weighting: bool = field(
        default_factory=lambda: settings.reward_use_confidence_weighting
    )

    # ── Auto-escalation threshold ─────────────────────────────────────────────
    escalation_confidence_threshold: float = field(
        default_factory=lambda: settings.escalation_confidence_threshold
    )


# ─────────────────────────────────────────────────────────────────────────────
# Reward Calculator
# ─────────────────────────────────────────────────────────────────────────────
class RewardCalculator:
    """
    Calculates scalar rewards for MADDPG training experiences.

    Decoupled from the decision path so that reward logic can evolve
    independently without touching inference code.
    """

    def __init__(self, config: Optional[RewardConfig] = None):
        self.config = config or RewardConfig()

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

        # Optionally scale by confidence
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
            marl_action:            MARL orchestrator's original decision.
            officer_decision:       "APPROVE" (legitimate) or "REJECT" (fraud).
            agent_confidence_scores: Optional dict {"transaction": 0.9, ...}

        Returns:
            Weighted scalar reward (already multiplied by manual_weight_multiplier).
        """
        # Determine base reward from action-outcome alignment
        if marl_action == "BLOCK" and officer_decision == "REJECT":
            # Agents blocked AND officer confirmed fraud → correct block
            base = self.config.manual_correct_block
        elif marl_action == "ALLOW" and officer_decision == "APPROVE":
            # Agents allowed AND officer confirmed legitimate → correct allow
            base = self.config.manual_correct_allow
        elif marl_action == "BLOCK" and officer_decision == "APPROVE":
            # Agents blocked BUT officer said legitimate → wrong block (over-blocking)
            base = self.config.manual_wrong_block
        elif marl_action == "ALLOW" and officer_decision == "REJECT":
            # Agents allowed BUT officer said fraud → dangerous miss
            base = self.config.manual_wrong_allow
        elif marl_action == "REVIEW":
            base = self._calculate_escalation_reward(officer_decision)
        else:
            base = 0.0

        # Apply confidence weighting across agents if available
        if self.config.use_confidence_weighting and agent_confidence_scores:
            scores = list(agent_confidence_scores.values())
            if scores:
                avg_confidence = sum(scores) / len(scores)
                base = base * avg_confidence

        # Manual review carries higher training weight
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
            # Reward aligned with what officer found
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


# ─────────────────────────────────────────────────────────────────────────────
# Singleton instance
# ─────────────────────────────────────────────────────────────────────────────
reward_calculator = RewardCalculator()
