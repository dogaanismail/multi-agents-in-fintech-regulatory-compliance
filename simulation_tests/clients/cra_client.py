"""
Customer Risk Agent Client (CRA)
=================================
Port 1002 — XGBoost model, ROC-AUC 0.9051
Endpoint: POST /api/v1/assess-risk  (single customer)
          POST /api/v1/batch-assess-risk (up to 500 customers)

SHAP explanation for CRA:
  The CRA's 'contributing_factors' field is a list of strings derived
  from SHAP TreeExplainer at inference time. Example:
    ["High cross_border_ratio (0.85)", "Many unique_receivers (42)"]
  These are the SHAP top-k features that pushed the risk_probability
  above the decision boundary — i.e. the model is not a black box;
  it tells you *why* this customer is flagged.

  In our simulation we generate realistic synthetic features
  that match the 19 CRA feature schema, calibrated per risk profile:
    - NORMAL: low cash_transaction_ratio, moderate active_days
    - HIGH_RISK: elevated cross_border_ratio, many unique_receivers
    - MULE: high cash_transaction_ratio, extreme cross_border patterns
"""

import random
from typing import Any, Dict, Optional

from simulation_tests.clients.base_client import BaseClient, ClientError
from simulation_tests.config import AgentURLs


def _build_customer_features(risk_profile: str) -> dict:
    """
    Generate the 19 CRA features for a synthetic customer.
    Values are calibrated to match distribution of SAML-D training data.

    Feature names must exactly match CustomerFeaturesInput in
    cra/app/models/schemas.py.
    """
    r = random.Random()  # not seeded per-call — intentional variation

    if risk_profile == "MULE":
        return {
            "transaction_count": r.randint(50, 200),
            "total_amount": round(r.uniform(100_000, 500_000), 2),
            "avg_amount": round(r.uniform(5_000, 15_000), 2),
            "median_amount": round(r.uniform(3_000, 10_000), 2),
            "max_amount": round(r.uniform(30_000, 80_000), 2),
            "min_amount": round(r.uniform(500, 2_000), 2),
            "std_amount": round(r.uniform(8_000, 20_000), 2),
            "active_days": r.randint(5, 20),
            "transactions_per_day": round(r.uniform(5.0, 20.0), 2),
            "cross_border_ratio": round(r.uniform(0.7, 1.0), 3),
            "cash_transaction_ratio": round(r.uniform(0.6, 0.95), 3),
            "amount_consistency": round(r.uniform(0.1, 0.4), 3),
            "large_transaction_ratio": round(r.uniform(0.4, 0.8), 3),
            "unique_receivers": r.randint(30, 100),
            "unique_receiver_countries": r.randint(6, 15),
            "receiver_diversity": round(r.uniform(0.7, 1.0), 3),
            "night_transaction_ratio": round(r.uniform(0.3, 0.7), 3),
            "weekend_transaction_ratio": round(r.uniform(0.3, 0.6), 3),
            "unique_currencies": r.randint(4, 10),
        }
    elif risk_profile == "HIGH_RISK":
        return {
            "transaction_count": r.randint(20, 80),
            "total_amount": round(r.uniform(20_000, 120_000), 2),
            "avg_amount": round(r.uniform(1_000, 5_000), 2),
            "median_amount": round(r.uniform(800, 3_000), 2),
            "max_amount": round(r.uniform(10_000, 30_000), 2),
            "min_amount": round(r.uniform(100, 500), 2),
            "std_amount": round(r.uniform(2_000, 8_000), 2),
            "active_days": r.randint(15, 60),
            "transactions_per_day": round(r.uniform(0.5, 3.0), 2),
            "cross_border_ratio": round(r.uniform(0.3, 0.65), 3),
            "cash_transaction_ratio": round(r.uniform(0.2, 0.5), 3),
            "amount_consistency": round(r.uniform(0.4, 0.7), 3),
            "large_transaction_ratio": round(r.uniform(0.1, 0.35), 3),
            "unique_receivers": r.randint(8, 30),
            "unique_receiver_countries": r.randint(2, 6),
            "receiver_diversity": round(r.uniform(0.4, 0.7), 3),
            "night_transaction_ratio": round(r.uniform(0.1, 0.3), 3),
            "weekend_transaction_ratio": round(r.uniform(0.15, 0.35), 3),
            "unique_currencies": r.randint(2, 4),
        }
    else:  # NORMAL
        return {
            "transaction_count": r.randint(5, 30),
            "total_amount": round(r.uniform(500, 15_000), 2),
            "avg_amount": round(r.uniform(100, 800), 2),
            "median_amount": round(r.uniform(80, 600), 2),
            "max_amount": round(r.uniform(500, 5_000), 2),
            "min_amount": round(r.uniform(10, 100), 2),
            "std_amount": round(r.uniform(50, 500), 2),
            "active_days": r.randint(30, 365),
            "transactions_per_day": round(r.uniform(0.05, 0.5), 2),
            "cross_border_ratio": round(r.uniform(0.0, 0.2), 3),
            "cash_transaction_ratio": round(r.uniform(0.0, 0.15), 3),
            "amount_consistency": round(r.uniform(0.7, 1.0), 3),
            "large_transaction_ratio": round(r.uniform(0.0, 0.05), 3),
            "unique_receivers": r.randint(1, 8),
            "unique_receiver_countries": r.randint(1, 2),
            "receiver_diversity": round(r.uniform(0.1, 0.4), 3),
            "night_transaction_ratio": round(r.uniform(0.0, 0.1), 3),
            "weekend_transaction_ratio": round(r.uniform(0.1, 0.25), 3),
            "unique_currencies": r.randint(1, 2),
        }


class CRAClient(BaseClient):
    """Async client for Customer Risk Agent (port 1002)."""

    def __init__(self):
        super().__init__(base_url=AgentURLs.CRA)

    async def assess_risk(
        self,
        customer_id: str,
        risk_profile: str = "NORMAL",
    ) -> Optional[Dict[str, Any]]:
        """
        POST /api/v1/assess-risk

        Generates synthetic features calibrated to risk_profile,
        then calls the CRA for a live inference.

        Returns the raw prediction dict (includes contributing_factors
        which are the SHAP-derived top features) or None on failure.
        """
        features = _build_customer_features(risk_profile)
        body = {
            "customer_id": customer_id,
            "features": features,
        }
        try:
            return await self.post("/api/v1/assess-risk", body)
        except ClientError:
            return None
        except Exception:
            return None
