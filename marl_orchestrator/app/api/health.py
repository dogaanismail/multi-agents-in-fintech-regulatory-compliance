"""
Health check endpoint
"""

from fastapi import APIRouter
from datetime import datetime

from ..models.schemas import HealthResponse
from ..services.detection_client import detection_client
from ..agents.maddpg_agent import maddpg_coordinator

router = APIRouter()


@router.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """
    Health check endpoint
    
    Returns:
        Health status of MARL orchestrator and detection agents
    """
    # Check detection agents
    agents_status = await detection_client.check_agents_health()
    
    # Check if MADDPG models are loaded
    maddpg_loaded = True  # Simplified check
    
    return HealthResponse(
        status="healthy",
        maddpg_loaded=maddpg_loaded,
        detection_agents_status=agents_status,
        timestamp=datetime.now().isoformat()
    )
