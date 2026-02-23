"""
Offline Trainer Service — orchestrates periodic batch retraining of MADDPG.

Responsibilities (this file only):
  - Own the APScheduler lifecycle (start / stop).
  - Guard against concurrent training runs.
  - Coordinate the four delegates:
      ExperienceBufferService → sample / mark-as-used
      TrainingRunRepository   → create / update audit record
      MADDPGTrainerService    → load buffer, run updates, save weights

All database access lives in repositories.
All ML logic lives in MADDPGTrainerService.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import asyncio
import uuid
from datetime import datetime, timezone
from typing import Optional

from apscheduler.schedulers.asyncio import AsyncIOScheduler

from app.core.config import settings
from app.core.dynamic_config import dynamic_config
from app.core.logging import logger
from app.services.reward_calculator_service import reward_calculator_service
from app.infrastructure.database.models import AgentTrainingRun
from app.repositories.training_run_repository import training_run_repository
from app.services.experience_buffer_service import experience_buffer_service
from app.services.maddpg_trainer_service import maddpg_trainer_service


class OfflineTrainerService:
    """
    Background service that periodically retrains MADDPG networks using
    accumulated experiences from the persisted replay buffer.

    Decoupled from the inference path: training never blocks decisions.
    """

    def __init__(self):
        self._scheduler: Optional[AsyncIOScheduler] = None
        self._is_training: bool = False
        self._last_training_run_id: Optional[uuid.UUID] = None
        self._last_training_at: Optional[datetime] = None
        self._total_training_runs: int = 0
        self._total_experiences_trained: int = 0
        # Track the interval used when the APScheduler job was last (re-)scheduled
        # so we can detect changes and hot-reload without container restart.
        self._scheduled_interval: int = 0

    # ──────────────────────────────────────────────────────────────────────────
    # Lifecycle
    # ──────────────────────────────────────────────────────────────────────────
    def start(self) -> None:
        """
        Start the APScheduler AsyncIO scheduler.

        Should be called once during application startup (inside the
        FastAPI lifespan context manager).
        The initial interval is resolved from dynamic_config (pre-warmed at startup)
        so the DB value takes effect immediately on first boot.
        """
        # Resolve initial interval — dynamic_config was pre-warmed at startup
        # so the DB value is used when available, otherwise env-var default.
        initial_interval = dynamic_config.get_int(
            "TRAINING_INTERVAL_SECONDS", settings.training_interval_seconds
        )
        self._scheduled_interval = initial_interval

        self._scheduler = AsyncIOScheduler(timezone="UTC")
        self._scheduler.add_job(
            self._training_cycle,
            trigger="interval",
            seconds=initial_interval,
            id="offline_maddpg_training",
            name="Offline MADDPG Batch Retraining",
            max_instances=1,        # Never run two training jobs in parallel
            misfire_grace_time=60,  # Allow up to 60s late start
        )
        self._scheduler.start()
        min_exp = dynamic_config.get_int(
            "MIN_EXPERIENCES_FOR_TRAINING", settings.min_experiences_for_training
        )
        logger.info(
            f"🕐 Offline trainer scheduler started "
            f"(interval={initial_interval}s, "
            f"min_experiences={min_exp})"
        )

    def stop(self) -> None:
        """Gracefully stop the scheduler during application shutdown."""
        if self._scheduler and self._scheduler.running:
            self._scheduler.shutdown(wait=False)
            logger.info("🛑 Offline trainer scheduler stopped")

    def _reschedule_if_interval_changed(self) -> None:
        """
        Hot-reload the APScheduler job interval when TRAINING_INTERVAL_SECONDS
        has been updated in configuration-service.

        Compares the current dynamic_config value against the interval that was
        used the last time the job was (re-)scheduled.  If they differ, APScheduler
        is asked to reschedule the job in-place — no container restart needed.
        """
        new_interval = dynamic_config.get_int(
            "TRAINING_INTERVAL_SECONDS", settings.training_interval_seconds
        )
        if new_interval == self._scheduled_interval:
            return  # No change

        if self._scheduler and self._scheduler.running:
            try:
                self._scheduler.reschedule_job(
                    "offline_maddpg_training",
                    trigger="interval",
                    seconds=new_interval,
                )
                logger.info(
                    f"🔄 Offline trainer interval updated: "
                    f"{self._scheduled_interval}s → {new_interval}s"
                )
                self._scheduled_interval = new_interval
            except Exception as exc:
                logger.warning(
                    f"⚠️  Failed to reschedule offline trainer: {exc}"
                )

    # ──────────────────────────────────────────────────────────────────────────
    # Public API: manual trigger
    # ──────────────────────────────────────────────────────────────────────────
    async def trigger_now(self) -> dict:
        """
        Manually trigger a training cycle outside the scheduler interval.

        Returns a status dict indicating whether training ran or was skipped.
        Useful for testing and the training management API endpoint.
        """
        if self._is_training:
            return {
                "triggered": False,
                "reason": "A training cycle is already in progress",
                "last_run_id": str(self._last_training_run_id),
            }

        min_exp = dynamic_config.get_int(
            "MIN_EXPERIENCES_FOR_TRAINING", settings.min_experiences_for_training
        )
        batch_size = dynamic_config.get_int(
            "TRAINING_BATCH_SIZE", settings.training_batch_size
        )

        count = await experience_buffer_service.count_unused_experiences()
        if count < min_exp:
            return {
                "triggered": False,
                "reason": (
                    f"Not enough new experiences: {count} available, "
                    f"{min_exp} required"
                ),
                "available_experiences": count,
            }

        # Run in a background task so the HTTP response returns immediately
        asyncio.create_task(self._training_cycle())
        return {
            "triggered": True,
            "available_experiences": count,
            "batch_size": batch_size,
        }

    # ──────────────────────────────────────────────────────────────────────────
    # Status
    # ──────────────────────────────────────────────────────────────────────────
    async def get_status(self) -> dict:
        """Return a status summary for the training management API."""
        unused = await experience_buffer_service.count_unused_experiences()
        total = await experience_buffer_service.count_all_experiences()

        return {
            "scheduler_running": self._scheduler.running if self._scheduler else False,
            "is_training": self._is_training,
            "training_interval_seconds": dynamic_config.get_int(
                "TRAINING_INTERVAL_SECONDS", settings.training_interval_seconds
            ),
            "scheduled_interval_seconds": self._scheduled_interval,
            "min_experiences_required": dynamic_config.get_int(
                "MIN_EXPERIENCES_FOR_TRAINING", settings.min_experiences_for_training
            ),
            "configuration_service_healthy": dynamic_config.is_service_healthy,
            "dynamic_config_last_refreshed": (
                dynamic_config.last_refreshed.isoformat()
                if dynamic_config.last_refreshed else None
            ),
            "unused_experiences": unused,
            "total_experiences": total,
            "last_training_run_id": str(self._last_training_run_id) if self._last_training_run_id else None,
            "last_training_at": self._last_training_at.isoformat() if self._last_training_at else None,
            "total_training_runs": self._total_training_runs,
            "total_experiences_trained": self._total_experiences_trained,
        }

    # ──────────────────────────────────────────────────────────────────────────
    # Core training cycle (runs in background)
    # ──────────────────────────────────────────────────────────────────────────
    async def _training_cycle(self) -> None:
        """
        Main offline retraining cycle.

        Called by the scheduler every `training_interval_seconds`.
        Uses a guard flag to prevent concurrent executions.
        """
        # ── Refresh dynamic config + hot-reload interval ──────────────────────
        await dynamic_config.refresh_if_stale()
        reward_calculator_service.refresh()
        self._reschedule_if_interval_changed()

        # ── Compliance freeze guard ───────────────────────────────────────────
        # Compliance officers can halt MADDPG learning during regulatory audits
        # by toggling FREEZE_TRAINING in configuration-service — no restart needed.
        if dynamic_config.get_bool("FREEZE_TRAINING", settings.freeze_training):
            logger.info(
                "🔒 Training cycle skipped — FREEZE_TRAINING is active "
                "(set by compliance officer via configuration-service)"
            )
            return

        if self._is_training:
            logger.warning("⚠️  Training cycle skipped: previous run still in progress")
            return

        # ── Pre-flight check ──────────────────────────────────────────────────
        unused_count = await experience_buffer_service.count_unused_experiences()
        if unused_count < settings.min_experiences_for_training:
            logger.info(
                f"⏭️  Training skipped: {unused_count} / "
                f"{settings.min_experiences_for_training} experiences available"
            )
            return

        self._is_training = True
        run_id = uuid.uuid4()
        run = AgentTrainingRun(
            id=run_id,
            status="running",
            batch_size=settings.training_batch_size,
            started_at=datetime.now(timezone.utc),
        )
        await training_run_repository.create(run)

        logger.info(
            f"🏋️  Offline training cycle started (run_id={run_id}, "
            f"available_experiences={unused_count})"
        )

        try:
            await self._run_training(run_id, unused_count)
        except Exception as exc:
            logger.error(f"❌ Training cycle failed: {exc}", exc_info=True)
            await training_run_repository.update(
                run_id,
                status="failed",
                error_message=str(exc),
                completed_at=datetime.now(timezone.utc),
            )
        finally:
            self._is_training = False

    async def _run_training(self, run_id: uuid.UUID, available_count: int) -> None:
        """
        Coordinate the training delegates — no ML or SQL in this method.

          1. Sample experiences from DB  (ExperienceBufferService)
          2. Load into in-memory buffer  (MADDPGTrainerService)
          3. Run gradient updates        (MADDPGTrainerService)
          4. Save model weights          (MADDPGTrainerService)
          5. Mark experiences as used    (ExperienceBufferService)
          6. Persist audit record        (TrainingRunRepository)
        """
        # Read training parameters from dynamic_config (with env-var fallbacks)
        batch_size = dynamic_config.get_int(
            "TRAINING_BATCH_SIZE", settings.training_batch_size
        )
        max_per_batch = dynamic_config.get_int(
            "MAX_EXPERIENCES_PER_BATCH", settings.max_experiences_per_batch
        )
        save_after_training = dynamic_config.get_bool(
            "SAVE_MODEL_AFTER_TRAINING", settings.save_model_after_training
        )

        load_size = min(available_count, max_per_batch)

        # ── 1. Sample experiences from DB ─────────────────────────────────────
        entries = await experience_buffer_service.sample_batch(
            batch_size=load_size, 
            include_used=False
        )
        
        if not entries:
            logger.warning("No entries returned from DB sample; skipping training")
            await training_run_repository.update(
                run_id,
                status="failed",
                error_message="No entries from DB",
                completed_at=datetime.now(timezone.utc),
            )
            return

        logger.info(f"  Loaded {len(entries)} experiences from DB replay buffer")

        # ── 2 & 3. Load buffer + run gradient updates ─────────────────────────
        loaded = await maddpg_trainer_service.load_experiences_into_buffer(entries)
        logger.info(f"  In-memory buffer loaded: {loaded} entries")

        result = await maddpg_trainer_service.run_update_steps(
            batch_size=batch_size,
            num_steps=max(1, len(entries) // batch_size),
        )
        steps_done = result["steps_done"]
        last_losses = result["losses"]

        # ── 4. Save model weights ──────────────────────────────────────────────────
        model_saved = False
        if save_after_training and steps_done > 0:
            model_saved = await maddpg_trainer_service.save_model_weights()

        # ── 5. Mark experiences as used ───────────────────────────────────────
        entry_ids = [e.id for e in entries]
        await experience_buffer_service.mark_as_used(entry_ids, run_id)

        # ── 5b. Evict expired experiences ─────────────────────────────────────
        # Removes replay buffer rows older than EXPERIENCE_RETENTION_DAYS so the
        # buffer does not grow unbounded and old fraud patterns do not dilute the
        # current reward signal.
        retention_days = dynamic_config.get_int(
            "EXPERIENCE_RETENTION_DAYS", settings.experience_retention_days
        )
        if retention_days > 0:
            evicted = await experience_buffer_service.evict_old_experiences(retention_days)
            if evicted > 0:
                logger.info(f"🗑️  Evicted {evicted} experiences older than {retention_days} days")

        # ── 6. Persist audit record ───────────────────────────────────────────
        await training_run_repository.update(
            run_id,
            status="completed",
            experiences_count=len(entries),
            train_steps_completed=steps_done,
            critic_loss=last_losses.get("critic_loss"),
            actor_transaction_loss=last_losses.get("actor_loss_transaction"),
            actor_customer_loss=last_losses.get("actor_loss_customer"),
            actor_network_loss=last_losses.get("actor_loss_network"),
            model_saved=model_saved,
            completed_at=datetime.now(timezone.utc),
        )

        # ── Update in-memory stats ────────────────────────────────────────────
        self._last_training_run_id = run_id
        self._last_training_at = datetime.now(timezone.utc)
        self._total_training_runs += 1
        self._total_experiences_trained += len(entries)

        logger.info(
            f"✅ Offline training run {run_id} completed "
            f"({len(entries)} experiences, {steps_done} steps, "
            f"model_saved={model_saved})"
        )


# ─────────────────────────────────────────────────────────────────────────────
# Singleton
# ─────────────────────────────────────────────────────────────────────────────
offline_trainer_service = OfflineTrainerService()
