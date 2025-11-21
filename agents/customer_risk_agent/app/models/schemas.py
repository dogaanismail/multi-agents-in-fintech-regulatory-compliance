"""
Pydantic schemas for Customer Risk Agent request/response models
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime


class CustomerFeaturesInput(BaseModel):
    """
    Input schema for customer-level features
    These are pre-aggregated features from a customer's transaction history
    """
    transaction_count: int = Field(..., gt=0, description="Total number of transactions")
    total_amount: float = Field(..., gt=0, description="Total transaction amount")
    avg_amount: float = Field(..., gt=0, description="Average transaction amount")
    median_amount: float = Field(..., gt=0, description="Median transaction amount")
    max_amount: float = Field(..., gt=0, description="Maximum transaction amount")
    min_amount: float = Field(..., gt=0, description="Minimum transaction amount")
    std_amount: float = Field(..., ge=0, description="Standard deviation of amounts")
    active_days: int = Field(..., gt=0, description="Number of active days")
    transactions_per_day: float = Field(..., gt=0, description="Transaction velocity")
    cross_border_ratio: float = Field(..., ge=0, le=1, description="Ratio of cross-border transactions")
    cash_transaction_ratio: float = Field(..., ge=0, le=1, description="Ratio of cash transactions")
    amount_consistency: float = Field(..., ge=0, description="Amount consistency indicator")
    large_transaction_ratio: float = Field(..., ge=0, le=1, description="Ratio of large transactions (>10k)")
    unique_receivers: int = Field(..., ge=0, description="Number of unique receiver accounts")
    unique_receiver_countries: int = Field(..., ge=0, description="Number of unique receiver countries")
    receiver_diversity: float = Field(..., ge=0, le=1, description="Receiver diversity metric")
    night_transaction_ratio: float = Field(..., ge=0, le=1, description="Ratio of night transactions")
    weekend_transaction_ratio: float = Field(..., ge=0, le=1, description="Ratio of weekend transactions")
    unique_currencies: int = Field(..., ge=0, description="Number of unique currencies used")

    class Config:
        schema_extra = {
            "example": {
                "transaction_count": 25,
                "total_amount": 125000.00,
                "avg_amount": 5000.00,
                "median_amount": 3000.00,
                "max_amount": 25000.00,
                "min_amount": 500.00,
                "std_amount": 5000.00,
                "active_days": 30,
                "transactions_per_day": 0.83,
                "cross_border_ratio": 0.6,
                "cash_transaction_ratio": 0.1,
                "amount_consistency": 1.0,
                "large_transaction_ratio": 0.2,
                "unique_receivers": 15,
                "unique_receiver_countries": 5,
                "receiver_diversity": 0.6,
                "night_transaction_ratio": 0.15,
                "weekend_transaction_ratio": 0.2,
                "unique_currencies": 3
            }
        }


class CustomerRiskInput(BaseModel):
    """
    Input schema for customer risk assessment
    Includes customer ID and their aggregated features
    """
    customer_id: str = Field(..., description="Unique customer identifier")
    features: CustomerFeaturesInput = Field(..., description="Customer aggregated features")

    class Config:
        schema_extra = {
            "example": {
                "customer_id": "CUST_123456",
                "features": {
                    "transaction_count": 25,
                    "total_amount": 125000.00,
                    "avg_amount": 5000.00,
                    "median_amount": 3000.00,
                    "max_amount": 25000.00,
                    "min_amount": 500.00,
                    "std_amount": 5000.00,
                    "active_days": 30,
                    "transactions_per_day": 0.83,
                    "cross_border_ratio": 0.6,
                    "cash_transaction_ratio": 0.1,
                    "amount_consistency": 1.0,
                    "large_transaction_ratio": 0.2,
                    "unique_receivers": 15,
                    "unique_receiver_countries": 5,
                    "receiver_diversity": 0.6,
                    "night_transaction_ratio": 0.15,
                    "weekend_transaction_ratio": 0.2,
                    "unique_currencies": 3
                }
            }
        }


class BatchCustomerRiskInput(BaseModel):
    """Input schema for batch customer risk assessment"""
    customers: List[CustomerRiskInput] = Field(..., min_items=1, max_items=500)


class CustomerRiskPrediction(BaseModel):
    """Output schema for customer risk assessment"""
    customer_id: str
    is_high_risk: bool = Field(..., description="Whether customer is flagged as high-risk")
    risk_probability: float = Field(..., ge=0, le=1, description="Probability of high risk (0-1)")
    risk_score: float = Field(..., ge=0, le=100, description="Risk score (0-100)")
    risk_level: str = Field(..., description="Risk level: LOW, MEDIUM, HIGH, CRITICAL")
    confidence: str = Field(..., description="Confidence level: LOW, MEDIUM, HIGH")
    recommendation: str = Field(..., description="Recommended action")
    contributing_factors: Optional[List[str]] = Field(None, description="Key risk factors")


class BatchRiskPredictionResponse(BaseModel):
    """Output schema for batch customer risk predictions"""
    total_customers: int
    high_risk_count: int
    low_risk_count: int
    average_risk_score: float
    predictions: List[CustomerRiskPrediction]
    processing_time_ms: float


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    model_loaded: bool
    scaler_loaded: bool
    timestamp: str
    model_info: Optional[Dict[str, Any]] = None


class ModelInfoResponse(BaseModel):
    """Model information response"""
    model_name: str
    model_type: str
    training_date: str
    performance_metrics: Dict[str, Any]
    training_config: Dict[str, Any]
    feature_names: List[str]
    num_features: int
