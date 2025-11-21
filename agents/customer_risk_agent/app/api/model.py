"""
Model information endpoint
"""

from fastapi import APIRouter, HTTPException, status

from ..models.schemas import ModelInfoResponse
from ..services.model_loader import model_loader
from ..core.logging import logger

router = APIRouter()


@router.get("/model-info", response_model=ModelInfoResponse, tags=["Model"])
async def get_model_info():
    """
    Get detailed information about the loaded model
    
    Returns:
        Model metadata including performance metrics and configuration
    
    Raises:
        HTTPException: If model is not loaded
    """
    if not model_loader.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model not loaded"
        )
    
    try:
        metadata = model_loader.model_metadata
        
        return ModelInfoResponse(
            model_name=metadata['model_name'],
            model_type=type(model_loader.model).__name__,
            training_date=metadata['training_date'],
            performance_metrics=metadata['metrics'],
            training_config=metadata['training_config'],
            feature_names=model_loader.feature_names,
            num_features=len(model_loader.feature_names)
        )
        
    except Exception as e:
        logger.error(f"Error retrieving model info: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to retrieve model information: {str(e)}"
        )
