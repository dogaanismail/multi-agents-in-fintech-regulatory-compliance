"""
Prediction endpoints
"""

from fastapi import APIRouter, HTTPException, status
from typing import List

from ..models.schemas import (
    TransactionInput,
    BatchTransactionInput,
    TransactionPrediction,
    BatchPredictionResponse
)
from ..services.prediction_service import prediction_service
from ..services.model_loader import model_loader
from ..core.logging import logger

router = APIRouter()


@router.post("/predict", response_model=TransactionPrediction, tags=["Prediction"])
async def predict_transaction(transaction: TransactionInput):
    """
    Predict if a single transaction is suspicious
    
    Args:
        transaction: Transaction data to analyze
    
    Returns:
        Prediction with fraud probability, risk score, and recommendation
    
    Raises:
        HTTPException: If model is not loaded or prediction fails
    """
    if not model_loader.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model not loaded. Please check service health."
        )
    
    try:
        prediction = prediction_service.predict_single(transaction)
        return prediction
        
    except Exception as e:
        logger.error(f"Prediction error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Prediction failed: {str(e)}"
        )


@router.post("/batch-predict", response_model=BatchPredictionResponse, tags=["Prediction"])
async def batch_predict_transactions(batch: BatchTransactionInput):
    """
    Predict multiple transactions in batch
    
    Efficiently processes up to 1000 transactions at once
    
    Args:
        batch: Batch of transactions to analyze
    
    Returns:
        Batch predictions with statistics and processing time
    
    Raises:
        HTTPException: If model is not loaded or prediction fails
    """
    if not model_loader.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model not loaded. Please check service health."
        )
    
    try:
        predictions, processing_time = prediction_service.predict_batch(batch.transactions)
        
        # Calculate statistics
        suspicious_count = sum(1 for p in predictions if p.is_suspicious)
        legitimate_count = len(predictions) - suspicious_count
        avg_risk_score = sum(p.risk_score for p in predictions) / len(predictions)
        
        return BatchPredictionResponse(
            total_transactions=len(predictions),
            suspicious_count=suspicious_count,
            legitimate_count=legitimate_count,
            average_risk_score=float(avg_risk_score),
            predictions=predictions,
            processing_time_ms=float(processing_time)
        )
        
    except Exception as e:
        logger.error(f"Batch prediction error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Batch prediction failed: {str(e)}"
        )
