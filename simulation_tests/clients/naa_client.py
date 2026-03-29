"""
Network Analysis Agent Client (NAA)
=====================================
Port 1003 — CatBoost model, ROC-AUC 0.8466
Endpoint: POST /api/v1/predict  (single account)

SHAP explanation for NAA:
  SHAP TreeExplainer is used on the CatBoost model.
  Top contributing features per prediction:
    pagerank (mean |SHAP| = 0.815)       — most important
    eigenvector_centrality (0.657)       — influence score
    community (0.520)                    — cluster membership

  The 'network_indicators' field in the response includes the raw
  centrality values, which are themselves proportional to SHAP values.
  High pagerank + high eigenvector_centrality → near-certain flag.

Feature generation strategy per account type:
  NORMAL account:   low in/out degree, low pagerank, isolated community
  HIGH_RISK account: elevated betweenness/closeness, moderate pagerank
  MULE account:     high out_degree (fan-out), high pagerank, bridges
"""

import math
import random
from typing import Any, Dict, Optional

from simulation_tests.clients.base_client import BaseClient, ClientError
from simulation_tests.config import AgentURLs


def _build_network_features(is_fraud_seed: bool, risk_profile: str) -> dict:
    """
    Generate the 11 NAA graph topology features for a synthetic account.

    Feature names must match AccountFeaturesInput in
    naa/app/models/schemas.py exactly.
    """
    r = random.Random()

    if is_fraud_seed:
        # Mule: high out-degree (fan-out sender), high pagerank
        in_deg  = r.randint(5, 30)
        out_deg = r.randint(40, 120)
        total   = in_deg + out_deg
        # Normalised centralities
        n = 10_000  # estimated network size
        return {
            "in_degree": in_deg,
            "out_degree": out_deg,
            "degree_centrality": round(total / n, 6),
            "in_degree_centrality": round(in_deg / n, 6),
            "out_degree_centrality": round(out_deg / n, 6),
            "betweenness_centrality": round(r.uniform(0.01, 0.05), 6),
            "closeness_centrality": round(r.uniform(0.4, 0.7), 6),
            "pagerank": round(r.uniform(0.005, 0.02), 8),      # high
            "eigenvector_centrality": round(r.uniform(0.05, 0.3), 6),  # high
            "clustering_coefficient": round(r.uniform(0.0, 0.05), 6),
            "community": r.randint(0, 20),
        }
    elif risk_profile == "HIGH_RISK":
        in_deg  = r.randint(10, 50)
        out_deg = r.randint(10, 40)
        total   = in_deg + out_deg
        n = 10_000
        return {
            "in_degree": in_deg,
            "out_degree": out_deg,
            "degree_centrality": round(total / n, 6),
            "in_degree_centrality": round(in_deg / n, 6),
            "out_degree_centrality": round(out_deg / n, 6),
            "betweenness_centrality": round(r.uniform(0.001, 0.01), 6),
            "closeness_centrality": round(r.uniform(0.3, 0.5), 6),
            "pagerank": round(r.uniform(0.0005, 0.003), 8),
            "eigenvector_centrality": round(r.uniform(0.01, 0.06), 6),
            "clustering_coefficient": round(r.uniform(0.05, 0.2), 6),
            "community": r.randint(0, 50),
        }
    else:  # NORMAL
        in_deg  = r.randint(1, 10)
        out_deg = r.randint(1, 10)
        total   = in_deg + out_deg
        n = 10_000
        return {
            "in_degree": in_deg,
            "out_degree": out_deg,
            "degree_centrality": round(total / n, 6),
            "in_degree_centrality": round(in_deg / n, 6),
            "out_degree_centrality": round(out_deg / n, 6),
            "betweenness_centrality": round(r.uniform(0.0, 0.001), 6),
            "closeness_centrality": round(r.uniform(0.1, 0.35), 6),
            "pagerank": round(r.uniform(0.00005, 0.0005), 8),
            "eigenvector_centrality": round(r.uniform(0.001, 0.01), 6),
            "clustering_coefficient": round(r.uniform(0.1, 0.5), 6),
            "community": r.randint(0, 200),
        }


class NAAClient(BaseClient):
    """Async client for Network Analysis Agent (port 1003)."""

    def __init__(self):
        super().__init__(base_url=AgentURLs.NAA)

    async def predict(
        self,
        account_id: str,
        is_fraud_seed: bool = False,
        risk_profile: str = "NORMAL",
    ) -> Optional[Dict[str, Any]]:
        """
        POST /api/v1/predict

        Generates synthetic network topology features calibrated to the
        account's risk profile, then calls NAA for live inference.

        Returns raw prediction dict (includes network_indicators mapping
        centrality metrics — these are the SHAP-weighted features)
        or None on failure.
        """
        features = _build_network_features(is_fraud_seed, risk_profile)
        body = {
            "account_id": account_id,
            "features": features,
        }
        try:
            return await self.post("/api/v1/predict", body)
        except ClientError:
            return None
        except Exception:
            return None
