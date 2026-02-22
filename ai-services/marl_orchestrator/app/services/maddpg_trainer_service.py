"""
MADDPG Trainer Service — pure machine learning responsibility.

Responsibility:
  - Load DB experiences into the in-memory ReplayBuffer.
  - Run MADDPG gradient update steps.
  - Persist updated model weights to disk.

No database access. No scheduling. No business logic.
Receives pre-loaded entries and returns training results.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import asyncio
from typing import List

from app.core.config import settings
from app.core.logging import logger
from app.infrastructure.database.models import AgentReplayBufferEntry
from maddpg.constants import AGENT_NAMES


class MADDPGTrainerService:
    """
    Executes gradient update steps on the MADDPG Actor-Critic networks.

    Called by OfflineTrainerService after it has checked the buffer and
    created the training run record. This service knows nothing about
    scheduling, databases, or API responses.
    """

    async def load_experiences_into_buffer(
        self, entries: List[AgentReplayBufferEntry]
    ) -> int:
        """
        Convert DB entries to numpy arrays and push them into the
        in-memory ReplayBuffer.

        Clears the buffer first so training is deterministic and uses
        only the current DB batch (no stale in-memory data).

        Runs the buffer fill in a thread-pool executor to avoid blocking
        the asyncio event loop.

        Returns the number of entries loaded.
        """
        # Import inside method to avoid circular imports at module init
        from maddpg.core import maddpg_coordinator
        from app.services.experience_buffer_service import experience_buffer_service

        states, actions_list, rewards, next_states, dones = (
            experience_buffer_service.entries_to_numpy(entries, AGENT_NAMES)
        )

        def _fill_buffer():
            maddpg_coordinator.replay_buffer.clear()
            for i in range(len(entries)):
                per_agent_actions = [a[i] for a in actions_list]
                maddpg_coordinator.replay_buffer.push(
                    state=states[i],
                    actions=per_agent_actions,
                    reward=float(rewards[i]),
                    next_state=next_states[i],
                    done=bool(dones[i]),
                )
            return len(entries)

        loop = asyncio.get_event_loop()
        count = await loop.run_in_executor(None, _fill_buffer)
        logger.info(f"  In-memory buffer loaded: {count} entries")
        return count

    async def run_update_steps(self, batch_size: int, num_steps: int) -> dict:
        """
        Run `num_steps` MADDPG gradient updates.

        Offloads the blocking PyTorch forward/backward passes to a
        thread-pool executor so the asyncio event loop stays responsive.

        Returns a summary dict:
            {
                "steps_done": int,
                "losses": {
                    "critic_loss": float,
                    "actor_loss_transaction": float,
                    "actor_loss_customer": float,
                    "actor_loss_network": float,
                }
            }
        """
        from maddpg.core import maddpg_coordinator

        def _train():
            steps_done = 0
            last_losses: dict = {}

            for step in range(num_steps):
                losses = maddpg_coordinator.update(batch_size=batch_size)
                if losses is None:
                    logger.warning(f"  Step {step}: buffer exhausted, stopping early")
                    break
                last_losses = losses
                steps_done += 1

                if step % 10 == 0:
                    logger.info(
                        f"  Step {step}/{num_steps}: "
                        f"critic={losses.get('critic_loss', 0):.4f} | "
                        f"txn={losses.get('actor_loss_transaction', 0):.4f} | "
                        f"cust={losses.get('actor_loss_customer', 0):.4f} | "
                        f"net={losses.get('actor_loss_network', 0):.4f}"
                    )

            logger.info(
                f"  Gradient updates complete: {steps_done}/{num_steps} steps, "
                f"final critic_loss={last_losses.get('critic_loss', 'N/A')}"
            )
            return {"steps_done": steps_done, "losses": last_losses}

        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(None, _train)

    async def save_model_weights(self) -> bool:
        """
        Persist the updated Actor and Critic weights to disk.

        Runs torch.save() calls in a thread-pool executor to keep
        disk I/O off the asyncio event loop.

        Returns True on success, False on failure (non-fatal).
        """
        from maddpg.core import maddpg_coordinator

        def _save():
            maddpg_coordinator.save_models()

        try:
            loop = asyncio.get_event_loop()
            await loop.run_in_executor(None, _save)
            logger.info("  ✅ Updated model weights saved to disk")
            return True
        except Exception as exc:
            logger.error(f"  ⚠️  Failed to save model weights: {exc}")
            return False


# Singleton
maddpg_trainer_service = MADDPGTrainerService()
