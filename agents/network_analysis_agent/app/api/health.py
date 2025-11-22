"""
Health check endpoint
"""

from fastapi import APIRouter, HTTPException
from datetime import datetime

from ..models.schemas import HealthResponse
from ..services.model_loader import model_loader
from ..core.logging import logger

router = APIRouter()


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Health check endpoint
    
    Returns:
        Health status and model information
    """
    try:
        is_loaded = model_loader.is_loaded()
        
        model_info = None
        if is_loaded:
            model_info = {
                "name": model_loader.metadata.get('model_info', {}).get('name', 'Unknown'),
                "version": model_loader.metadata.get('model_info', {}).get('version', 'Unknown'),
                "roc_auc": model_loader.metadata.get('performance', {}).get('roc_auc', 0)
            }
        
        return HealthResponse(
            status="healthy" if is_loaded else "unhealthy",
            model_loaded=model_loader.model is not None,
            scaler_loaded=model_loader.scaler is not None,
            timestamp=datetime.utcnow().isoformat(),
            model_info=model_info
        )
        
    except Exception as e:
        logger.error(f"Health check failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Health check failed: {str(e)}")
