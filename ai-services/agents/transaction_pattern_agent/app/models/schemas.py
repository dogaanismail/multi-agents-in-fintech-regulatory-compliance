"""
Pydantic schemas for request/response models
"""

from pydantic import BaseModel, Field, validator
from typing import List, Optional, Dict, Any

class TransactionInput(BaseModel):
    """
    Input schema for a single transaction - aligned with Avro FraudAnalysisRequestedEvent.transactionFeatures
    
    Note: 
    - Avro sends additional fields (paymentId, senderAccount, receiverAccount) which are ignored
    - Model only uses: date, time, amount, currencies, locations, paymentType
    - Account numbers are NOT used for prediction (model drops them during training)
    - ISO codes are validated by backend and Avro schema before reaching this service
    """
    date: str = Field(..., description="Transaction date (YYYY-MM-DD format)")
    time: str = Field(..., description="Transaction time (HH:MM:SS format)")
    amount: float = Field(..., gt=0, description="Transaction amount")
    paymentCurrency: str = Field(..., description="Payment currency code (ISO 4217, 3-letter: USD, EUR, GBP, etc.)")
    receivedCurrency: str = Field(..., description="Received currency code (ISO 4217, 3-letter: USD, EUR, GBP, etc.)")
    senderBankLocation: str = Field(..., description="Sender bank country (ISO 3166-1 alpha-2, 2-letter: US, GB, TR, etc.)")
    receiverBankLocation: str = Field(..., description="Receiver bank country (ISO 3166-1 alpha-2, 2-letter: US, GB, TR, etc.)")
    paymentType: str = Field(..., description="Type of payment (ACH, Cash Deposit, Cheque, Credit card, Debit card, Cross-border)")

    class Config:
        extra = "ignore"
        schema_extra = {
            "example": {
                "date": "2024-01-15",
                "time": "14:30:00",
                "amount": 15000.50,
                "paymentCurrency": "USD",
                "receivedCurrency": "USD",
                "senderBankLocation": "US",
                "receiverBankLocation": "GB",
                "paymentType": "Wire"
            }
        }

class BatchTransactionInput(BaseModel):
    """Input schema for batch transaction analysis"""
    transactions: List[TransactionInput] = Field(..., min_items=1, max_items=1000)


class TransactionPrediction(BaseModel):
    """Output schema for a single transaction prediction"""
    payment_id: Optional[str] = None
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
