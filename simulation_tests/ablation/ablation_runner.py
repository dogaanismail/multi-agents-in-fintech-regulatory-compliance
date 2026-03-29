"""
Ablation Runner
===============
Direct per-agent benchmarking — bypasses payment-svc and Kafka completely.

Use this for standalone Chapter 5 evaluation:
  - Test each AI agent (TPA / CRA / NAA) in isolation
  - Get exact SHAP values per prediction (not just contributing_factors)
  - Validate that each agent gives sensible responses independently
  - Useful when you want to run agent evaluation WITHOUT all microservices up

This does NOT replace the main simulation — it is a COMPLEMENTARY ablation
study showing each agent's behaviour in isolation vs the coordinated system.

How to use:
    python -m simulation_tests.ablation.ablation_runner

or in code:
    from simulation_tests.ablation.ablation_runner import AblationRunner
    results = await AblationRunner().run_tpa_batch(transactions)
"""

import asyncio
import logging
from typing import List, Optional

from simulation_tests.clients.tpa_client import TPAClient
from simulation_tests.clients.cra_client import CRAClient
from simulation_tests.clients.naa_client import NAAClient
from simulation_tests.clients.orchestrator_client import OrchestratorClient

logger = logging.getLogger(__name__)


class AblationRunner:
    """
    Runs each agent independently for ablation / sensitivity analysis.

    Usage (async):
        runner = AblationRunner()
        await runner.check_all_agents_healthy()
    """

    async def check_all_agents_healthy(self) -> dict:
        """Ping all four AI agent health endpoints. Returns status dict."""
        results = {}
        agent_clients = [
            ("TPA (Transaction Pattern Agent)",  TPAClient()),
            ("CRA (Customer Risk Agent)",        CRAClient()),
            ("NAA (Network Analysis Agent)",     NAAClient()),
            ("Orchestrator (MADDPG)",            OrchestratorClient()),
        ]
        for name, client in agent_clients:
            async with client:
                health = await client.health()
            status = health.get("status", "unknown")
            results[name] = status
            icon = "✓" if "ok" in status.lower() or "healthy" in status.lower() else "✗"
            logger.info("%s  %s → %s", icon, name, status)
        return results

    async def run_tpa_single(self, transaction_features: dict) -> Optional[dict]:
        """
        Call TPA directly for a single transaction.
        Returns the prediction dict with fraud_probability and risk_score.
        """
        async with TPAClient() as client:
            return await client.predict_single(transaction_features)

    async def run_cra_single(self, customer_features: dict) -> Optional[dict]:
        """
        Call CRA directly for a single customer.
        Returns the risk assessment with contributing_factors (SHAP-derived).

        SHAP reminder:
          contributing_factors are the human-readable top SHAP features —
          e.g. ['active_days (high)', 'cash_transaction_ratio (elevated)']
          Each factor tells you WHY the customer was flagged as high-risk.
        """
        async with CRAClient() as client:
            return await client.assess_risk(customer_features)

    async def run_naa_single(self, network_features: dict) -> Optional[dict]:
        """
        Call NAA directly for a single account's network topology features.
        Returns suspicion_probability and network_indicators (centrality metrics).
        """
        async with NAAClient() as client:
            return await client.predict_single(network_features)


async def _demo():
    """Quick smoke test — calls health on all agents."""
    logging.basicConfig(level=logging.INFO, format="%(levelname)s  %(message)s")
    runner = AblationRunner()
    health = await runner.check_all_agents_healthy()
    print("\nAgent health summary:")
    for name, status in health.items():
        print(f"  {name}: {status}")


if __name__ == "__main__":
    asyncio.run(_demo())
