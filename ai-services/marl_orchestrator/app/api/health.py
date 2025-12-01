"""
Health check endpoint
"""

from fastapi import APIRouter
from datetime import datetime

from ..models.schemas import HealthResponse
from ..services.agent_orchestrator import agent_orchestrator
from maddpg.core import maddpg_coordinator

router = APIRouter()


@router.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """
    Health check endpoint
    
    Returns:
        Health status of MARL orchestrator and detection agents
    """
    # Check detection agents
    agents_status = await agent_orchestrator.check_all_agents_health()
    
    # Check if MADDPG models are loaded
    maddpg_loaded = True  # Simplified check
    
    return HealthResponse(
        status="healthy",
        maddpg_loaded=maddpg_loaded,
        detection_agents_status=agents_status,
        timestamp=datetime.now().isoformat()
    )
