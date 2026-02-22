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
    async def apply_manual_reward(
        self,
        payment_id: str,
        manual_reward: float,
        reviewed_by: Optional[str] = None,
    ) -> bool:
        """
        Override the reward with a compliance officer's decision.

        Resets is_used_in_training=False so the entry is re-sampled in the
        next training cycle with the corrected reward (Issue #54).

        Returns True if the entry was found and updated, False otherwise.
        """
        return await replay_buffer_repository.apply_manual_reward(
            payment_id, manual_reward, reviewed_by
        )

    # ──────────────────────────────────────────────────────────────────────────
    # Helper: convert DB entries → numpy arrays for in-memory ReplayBuffer
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
