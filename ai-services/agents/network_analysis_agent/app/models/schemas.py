"""
Pydantic schemas for Network Analysis Agent request/response models
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime


class AccountFeaturesInput(BaseModel):
    """
    Input schema for account network features
    These are the 10 local ego-net and money-flow features extracted from the
    transaction graph around the account
    """
    unique_in_counterparties: int = Field(..., ge=0, description="Distinct accounts that sent money to this account")
    unique_out_counterparties: int = Field(..., ge=0, description="Distinct accounts this account sent money to")
    reciprocity: float = Field(..., ge=0, le=1,
                               description="Counterparties transacted with in both directions over all counterparties")
    cycle3_count: int = Field(..., ge=0,
                              description="Distinct accounts closing a directed 3-cycle through this account")
    two_hop_out_reach: int = Field(..., ge=0, description="Distinct accounts reachable in exactly two outgoing hops")
    in_out_amount_ratio: float = Field(..., ge=0,
                                       description="Total outgoing amount over total incoming amount plus one")
    in_concentration: float = Field(..., ge=0, le=1,
                                    description="Herfindahl concentration of incoming amounts by counterparty")
    out_concentration: float = Field(..., ge=0, le=1,
                                     description="Herfindahl concentration of outgoing amounts by counterparty")
    forwarding_gap_hours: float = Field(..., ge=0,
                                        description="Median hours between an outgoing payment and the most recent incoming one, capped at 168")
    peak_day_share: float = Field(..., ge=0, le=1, description="Busiest day's share of the account's payments")

    class Config:
        schema_extra = {
            "example": {
                "unique_in_counterparties": 45,
                "unique_out_counterparties": 38,
                "reciprocity": 0.31,
                "cycle3_count": 2,
                "two_hop_out_reach": 140,
                "in_out_amount_ratio": 0.95,
                "in_concentration": 0.21,
                "out_concentration": 0.17,
                "forwarding_gap_hours": 3.5,
                "peak_day_share": 0.4
            }
        }


class AccountRiskInput(BaseModel):
    """
    Input schema for account risk assessment
    Includes account ID and their network topology features
    """
    account_id: str = Field(..., description="Unique account identifier")
    features: AccountFeaturesInput = Field(..., description="Network topology features")

    class Config:
        schema_extra = {
            "example": {
                "account_id": "ACC_789012",
                "features": {
                    "unique_in_counterparties": 45,
                    "unique_out_counterparties": 38,
                    "reciprocity": 0.31,
                    "cycle3_count": 2,
                    "two_hop_out_reach": 140,
                    "in_out_amount_ratio": 0.95,
                    "in_concentration": 0.21,
                    "out_concentration": 0.17,
                    "forwarding_gap_hours": 3.5,
                    "peak_day_share": 0.4
                }
            }
        }


class BatchAccountRiskInput(BaseModel):
    """Input schema for batch account risk assessment"""
    accounts: List[AccountRiskInput] = Field(..., min_items=1, max_items=500)


class FeatureContribution(BaseModel):
    """One feature's signed SHAP contribution to the fraud-class score."""
    feature: str
    value: str
    shap_value: float
    direction: str


class AccountRiskPrediction(BaseModel):
    """Output schema for account risk assessment"""
    account_id: str
    is_suspicious: bool = Field(..., description="Whether account is flagged as suspicious")
    suspicion_probability: float = Field(..., ge=0, le=1, description="Probability of suspicious activity (0-1)")
    risk_score: float = Field(..., ge=0, le=100, description="Risk score (0-100)")
    risk_level: str = Field(..., description="Risk level: LOW, MEDIUM, HIGH, CRITICAL")
    confidence: str = Field(..., description="Confidence level: LOW, MEDIUM, HIGH")
    recommendation: str = Field(..., description="Recommended action")
    network_indicators: Optional[Dict[str, Any]] = Field(None, description="Key network topology indicators")
    feature_contributions: Optional[List[FeatureContribution]] = None
    shap_base_value: Optional[float] = None


class BatchRiskPredictionResponse(BaseModel):
    """Output schema for batch account risk predictions"""
    total_accounts: int
    suspicious_count: int
    normal_count: int
    average_risk_score: float
    predictions: List[AccountRiskPrediction]
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
    network_stats: Dict[str, Any]
