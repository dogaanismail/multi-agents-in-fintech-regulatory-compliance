"""
Pydantic schemas for request/response models
"""

from pydantic import BaseModel, Field, validator
from typing import List, Optional, Dict, Any
from datetime import datetime


class TransactionInput(BaseModel):
    """Input schema for a single transaction"""
    Date: str = Field(..., description="Transaction date (YYYY-MM-DD format)")
    Time: str = Field(..., description="Transaction time (HH:MM:SS format)")
    From_Bank: str = Field(..., description="Sender bank name")
    Account: str = Field(..., description="Sender account number")
    To_Bank: str = Field(..., description="Receiver bank name")
    Account_1: str = Field(..., description="Receiver account number")
    Amount_Received: float = Field(..., gt=0, description="Transaction amount")
    Receiving_Currency: str = Field(..., description="Currency code")
    Amount_Paid: float = Field(..., gt=0, description="Amount paid")
    Payment_Currency: str = Field(..., description="Payment currency code")
    Payment_type: str = Field(..., description="Type of payment (e.g., ACH, Wire, Card)")
    Sender_bank_location: str = Field(..., description="Sender bank location/country")
    Receiver_bank_location: str = Field(..., description="Receiver bank location/country")

    class Config:
        schema_extra = {
            "example": {
                "Date": "2024-01-15",
                "Time": "14:30:00",
                "From_Bank": "HSBC Bank",
                "Account": "ACC123456",
                "To_Bank": "Chase Bank",
                "Account_1": "ACC789012",
                "Amount_Received": 15000.50,
                "Receiving_Currency": "USD",
                "Amount_Paid": 15000.50,
                "Payment_Currency": "USD",
                "Payment_type": "Wire",
                "Sender_bank_location": "USA",
                "Receiver_bank_location": "UK"
            }
        }


class BatchTransactionInput(BaseModel):
    """Input schema for batch transaction analysis"""
    transactions: List[TransactionInput] = Field(..., min_items=1, max_items=1000)


class TransactionPrediction(BaseModel):
    """Output schema for a single transaction prediction"""
    transaction_id: Optional[str] = None
    is_suspicious: bool = Field(..., description="Whether transaction is flagged as suspicious")
    fraud_probability: float = Field(..., ge=0, le=1, description="Probability of fraud (0-1)")
    risk_score: float = Field(..., ge=0, le=100, description="Risk score (0-100)")
    confidence: str = Field(..., description="Confidence level: LOW, MEDIUM, HIGH")
    recommendation: str = Field(..., description="Recommended action")
    threshold_used: float = Field(..., description="Decision threshold used")


class BatchPredictionResponse(BaseModel):
    """Output schema for batch predictions"""
    total_transactions: int
    suspicious_count: int
    legitimate_count: int
    average_risk_score: float
    predictions: List[TransactionPrediction]
    processing_time_ms: float


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    model_loaded: bool
    preprocessor_loaded: bool
    timestamp: str
    model_info: Optional[Dict[str, Any]] = None


class ModelInfoResponse(BaseModel):
    """Model information response"""
    model_name: str
    model_type: str
    training_date: str
    dataset: str
    performance_metrics: Dict[str, Any]
    hyperparameters: Dict[str, Any]
    optimal_threshold: float
