"""
Experience Buffer Service — coordinates replay buffer management.

Responsibilities:
  - Build a new AgentReplayBufferEntry from raw decision data and persist it
    via ReplayBufferRepository.
  - Provide buffer statistics for the scheduler and the API.
  - Convert DB entries to numpy arrays for the in-memory ReplayBuffer
    (needed by MADDPGTrainerService).
  - Forward manual-review reward overrides to the repository (Issue #54).

All database access is delegated to ReplayBufferRepository.
No SQL here, no scheduling, no ML.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import uuid
from datetime import datetime, timezone
from typing import List, Optional

import numpy as np

from app.core.logging import logger
from app.infrastructure.database.models import AgentReplayBufferEntry
from app.repositories.replay_buffer_repository import replay_buffer_repository


class ExperienceBufferService:
    """
    Service layer for replay buffer management.

    Builds experience entries from raw decision data, delegates persistence
    to ReplayBufferRepository, and provides numpy conversion for training.
    """

    # ──────────────────────────────────────────────────────────────────────────
    # Write path: save experience after each MARL decision
    # ──────────────────────────────────────────────────────────────────────────
    async def save_experience(
        self,
        payment_id: str,
        state: List[float],
        actions: dict,
        automated_reward: float,
        next_state: List[float],
        done: bool,
        marl_action: str,
        marl_confidence: float,
        marl_q_value: float,
        mean_risk_score: float,
    ) -> AgentReplayBufferEntry:
        """Build and persist one (s, a, r, s', done) experience tuple."""
        entry = AgentReplayBufferEntry(
            id=uuid.uuid4(),
            payment_id=payment_id,
            state=state,
            actions=actions,
            next_state=next_state,
            done=done,
            automated_reward=automated_reward,
            manual_reward=None,
            effective_reward=automated_reward,
            reward_source="automated",
            marl_action=marl_action,
            marl_confidence=marl_confidence,
            marl_q_value=marl_q_value,
            mean_risk_score=mean_risk_score,
            is_used_in_training=False,
            created_at=datetime.now(timezone.utc),
            updated_at=datetime.now(timezone.utc),
        )
        saved = await replay_buffer_repository.save(entry)
        logger.info(
            f"📦 Experience saved: payment={payment_id} action={marl_action} "
            f"reward={automated_reward:.4f}"
        )
        return saved

    # ──────────────────────────────────────────────────────────────────────────
    # Read path: buffer statistics
    # ──────────────────────────────────────────────────────────────────────────
    async def count_unused_experiences(self) -> int:
        """Return the number of experiences not yet consumed by a training run."""
        return await replay_buffer_repository.count_unused()

    async def count_all_experiences(self) -> int:
        """Return the total number of stored experiences."""
        return await replay_buffer_repository.count_all()

    async def sample_batch(
        self,
        batch_size: int,
        include_used: bool = False,
    ) -> List[AgentReplayBufferEntry]:
        """Sample a random mini-batch (PostgreSQL ORDER BY random())."""
        return await replay_buffer_repository.sample_batch(batch_size, include_used)

    async def sample_training_batch(
            self,
            batch_size: int,
            top_up_with_used: bool = True,
    ) -> List[AgentReplayBufferEntry]:
        """Sample unused entries, topped up with recently-used ones when short."""
        return await replay_buffer_repository.sample_training_batch(
            batch_size, top_up_with_used
        )

    # ──────────────────────────────────────────────────────────────────────────
    # Write path: mark as used after training
    # ──────────────────────────────────────────────────────────────────────────
    async def mark_as_used(
        self, entry_ids: List[uuid.UUID], training_run_id: uuid.UUID
    ) -> None:
        """Mark a list of experiences as consumed by a training run."""
        await replay_buffer_repository.mark_as_used(entry_ids, training_run_id)

    # ──────────────────────────────────────────────────────────────────────────
    # Write path: manual reward override (Issue #54 hook)
    # ──────────────────────────────────────────────────────────────────────────
    async def apply_manual_feedback(
        self,
        payment_id: str,
        manual_reward: float,
            officer_decision: str,
            feedback_type: str,
            counterfactual_reward: float,
        reviewed_by: Optional[str] = None,
    ) -> bool:
        """
        Apply a compliance officer's verdict to the replay buffer.

        Two writes happen:
          1. The original decision entry gets the manual reward (penalty or
             confirmation) plus the officer's verdict, and is reset to unused
             so the next training cycle re-samples it (Issue #54).
          2. When the officer's correct action contradicts the recorded action,
             a mirrored *counterfactual* entry is inserted — same state, the
             corrected action, positive reward — so the critic finally sees
             evidence that the correct action is good, not merely that the
             taken action was bad.

        Returns True if the original entry was found and updated.
        """
        entry = await replay_buffer_repository.apply_manual_reward(
            payment_id=payment_id,
            manual_reward=manual_reward,
            reviewed_by=reviewed_by,
            officer_decision=officer_decision,
            feedback_type=feedback_type,
        )
        if entry is None:
            return False

        correct_action = "ALLOW" if officer_decision == "APPROVE" else "BLOCK"
        correct_action_int = 1 if correct_action == "ALLOW" else 0
        recorded_action_int = 0 if entry.marl_action == "BLOCK" else 1

        if correct_action_int == recorded_action_int:
            return True  # Original entry already carries the corrective signal

        counterfactual = AgentReplayBufferEntry(
            id=uuid.uuid4(),
            payment_id=payment_id,
            state=entry.state,
            actions={name: correct_action_int for name in entry.actions},
            next_state=entry.next_state,
            done=entry.done,
            automated_reward=0.0,
            manual_reward=counterfactual_reward,
            effective_reward=counterfactual_reward,
            reward_source="counterfactual",
            officer_decision=officer_decision,
            feedback_type=feedback_type,
            marl_action=correct_action,
            marl_confidence=entry.marl_confidence,
            marl_q_value=entry.marl_q_value,
            mean_risk_score=entry.mean_risk_score,
            is_used_in_training=False,
            created_at=datetime.now(timezone.utc),
            updated_at=datetime.now(timezone.utc),
        )
        await replay_buffer_repository.save(counterfactual)
        logger.info(
            f"🪞 Counterfactual experience injected: payment={payment_id} "
            f"{entry.marl_action} → {correct_action} reward={counterfactual_reward:.4f}"
        )
        return True

    # ──────────────────────────────────────────────────────────────────────────────
    # Write path: evict stale experiences
    # ──────────────────────────────────────────────────────────────────────────────
    async def evict_old_experiences(self, retention_days: int) -> int:
        """
        Remove replay buffer entries older than *retention_days* days.

        Only already-trained entries are removed so brand-new un-trained
        samples are never silently discarded.

        Args:
            retention_days: Retention window; 0 means keep forever.

        Returns:
            Number of rows deleted.
        """
        if retention_days <= 0:
            return 0
        count = await replay_buffer_repository.evict_older_than(retention_days)
        logger.info(
            f"🗑️  Experience eviction complete: {count} rows removed "
            f"(retention={retention_days} days)"
        )
        return count
    # ──────────────────────────────────────────────────────────────────────────
    @staticmethod
    def entries_to_numpy(
        entries: List[AgentReplayBufferEntry],
        agent_names: List[str],
    ):
        """
        Convert a list of DB entries into numpy arrays suitable for
        pushing into the in-memory ReplayBuffer.

        Args:
            entries:      List of AgentReplayBufferEntry ORM objects.
            agent_names:  Ordered list of agent names (e.g. ["transaction",
                          "customer", "network"]) — must match the actions dict.

        Returns:
            Tuple (states, actions_list, rewards, next_states, dones):
              - states       : np.ndarray  [N, state_dim]
              - actions_list : List[np.ndarray]  len=num_agents, each [N, 1]
              - rewards      : np.ndarray  [N]
              - next_states  : np.ndarray  [N, state_dim]
              - dones        : np.ndarray  [N]  (float)
        """
        states = np.array([e.state for e in entries], dtype=np.float32)
        rewards = np.array([e.effective_reward for e in entries], dtype=np.float32)
        next_states = np.array([e.next_state for e in entries], dtype=np.float32)
        dones = np.array([float(e.done) for e in entries], dtype=np.float32)

        # Build per-agent action arrays (shape [N, 1] → one-hot [N, action_dim])
        # The DB stores action as int (0=BLOCK, 1=ALLOW); convert to one-hot for critic
        num_agents = len(agent_names)
        action_dim = 2  # BLOCK=0, ALLOW=1

        actions_list = []
        for name in agent_names:
            agent_actions = np.array(
                [e.actions.get(name, 1) for e in entries], dtype=np.int64
            )
            # One-hot encode
            one_hot = np.zeros((len(entries), action_dim), dtype=np.float32)
            one_hot[np.arange(len(entries)), agent_actions] = 1.0
            actions_list.append(one_hot)

        return states, actions_list, rewards, next_states, dones


# ─────────────────────────────────────────────────────────────────────────────
# Singleton
# ─────────────────────────────────────────────────────────────────────────────
experience_buffer_service = ExperienceBufferService()
