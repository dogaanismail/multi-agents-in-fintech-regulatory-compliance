"""
Pydantic schemas for MARL Orchestrator

Author: Ismail Dogan
"""

from pydantic import BaseModel, Field
from typing import Dict, Any, Optional, List
from datetime import datetime
from enum import Enum


class ActionType(str, Enum):
    """Decision action types"""
    BLOCK = "BLOCK"
    ALLOW = "ALLOW"
    REVIEW = "REVIEW"


class TransactionFeatures(BaseModel):
    """Transaction features for Transaction Pattern Agent"""
    Date: str
    Time: str
    From_Bank: str
    Account: str
    To_Bank: str
    Account_1: str
    Amount_Received: float
    Receiving_Currency: str
    Amount_Paid: float
    Payment_Currency: str
    Payment_type: str
    Sender_bank_location: str
    Receiver_bank_location: str


class CustomerFeatures(BaseModel):
    """Customer features for Customer Risk Agent"""
    transaction_count: int
    total_amount: float
    avg_amount: float
    median_amount: float
    max_amount: float
    min_amount: float
    std_amount: float
    active_days: int
    transactions_per_day: float
    cross_border_ratio: float
    cash_transaction_ratio: float
    amount_consistency: float
    large_transaction_ratio: float
    unique_receivers: int
    unique_receiver_countries: int
    receiver_diversity: float
    night_transaction_ratio: float
    weekend_transaction_ratio: float
    unique_currencies: int


class NetworkFeatures(BaseModel):
    """Network topology features for Network Analysis Agent"""
    in_degree: int
    out_degree: int
    degree_centrality: float
    in_degree_centrality: float
    out_degree_centrality: float
    betweenness_centrality: float
    closeness_centrality: float
    pagerank: float
    eigenvector_centrality: float
    clustering_coefficient: float
    community: int


class AgentObservation(BaseModel):
    """Observation from a single detection agent"""
    agent_name: str
    is_suspicious: bool
    probability: float  # fraud_probability or risk_probability
    risk_score: float
    confidence: Optional[str] = None
    response_time_ms: Optional[float] = None


class EnrichedTransactionEvent(BaseModel):
    """Complete enriched transaction event (from Kafka)"""
    transaction_id: str
    customer_id: str
    timestamp: str
    transaction: TransactionFeatures
    customer_profile: CustomerFeatures
    network_topology: NetworkFeatures


class CoordinatedDecisionRequest(BaseModel):
    """Request for coordinated decision (REST API)"""
    transaction: TransactionFeatures
    customer: CustomerFeatures
    network: NetworkFeatures
    transaction_id: Optional[str] = None


class CoordinatedDecisionResponse(BaseModel):
    """Coordinated decision from MADDPG"""
    transaction_id: Optional[str]
    action: ActionType
    confidence: float = Field(..., ge=0, le=1, description="Confidence in decision (0-1)")
    maddpg_q_value: float = Field(..., description="Q-value from centralized critic")
    
    # Agent observations
    transaction_agent_observation: AgentObservation
    customer_agent_observation: AgentObservation
    network_agent_observation: AgentObservation
    
    # Agent contributions (actor outputs)
    agent_contributions: Dict[str, float] = Field(
        ..., 
        description="How much each agent contributed to the decision"
    )
    
    # Metadata
    processing_time_ms: float
    timestamp: str
    mode: str = Field(..., description="inference or training")


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    maddpg_loaded: bool
    detection_agents_status: Dict[str, str]
    timestamp: str


class ModelInfoResponse(BaseModel):
    """MADDPG model information"""
    model_name: str
    version: str
    architecture: Dict[str, Any]
    hyperparameters: Dict[str, Any]
    training_info: Optional[Dict[str, Any]] = None
