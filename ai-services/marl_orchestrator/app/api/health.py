"""
Health check endpoint
"""

from fastapi import APIRouter
from datetime import datetime

from ..models.schemas import HealthResponse
from ..services.agent_orchestrator import agent_orchestrator
from ..core.dynamic_config import dynamic_config
from ..core.config import settings

router = APIRouter()


@router.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """
    Health check endpoint.

    Returns:
        Health status of MARL orchestrator, detection agents, and
        configuration-service integration.
    """
    # Check detection agents
    agents_status = await agent_orchestrator.check_all_agents_health()

    # Check if MADDPG models are loaded
    maddpg_loaded = True

    # Configuration-service status
    cfg_healthy        = dynamic_config.is_service_healthy
    last_refreshed     = dynamic_config.last_refreshed
    cache_size         = dynamic_config.cache_size
    freeze_active      = dynamic_config.get_bool(
        "FREEZE_TRAINING", settings.freeze_training
    )

    # Overall status: degraded when config-service is down but still operational
    all_agents_up = all(v == "healthy" for v in agents_status.values())
    status = "healthy" if (maddpg_loaded and all_agents_up) else "degraded"

    return HealthResponse(
        status=status,
        maddpg_loaded=maddpg_loaded,
        detection_agents_status=agents_status,
        timestamp=datetime.now().isoformat(),
        configuration_service_healthy=cfg_healthy,
        dynamic_config_last_refreshed=(
            last_refreshed.isoformat() if last_refreshed else None
        ),
        dynamic_config_cache_size=cache_size,
        freeze_training_active=freeze_active,
    )
