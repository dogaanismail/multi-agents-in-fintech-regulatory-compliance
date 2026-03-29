"""
MARL Orchestrator Client
=========================
Port 1004 — MADDPG coordinated decision engine
Endpoint: POST /api/v1/predict

The orchestrator is the crown jewel of the system:
  1. Receives transaction, customer, and network features
  2. Queries all 3 agents in parallel internally
  3. Converts agent observations → MADDPG state vector
  4. MADDPG Actor networks output coordinated action: APPROVE / FLAG / BLOCK
  5. Returns action + confidence + per-agent contributions

In the simulation we call the orchestrator *after* getting individual
agent responses, so we can compare the orchestrator's coordinated
action against the individual agent verdicts. This directly tests
whether MADDPG improves precision over single-agent decisions.
"""

from typing import Any, Dict, Optional

from simulation_tests.clients.base_client import BaseClient, ClientError
from simulation_tests.config import AgentURLs


class OrchestratorClient(BaseClient):
    """Async client for MARL Orchestrator / MADDPG (port 1004)."""

    def __init__(self):
        super().__init__(base_url=AgentURLs.ORCHESTRATOR)

    async def predict(
        self,
        payment_id: str,
        transaction_features: dict,
        customer_features: dict,
        network_features: dict,
    ) -> Optional[Dict[str, Any]]:
        """
        POST /api/v1/predict  (CoordinatedDecisionRequest)

        Parameters mirror the orchestrator's CoordinatedDecisionRequest:
          payment_id           : str
          transaction (dict)   : all TransactionInput fields
          customer (dict)      : all CustomerFeaturesInput fields (19)
          network (dict)       : all AccountFeaturesInput fields (11)

        Returns the CoordinatedDecisionResponse dict or None on failure.
        Expected response fields:
          action              : "APPROVE" | "FLAG" | "BLOCK"
          confidence          : float 0–1
          agent_contributions : {tpa: ..., cra: ..., naa: ...}
        """
        body = {
            "payment_id": payment_id,
            "transaction": transaction_features,
            "customer": customer_features,
            "network": network_features,
        }
        try:
            return await self.post("/api/v1/predict", body)
        except ClientError:
            return None
        except Exception:
            return None
