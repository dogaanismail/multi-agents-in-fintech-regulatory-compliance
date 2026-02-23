"""
Inference schemas — Pydantic models for the MARL Orchestrator REST API.

Covers: agent observations, transaction/customer/network features,
coordinated decision request/response, health and model info.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class ActionType(str, Enum):
    """Decision action types."""
    BLOCK = "BLOCK"
    ALLOW = "ALLOW"
    REVIEW = "REVIEW"


# TODO: Refactor schemas in marl orchestrator
class TransactionFeatures(BaseModel):
    """Transaction features for Transaction Pattern Agent."""
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
    """Customer features for Customer Risk Agent."""
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
    """Network topology features for Network Analysis Agent."""
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
    """Observation returned by a single detection agent."""
    agent_name: str
    is_suspicious: bool
    probability: float
    risk_score: float
    confidence: Optional[str] = None
    response_time_ms: Optional[float] = None


class EnrichedTransactionEvent(BaseModel):
    """Complete enriched transaction event (consumed from Kafka)."""
    transaction_id: str
    customer_id: str
    timestamp: str
    transaction: TransactionFeatures
    customer_profile: CustomerFeatures
    network_topology: NetworkFeatures


class CoordinatedDecisionRequest(BaseModel):
    """REST API request for a coordinated MADDPG decision."""
    transaction: TransactionFeatures
    customer: CustomerFeatures
    network: NetworkFeatures
    payment_id: str = Field(..., description="Payment identifier")


class CoordinatedDecisionResponse(BaseModel):
    """Coordinated MADDPG decision response."""
    payment_id: str = Field(..., description="Payment identifier")
    action: ActionType
    confidence: float = Field(..., ge=0, le=1, description="Decision confidence (0-1)")
    maddpg_q_value: float = Field(..., description="Q-value from centralised critic")

    transaction_agent_observation: AgentObservation
    customer_agent_observation: AgentObservation
    network_agent_observation: AgentObservation

    agent_contributions: Dict[str, float] = Field(
        ..., description="Per-agent contribution to the final decision"
    )

    processing_time_ms: float
    timestamp: str
    mode: str = Field(..., description="inference | training")


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    maddpg_loaded: bool
    detection_agents_status: Dict[str, str]
    timestamp: str
    # Configuration-service integration
    configuration_service_healthy: bool = False
    dynamic_config_last_refreshed: Optional[str] = None
    dynamic_config_cache_size: int = 0
    # Compliance controls
    freeze_training_active: bool = False


class ModelInfoResponse(BaseModel):
    """MADDPG model information."""
    model_name: str
    version: str
    architecture: Dict[str, Any]
    hyperparameters: Dict[str, Any]
    training_info: Optional[Dict[str, Any]] = None
