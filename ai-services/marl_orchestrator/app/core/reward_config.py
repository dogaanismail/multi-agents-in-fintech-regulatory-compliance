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
from app.core.dynamic_config import dynamic_config
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
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_AUTO_HIGH_RISK_BLOCK", settings.reward_auto_high_risk_block
        )
    )
    auto_low_risk_allow: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_AUTO_LOW_RISK_ALLOW", settings.reward_auto_low_risk_allow
        )
    )
    auto_conflict: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_AUTO_CONFLICT", settings.reward_auto_conflict
        )
    )
    auto_risk_threshold: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_AUTO_RISK_THRESHOLD", settings.reward_auto_risk_threshold
        )
    )

    # ── Manual review rewards (compliance officer decisions) ──────────────────
    manual_correct_block: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_MANUAL_CORRECT_BLOCK", settings.reward_manual_correct_block
        )
    )
    manual_correct_allow: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_MANUAL_CORRECT_ALLOW", settings.reward_manual_correct_allow
        )
    )
    manual_wrong_block: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_MANUAL_WRONG_BLOCK", settings.reward_manual_wrong_block
        )
    )
    manual_wrong_allow: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_MANUAL_WRONG_ALLOW", settings.reward_manual_wrong_allow
        )
    )
    manual_weight_multiplier: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_MANUAL_WEIGHT_MULTIPLIER", settings.reward_manual_weight_multiplier
        )
    )

    # ── Override rewards (terminal decision reversed by compliance officer) ───
    override_block_to_approve: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_OVERRIDE_BLOCK_TO_APPROVE", settings.reward_override_block_to_approve
        )
    )
    override_allow_to_reject: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_OVERRIDE_ALLOW_TO_REJECT", settings.reward_override_allow_to_reject
        )
    )
    override_multiplier: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_OVERRIDE_MULTIPLIER", settings.reward_override_multiplier
        )
    )

    # ── Escalation rewards ────────────────────────────────────────────────────
    escalation_mode: str = field(
        default_factory=lambda: dynamic_config.get_str(
            "REWARD_ESCALATION_MODE", settings.reward_escalation_mode
        )
    )
    escalation_positive_reward: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "REWARD_ESCALATION_POSITIVE", settings.reward_escalation_positive
        )
    )

    # ── Confidence weighting ──────────────────────────────────────────────────
    use_confidence_weighting: bool = field(
        default_factory=lambda: dynamic_config.get_bool(
            "REWARD_USE_CONFIDENCE_WEIGHTING", settings.reward_use_confidence_weighting
        )
    )

    # ── Auto-escalation threshold ─────────────────────────────────────────────
    escalation_confidence_threshold: float = field(
        default_factory=lambda: dynamic_config.get_float(
            "ESCALATION_CONFIDENCE_THRESHOLD", settings.escalation_confidence_threshold
        )
    )


