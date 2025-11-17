"""
Model information endpoints
"""

from fastapi import APIRouter, HTTPException, status

from ..models.schemas import ModelInfoResponse
from ..services.model_loader import model_loader
from ..core.config import settings

router = APIRouter()

@router.get("/model-info", response_model=ModelInfoResponse, tags=["Model"])
async def get_model_info():
    """
    Get detailed model information and performance metrics
    
    Returns:
        Model metadata including performance metrics and hyperparameters
    
    Raises:
        HTTPException: If model metadata is not available
    """
    metadata = model_loader.get_metadata()
    
    if metadata is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model metadata not available"
        )
    
    return ModelInfoResponse(
        model_name=metadata.get("model_name", "Unknown"),
        model_type=metadata.get("model_type", "Unknown"),
        training_date=metadata.get("training_date", "Unknown"),
        dataset=metadata.get("dataset", "Unknown"),
        performance_metrics=metadata.get("performance_metrics", {}),
        hyperparameters=metadata.get("hyperparameters", {}),
        optimal_threshold=metadata.get("optimal_threshold", settings.optimal_threshold)
    )
