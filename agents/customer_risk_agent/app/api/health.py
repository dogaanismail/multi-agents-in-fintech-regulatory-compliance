"""
Health check endpoint
"""

from fastapi import APIRouter
from datetime import datetime

from ..models.schemas import HealthResponse
from ..services.model_loader import model_loader

router = APIRouter()


@router.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """
    Health check endpoint
    
    Returns:
        Service health status and model information
    """
    is_loaded = model_loader.is_loaded
    
    response = HealthResponse(
        status="healthy" if is_loaded else "unhealthy",
        model_loaded=is_loaded,
        scaler_loaded=is_loaded and model_loader.scaler is not None,
        timestamp=datetime.utcnow().isoformat(),
        model_info=model_loader.get_model_info() if is_loaded else None
    )
    
    return response
