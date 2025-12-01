"""
Prediction endpoints
"""

from fastapi import APIRouter, HTTPException, status
from typing import List

from ..models.schemas import (
    CustomerRiskInput,
    BatchCustomerRiskInput,
    CustomerRiskPrediction,
    BatchRiskPredictionResponse
)
from ..services.prediction_service import prediction_service
from ..services.model_loader import model_loader
from ..core.logging import logger

router = APIRouter()


@router.post("/assess-risk", response_model=CustomerRiskPrediction, tags=["Prediction"])
async def assess_customer_risk(customer: CustomerRiskInput):
    """
    Assess risk for a single customer
    
    Args:
        customer: Customer data with aggregated features
    
    Returns:
        Risk assessment with probability, risk level, and recommendations
    
    Raises:
        HTTPException: If model is not loaded or prediction fails
    """
    if not model_loader.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model not loaded. Please check service health."
        )
    
    try:
        prediction = prediction_service.predict_single(customer)
        return prediction
        
    except Exception as e:
        logger.error(f"Risk assessment error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Risk assessment failed: {str(e)}"
        )


@router.post("/batch-assess-risk", response_model=BatchRiskPredictionResponse, tags=["Prediction"])
async def batch_assess_customer_risk(batch: BatchCustomerRiskInput):
    """
    Assess risk for multiple customers in batch
    
    Efficiently processes up to 500 customers at once
    
    Args:
        batch: Batch of customer data to analyze
    
    Returns:
        Batch risk assessments with statistics and processing time
    
    Raises:
        HTTPException: If model is not loaded or prediction fails
    """
    if not model_loader.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model not loaded. Please check service health."
        )
    
    try:
        predictions, processing_time = prediction_service.predict_batch(batch.customers)
        
        # Calculate statistics
        high_risk_count = sum(1 for p in predictions if p.is_high_risk)
        low_risk_count = len(predictions) - high_risk_count
        avg_risk_score = sum(p.risk_score for p in predictions) / len(predictions)
        
        return BatchRiskPredictionResponse(
            total_customers=len(predictions),
            high_risk_count=high_risk_count,
            low_risk_count=low_risk_count,
            average_risk_score=float(avg_risk_score),
            predictions=predictions,
            processing_time_ms=float(processing_time)
        )
        
    except Exception as e:
        logger.error(f"Batch risk assessment error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Batch risk assessment failed: {str(e)}"
        )
