"""
MARL Orchestrator — entry point.

Startup order:
  1. Database tables initialised
  2. MADDPG model weights loaded (if available)
  3. Dynamic config pre-warmed from configuration-service
  4. Offline training scheduler started
  5. Kafka listener started

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import asyncio
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import api_router
from app.consumers.fraud_analysis_requested_listener import fraud_analysis_requested_listener
from app.core.config import settings
from app.core.dynamic_config import dynamic_config
from app.core.logging import logger
from app.infrastructure.database.database import init_db
from app.services.offline_trainer_service import offline_trainer_service


# ─────────────────────────────────────────────────────────────────────────────
# Startup helpers — one responsibility each
# ─────────────────────────────────────────────────────────────────────────────

async def _init_database() -> None:
    """Create database tables if they do not yet exist."""
    try:
        await init_db()
        logger.info("✅ Database tables ready")
    except Exception as exc:
        logger.error(f"❌ Database initialisation failed: {exc}")
        logger.warning("⚠️  Continuing without database — offline learning disabled")


async def _warm_dynamic_config() -> None:
    """
    Pre-warm the in-memory dynamic config cache by fetching from configuration-service.

    Must be called before the training scheduler starts so that
    the initial APScheduler interval and reward weights are read from the DB
    rather than from env-var defaults.
    Falls back silently to env-var values when configuration-service is unreachable.
    """
    logger.info("🔧 Pre-warming dynamic config from configuration-service…")
    success = await dynamic_config.refresh()
    if success:
        logger.info(
            f"✅ Dynamic config ready — "
            f"{dynamic_config.cache_size} entries loaded "
            f"(configuration-service is healthy)"
        )
    else:
        logger.warning(
            "⚠️  Dynamic config pre-warm failed — "
            "using env-var defaults; configuration-service may be starting up"
        )


def _load_model_weights() -> None:
    """
    Load persisted Actor/Critic weights from disk when available.
    On first boot (no .pth files) the networks start with random weights and
    improve automatically after the first offline training cycle.
    """
    from maddpg.core import maddpg_coordinator

    model_dir = Path(settings.model_path)
    pth_files = list(model_dir.glob("*.pth")) if model_dir.exists() else []

    if not pth_files:
        logger.warning(
            f"⚠️  No .pth files in '{settings.model_path}' — "
            "starting with random weights. "
            "Networks will improve after the first training cycle."
        )
        return

    try:
        maddpg_coordinator.load_models()
        logger.info(f"✅ Model weights loaded ({len(pth_files)} files) from {settings.model_path}")
    except Exception as exc:
        logger.warning(f"⚠️  Failed to load model weights: {exc} — starting with random weights")


def _start_training_scheduler() -> None:
    """Start the APScheduler background retraining job."""
    try:
        offline_trainer_service.start()
        logger.info(
            f"✅ Training scheduler started "
            f"(every {settings.training_interval_seconds}s, "
            f"min {settings.min_experiences_for_training} experiences)"
        )
    except Exception as exc:
        logger.error(f"❌ Failed to start training scheduler: {exc}")


# ─────────────────────────────────────────────────────────────────────────────
# Lifespan
# ─────────────────────────────────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info(
        f"🚀 Starting MARL Orchestrator "
        f"(env={settings.environment}, version={settings.app_version}, "
        f"mode={settings.maddpg_mode})"
    )

    await _init_database()
    _load_model_weights()
    await _warm_dynamic_config()
    _start_training_scheduler()

    logger.info("🎧 Starting Kafka listener...")
    listener_task = asyncio.create_task(fraud_analysis_requested_listener.start())

    logger.info("✅ MARL Orchestrator ready")
    yield

    logger.info("👋 Shutting down MARL Orchestrator...")
    offline_trainer_service.stop()
    fraud_analysis_requested_listener.stop()
    await listener_task


# ─────────────────────────────────────────────────────────────────────────────
# Application
# ─────────────────────────────────────────────────────────────────────────────

app = FastAPI(
    title=settings.app_name,
    description=settings.description,
    version=settings.app_version,
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=settings.cors_credentials,
    allow_methods=settings.cors_methods,
    allow_headers=settings.cors_headers,
)

app.include_router(api_router, prefix=settings.api_v1_prefix)


@app.get("/", tags=["General"])
async def root():
    return {
        "service": settings.app_name,
        "version": settings.app_version,
        "status": "operational",
        "environment": settings.environment,
        "docs": "/docs",
        "api": settings.api_v1_prefix,
    }


if __name__ == "__main__":
    import uvicorn
    
    uvicorn.run(
        "main:app",
        host=settings.host,
        port=settings.port,
        workers=settings.workers,
        log_level=settings.log_level,
        reload=settings.debug
    )
