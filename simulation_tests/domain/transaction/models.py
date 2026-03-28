"""
Transaction Domain Models
=========================
Represents a payment to fire via payment-svc and all the downstream
risk signals collected from the AI agents.

FraudLabel is the *ground truth* we seed — used to compute TP/FP/TN/FN
after the simulation.
"""

from dataclasses import dataclass, field
from typing import Optional, Dict, Any
from enum import Enum
import time


class FraudLabel(str, Enum):
    """Ground-truth label we assign before sending the payment."""
    LEGITIMATE         = "LEGITIMATE"
    SMURFING           = "SMURFING"          # many small transfers, same origin
    FAN_OUT            = "FAN_OUT"           # one sender → many receivers
    LAYERING           = "LAYERING"          # cross-border chains GB→TR→AE
    HIGH_VALUE_SINGLE  = "HIGH_VALUE_SINGLE" # single >$50k transfer (TPA amount signal)
    ROUND_TRIP         = "ROUND_TRIP"        # A→B then B→A (NAA cycle detection)


@dataclass
class PaymentRequestData:
    """
    Mirrors Java PaymentRequest.
    Used to POST /api/v1/payments/request.
    """
    customer_id: str
    amount: float
    from_currency: str
    to_currency: str
    payment_type: str              # "TRANSFER_IN" | "TRANSFER_OUT" | "WITHDRAWAL"
    source_account_id: Optional[str] = None
    destination_account_id: Optional[str] = None
    description: Optional[str] = None

    def to_api_payload(self) -> dict:
        payload: dict = {
            "customerId": self.customer_id,
            "amount": self.amount,
            "fromCurrency": self.from_currency,
            "toCurrency": self.to_currency,
            "paymentType": self.payment_type,
        }
        if self.source_account_id:
            payload["sourceAccountId"] = self.source_account_id
        if self.destination_account_id:
            payload["destinationAccountId"] = self.destination_account_id
        if self.description:
            payload["description"] = self.description
        return payload


@dataclass
class AgentSignals:
    """
    Risk signals returned by the three AI agents for one transaction.

    What is SHAP?
    -------------
    SHAP (SHapley Additive exPlanations) is a mathematical framework that
    explains *why* an ML model gave a particular prediction score.
    For each prediction, SHAP assigns each input feature a contribution value
    (+ = pushed score higher, − = pushed score lower).
    Example: for TPA, SHAP says "Amount contributed +0.058 to the fraud
    probability, while Receiver_GB contributed +0.039."
    The `contributing_factors` below are the human-readable SHAP top features
    returned by each agent in its recommendation/contributing_factors field.
    """

    # --- TPA (Transaction Pattern Agent) ---
    tpa_fraud_probability: Optional[float] = None   # 0.0–1.0
    tpa_risk_score: Optional[float] = None          # 0–100
    tpa_is_suspicious: Optional[bool] = None
    tpa_recommendation: Optional[str] = None
    tpa_confidence: Optional[str] = None
    tpa_threshold_used: Optional[float] = None      # should be ~0.1322

    # --- CRA (Customer Risk Agent) ---
    cra_risk_probability: Optional[float] = None
    cra_risk_score: Optional[float] = None
    cra_risk_level: Optional[str] = None            # LOW/MEDIUM/HIGH/CRITICAL
    cra_is_high_risk: Optional[bool] = None
    cra_contributing_factors: Optional[list] = None  # SHAP-derived top features

    # --- NAA (Network Analysis Agent) ---
    naa_suspicion_probability: Optional[float] = None
    naa_risk_score: Optional[float] = None
    naa_risk_level: Optional[str] = None
    naa_is_suspicious: Optional[bool] = None
    naa_network_indicators: Optional[Dict[str, Any]] = None  # centrality metrics

    # --- MARL Orchestrator (MADDPG coordinated decision) ---
    orchestrator_action: Optional[str] = None       # APPROVE/FLAG/BLOCK
    orchestrator_confidence: Optional[float] = None
    orchestrator_agent_contributions: Optional[Dict[str, Any]] = None
    orchestrator_raw_response: Optional[Dict[str, Any]] = None

    @property
    def is_flagged(self) -> bool:
        """True if any agent or orchestrator flagged this payment."""
        return any([
            self.tpa_is_suspicious,
            self.cra_is_high_risk,
            self.naa_is_suspicious,
            self.orchestrator_action in ("FLAG", "BLOCK"),
        ])


@dataclass
class TransactionRecord:
    """
    Complete runtime record for one simulated payment.

    Lifecycle:
      1. Created by TransactionFactory (has payment_data + fraud_label)
      2. Enriched by SimulationRunner (adds timing + API response)
      3. Enriched by collectors (adds agent_signals)
      4. Read by ResultAggregator to build the final CSV/JSON
    """
    payment_data: PaymentRequestData
    fraud_label: FraudLabel
    scenario_index: int           # 0-based position in scenario list

    # --- Timing ---
    send_timestamp_ms: Optional[float] = None    # epoch ms when request sent
    response_timestamp_ms: Optional[float] = None
    latency_ms: Optional[float] = None

    # --- Payment API response ---
    payment_id: Optional[str] = None            # UUID from payment-svc
    payment_api_status: Optional[int] = None    # HTTP status code
    payment_api_error: Optional[str] = None

    # --- AI signals ---
    agent_signals: AgentSignals = field(default_factory=AgentSignals)

    # --- Derived after collection ---
    detection_result: Optional[str] = None  # TP / FP / TN / FN

    def finalise_detection(self) -> None:
        """
        Compute TP/FP/TN/FN based on ground-truth label vs agent verdict.
        Called by ResultAggregator after all signals are collected.
        """
        predicted_fraud = self.agent_signals.is_flagged
        actual_fraud = self.fraud_label != FraudLabel.LEGITIMATE

        if actual_fraud and predicted_fraud:
            self.detection_result = "TP"
        elif actual_fraud and not predicted_fraud:
            self.detection_result = "FN"
        elif not actual_fraud and predicted_fraud:
            self.detection_result = "FP"
        else:
            self.detection_result = "TN"
