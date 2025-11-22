"""
Pydantic schemas for Network Analysis Agent request/response models
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime


class AccountFeaturesInput(BaseModel):
    """
    Input schema for account network topology features
    These are the 11 topology features extracted from the transaction network
    """
    in_degree: int = Field(..., ge=0, description="Number of incoming connections")
    out_degree: int = Field(..., ge=0, description="Number of outgoing connections")
    degree_centrality: float = Field(..., ge=0, le=1, description="Degree centrality (normalized)")
    in_degree_centrality: float = Field(..., ge=0, le=1, description="In-degree centrality")
    out_degree_centrality: float = Field(..., ge=0, le=1, description="Out-degree centrality")
    betweenness_centrality: float = Field(..., ge=0, le=1, description="Betweenness centrality")
    closeness_centrality: float = Field(..., ge=0, le=1, description="Closeness centrality")
    pagerank: float = Field(..., ge=0, description="PageRank score")
    eigenvector_centrality: float = Field(..., ge=0, description="Eigenvector centrality")
    clustering_coefficient: float = Field(..., ge=0, le=1, description="Clustering coefficient")
    community: int = Field(..., ge=0, description="Community ID")

    class Config:
        schema_extra = {
            "example": {
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
            }
        }


class BatchAccountRiskInput(BaseModel):
    """Input schema for batch account risk assessment"""
    accounts: List[AccountRiskInput] = Field(..., min_items=1, max_items=500)


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
