"""
Simulation Configuration
========================
Central configuration for all service endpoints, simulation parameters,
and scenario settings. Edit these values to target different environments.
"""

from dataclasses import dataclass, field
from typing import List


# ---------------------------------------------------------------------------
# Service Base URLs
# ---------------------------------------------------------------------------

class ServiceURLs:
    """Backend Java/Spring Boot services."""
    CUSTOMER_SVC    = "http://localhost:5001"
    ACCOUNT_SVC     = "http://localhost:5002"
    PAYMENT_SVC     = "http://localhost:5003"
    PAYMENT_ENGINE  = "http://localhost:5004"
    PAYMENT_HISTORY = "http://localhost:5005"
    RISK_ENGINE     = "http://localhost:5006"
    NETWORK_TOPOLOGY= "http://localhost:5007"
    CUSTOMER_PROFILE= "http://localhost:5008"
    CONFIGURATION   = "http://localhost:5009"

    # React backoffice gateway
    BACKOFFICE_GATEWAY = "http://localhost:3030"


class AgentURLs:
    """Python AI Agent services."""
    TPA         = "http://localhost:1001"   # Transaction Pattern Agent
    CRA         = "http://localhost:1002"   # Customer Risk Agent
    NAA         = "http://localhost:1003"   # Network Analysis Agent
    ORCHESTRATOR= "http://localhost:1004"   # MARL Orchestrator (MADDPG)


# ---------------------------------------------------------------------------
# API Path Constants
# ---------------------------------------------------------------------------

class CustomerPaths:
    CREATE      = "/api/v1/customers"
    GET_BY_ID   = "/api/v1/customers/{id}"
    LIST_ALL    = "/api/v1/customers"


class AccountPaths:
    OPEN        = "/api/v1/accounts/open-account"
    GET_BY_ID   = "/api/v1/accounts/{id}"
    BY_CUSTOMER = "/api/v1/accounts/customer/{customerId}"
    BALANCES    = "/api/v1/accounts/{id}/balances"


class PaymentPaths:
    REQUEST     = "/api/v1/payments/request"
    BY_CUSTOMER = "/api/v1/payments/customer/{customerId}"


class TPAPaths:
    PREDICT_SINGLE  = "/api/v1/predict"
    PREDICT_BATCH   = "/api/v1/batch-predict"
    HEALTH          = "/health"


class CRAPaths:
    ASSESS_SINGLE   = "/api/v1/assess-risk"
    ASSESS_BATCH    = "/api/v1/batch-assess-risk"
    HEALTH          = "/health"


class NAAPaths:
    PREDICT_SINGLE  = "/api/v1/predict"
    PREDICT_BATCH   = "/api/v1/predict/batch"
    HEALTH          = "/health"


class OrchestratorPaths:
    PREDICT         = "/api/v1/predict"
    HEALTH          = "/health"


# ---------------------------------------------------------------------------
# Simulation Scenario Parameters
# ---------------------------------------------------------------------------

@dataclass
class SimulationConfig:
    """Main scenario parameters — all counts and ratios in one place."""

    # --- Population ---
    num_customers: int = 1_000     # synthetic customers to create
    accounts_per_customer: int = 1  # accounts opened per customer
    num_payments: int = 10_000     # total payments to fire concurrently

    # --- Fraud seed distribution ---
    # Must sum to num_payments
    # 10% fraud rate across 5 typologies for rich MARL experience buffer
    legitimate_count: int          = 9_000  # 90%  clean payments
    fraud_smurfing_count: int      = 300    # 3%   many small transfers, same origin
    fraud_fanout_count: int        = 250    # 2.5% one sender → many receivers
    fraud_layering_count: int      = 250    # 2.5% cross-border chain (GB→TR→AE)
    fraud_high_value_count: int    = 100    # 1%   single >$50k transfer
    fraud_round_trip_count: int    = 100    # 1%   A→B then B→A cycle (pairs = 50)

    # --- Concurrency ---
    # Max simultaneous HTTP connections in the async runner
    max_concurrency: int = 100
    request_timeout_seconds: int = 30

    # --- Kafka drain wait ---
    # Time to wait after all payments are fired before polling payment-history.
    # The Kafka pipeline (payment-svc → risk-engine → TPA/CRA/NAA/MARL →
    # payment-history) needs time to drain the burst.
    # Rule of thumb: ~1s per 80 payments, minimum 15s.
    # Override with --drain-wait CLI flag on run_simulation.py.
    kafka_drain_wait_seconds: int = 120  # 2 min for 10k run; set to 15 for smoke test

    # --- Risk thresholds (from notebook evaluation) ---
    tpa_threshold: float = 0.1322  # optimal threshold from TPA evaluation
    cra_high_risk_threshold: float = 0.5
    naa_suspicious_threshold: float = 0.5

    # --- Output paths ---
    results_csv: str = "simulation_tests/reports/results.csv"
    summary_json: str = "simulation_tests/reports/summary.json"
    charts_dir: str = "simulation_tests/reports/charts"

    # --- Random seed for reproducibility ---
    random_seed: int = 42


# ---------------------------------------------------------------------------
# Supported Enum Values (mirrors Java enums and Python Pydantic schemas)
# ---------------------------------------------------------------------------

CURRENCIES       = ["USD", "EUR", "GBP", "TRY", "CHF", "JPY", "INR", "PKR",
                    "MAD", "NGN", "AED", "MXN"]
PAYMENT_TYPES    = ["TRANSFER_OUT"]  # WITHDRAWAL/DEPOSIT excluded — require ledger account
ACCOUNT_TYPES    = ["CHECKING", "SAVINGS", "BUSINESS"]
BANK_LOCATIONS   = ["US", "GB", "DE", "FR", "TR", "JP", "IN", "CH",
                    "NL", "IT", "ES", "AE", "MX", "NG", "PK", "MA"]
CUSTOMER_TYPES   = ["INDIVIDUAL", "BUSINESS"]

# High-risk country pairs used for cross-border layering fraud patterns
HIGH_RISK_CORRIDORS = [
    ("GB", "TR"),   # UK → Turkey
    ("TR", "AE"),   # Turkey → UAE
    ("DE", "PK"),   # Germany → Pakistan
    ("US", "MX"),   # USA → Mexico
    ("FR", "MA"),   # France → Morocco
    ("NL", "NG"),   # Netherlands → Nigeria
]


# Singleton — import this in all modules
SIM_CONFIG = SimulationConfig()
