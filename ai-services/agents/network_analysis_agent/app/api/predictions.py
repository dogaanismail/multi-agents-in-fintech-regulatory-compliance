"""
Prediction endpoints for account risk assessment
"""

from fastapi import APIRouter, HTTPException
from typing import List

from ..models.schemas import (
    AccountRiskInput,
    AccountRiskPrediction,
    BatchAccountRiskInput,
    BatchRiskPredictionResponse
)
from ..services.prediction_service import prediction_service
from ..services.model_loader import model_loader
from ..core.config import settings
from ..core.logging import logger

router = APIRouter()


@router.post("/predict", response_model=AccountRiskPrediction)
async def predict_account_risk(account: AccountRiskInput):
    """
    Predict risk for a single account based on network topology features
    
    Args:
        account: Account data with network topology features
    
    Returns:
        Risk prediction with suspicion probability, risk level, and recommendations
    
    Raises:
        HTTPException: If model not loaded or prediction fails
    """
    try:
        if not model_loader.is_loaded():
            raise HTTPException(status_code=503, detail="Model not loaded")
        
        prediction = prediction_service.predict_single(account)
        
        logger.info(f"Prediction for account {account.account_id}: "
                   f"suspicious={prediction.is_suspicious}, "
                   f"score={prediction.risk_score:.2f}")
        
        return prediction
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Prediction failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@router.post("/predict/batch", response_model=BatchRiskPredictionResponse)
async def predict_batch_account_risk(batch: BatchAccountRiskInput):
    """
    Predict risk for multiple accounts in batch
    
    Args:
        batch: Batch of account data
    
    Returns:
        Batch prediction results with statistics
    
    Raises:
        HTTPException: If model not loaded, batch too large, or prediction fails
    """
    try:
        if not model_loader.is_loaded():
            raise HTTPException(status_code=503, detail="Model not loaded")
        
        # Validate batch size
        if len(batch.accounts) > settings.max_batch_size:
            raise HTTPException(
                status_code=400,
                detail=f"Batch size {len(batch.accounts)} exceeds maximum {settings.max_batch_size}"
            )
        
        # Make predictions
        predictions, processing_time = prediction_service.predict_batch(batch.accounts)
        
        # Calculate statistics
        suspicious_count = sum(1 for p in predictions if p.is_suspicious)
        normal_count = len(predictions) - suspicious_count
        average_risk_score = sum(p.risk_score for p in predictions) / len(predictions)
        
        logger.info(f"Batch prediction: {len(predictions)} accounts, "
                   f"{suspicious_count} suspicious, "
                   f"avg_score={average_risk_score:.2f}")
        
        return BatchRiskPredictionResponse(
            total_accounts=len(predictions),
            suspicious_count=suspicious_count,
            normal_count=normal_count,
            average_risk_score=average_risk_score,
            predictions=predictions,
            processing_time_ms=processing_time
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Batch prediction failed: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Batch prediction failed: {str(e)}")


@router.get("/predict/example")
async def get_prediction_example():
    """
    Get an example of prediction request/response
    
    Returns:
        Example request and expected response structure
    """
    return {
        "request_example": {
            "account_id": "ACC_789012",
            "features": {
                "in_degree": 45,
                "out_degree": 38,
                "degree_centrality": 0.0285,
                "in_degree_centrality": 0.0155,
                "out_degree_centrality": 0.0131,
                "betweenness_centrality": 0.0012,
                "closeness_centrality": 0.4521,
                "pagerank": 0.00089,
                "eigenvector_centrality": 0.0234,
                "clustering_coefficient": 0.0156,
                "community": 5
            }
        },
        "response_example": {
            "account_id": "ACC_789012",
            "is_suspicious": True,
            "suspicion_probability": 0.78,
            "risk_score": 78.0,
            "risk_level": "HIGH",
            "confidence": "HIGH",
            "recommendation": "URGENT REVIEW - Flag for AML investigation and enhanced monitoring",
            "network_indicators": {
                "centrality_metrics": {
                    "pagerank": 0.00089,
                    "eigenvector_centrality": 0.0234,
                    "betweenness_centrality": 0.0012,
                    "closeness_centrality": 0.4521
                },
                "connectivity": {
                    "in_degree": 45,
                    "out_degree": 38,
                    "total_degree": 83
                },
                "network_position": {
                    "clustering_coefficient": 0.0156,
                    "community_id": 5
                },
                "risk_flags": [
                    "High eigenvector centrality - connected to important nodes",
                    "High out-degree - dispersed transaction pattern"
                ]
            }
        },
        "feature_descriptions": {
            "in_degree": "Number of incoming transaction connections",
            "out_degree": "Number of outgoing transaction connections",
            "degree_centrality": "Proportion of nodes connected to this account",
            "pagerank": "Importance score based on connection quality",
            "eigenvector_centrality": "Influence based on connection to important nodes",
            "betweenness_centrality": "How often account acts as bridge between others",
            "closeness_centrality": "How close account is to all other accounts",
            "clustering_coefficient": "How tightly connected neighbors are",
            "community": "Network community/cluster membership"
        }
    }
