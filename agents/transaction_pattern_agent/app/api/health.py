"""
Health check endpoints
"""

from fastapi import APIRouter
from datetime import datetime

from ..models.schemas import HealthResponse
from ..services.model_loader import model_loader
from ..core.config import settings

router = APIRouter()

@router.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """
    Health check endpoint
    
    Returns:
        Health status including model loading state
    """
    return HealthResponse(
        status="healthy" if model_loader.is_loaded else "unhealthy",
        model_loaded=model_loader.model is not None,
        preprocessor_loaded=model_loader.preprocessor is not None,
        timestamp=datetime.now().isoformat(),
        model_info={
            "type": "XGBClassifier",
            "threshold": settings.optimal_threshold
        } if model_loader.is_loaded else None
    )
