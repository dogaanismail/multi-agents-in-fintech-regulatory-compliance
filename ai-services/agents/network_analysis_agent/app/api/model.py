"""
Model information endpoint
"""

from fastapi import APIRouter, HTTPException

from ..models.schemas import ModelInfoResponse
from ..services.model_loader import model_loader
from ..core.logging import logger

router = APIRouter()


@router.get("/model", response_model=ModelInfoResponse)
async def get_model_info():
    """
    Get detailed model information
    
    Returns:
        Model metadata, performance metrics, and configuration
    """
    try:
        if not model_loader.is_loaded():
            raise HTTPException(status_code=503, detail="Model not loaded")
        
        model_info = model_loader.get_model_info()
        
        return ModelInfoResponse(
            model_name=model_info['model_name'],
            model_type=model_info['model_type'],
            training_date=model_info['created_at'],
            performance_metrics=model_info['performance'],
            training_config=model_info['training_config'],
            feature_names=model_info['feature_names'],
            num_features=model_info['num_features'],
            network_stats=model_info['network_stats']
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to get model info: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to get model info: {str(e)}")


@router.get("/model/features")
async def get_feature_names():
    """
    Get list of feature names required for prediction
    
    Returns:
        List of feature names
    """
    try:
        if not model_loader.is_loaded():
            raise HTTPException(status_code=503, detail="Model not loaded")
        
        return {
            "feature_names": model_loader.feature_names,
            "num_features": len(model_loader.feature_names),
            "feature_description": "Network topology features (centrality metrics, clustering, community)"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to get feature names: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to get feature names: {str(e)}")
