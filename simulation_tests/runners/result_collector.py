"""
Result Collector
================
Phase 3: after all payments are fired, poll payment-history-svc
to collect the final risk verdict for each payment_id.

Why poll?
  payment-svc returns HTTP 201 immediately — the risk decision is
  made ASYNCHRONOUSLY by risk-engine-svc via Kafka. The chain is:

    payment-svc → [Kafka: PaymentCreatedEvent]
      → risk-engine-svc → TPA + CRA + NAA + MADDPG
      → [Kafka: RiskAssessmentCompletedEvent]
      → payment-history-svc stores final enriched record

  On local Docker, this pipeline completes in ~1–4 seconds per payment.
  We poll with retries so slower payments aren't missed.

What we extract from payment-history-svc response:
  - riskScore, riskLevel, riskStatus  (from risk-engine verdict)
  - agentScores (TPA / CRA / NAA individual scores)
  - marlDecision (MADDPG coordinated action)
  - processingTimeMs (end-to-end Kafka latency from payment to verdict)

These fields are mapped onto TransactionRecord.agent_signals so the
aggregator can compute TP/FP/TN/FN and latency statistics.
"""

import asyncio
import logging
from typing import List, Optional

from simulation_tests.clients.payment_history_client import PaymentHistoryClient
from simulation_tests.config import SIM_CONFIG
from simulation_tests.domain.transaction.models import AgentSignals, TransactionRecord

logger = logging.getLogger(__name__)


def _map_history_response(record: TransactionRecord, history: dict) -> None:
    """
    Parse the payment-history-svc JSON response and populate
    record.agent_signals with whatever fields are available.

    Actual response structure from risk-engine-svc:
      {
        "riskScore": float,        "riskLevel": str,
        "riskAction": str,         "fraudStatus": str,
        "fraudIndicators": [...],
        "marlAssessment": {
          "action": str,           "confidence": float,
          "maddpgQValue": float,   "agentContributions": {...},
          "transactionAgentObservation": { "isSuspicious": bool, "probability": float,
                                           "riskScore": float, "confidence": str },
          "customerAgentObservation":    { ... },
          "networkAgentObservation":     { ... }
        }
      }
    """
    sigs = record.agent_signals

    # ---- Orchestrator / MARL — root of all per-agent data ----
    marl = history.get("marlAssessment") or history.get("marlDecision") or {}
    if isinstance(marl, dict):
        action = marl.get("action") or marl.get("decision")
        if not action:
            action = (history.get("riskAction") or history.get("riskStatus") or "").upper() or None
        sigs.orchestrator_action              = action
        sigs.orchestrator_confidence          = marl.get("confidence")
        sigs.orchestrator_agent_contributions = marl.get("agentContributions")
        sigs.orchestrator_raw_response        = marl

    # ---- TPA — transactionAgentObservation ----
    tpa = marl.get("transactionAgentObservation") or {} if isinstance(marl, dict) else {}
    if isinstance(tpa, dict) and tpa:
        sigs.tpa_fraud_probability = tpa.get("probability")
        sigs.tpa_risk_score        = tpa.get("riskScore")
        sigs.tpa_is_suspicious     = tpa.get("isSuspicious")
        sigs.tpa_confidence        = tpa.get("confidence")

    # ---- CRA — customerAgentObservation ----
    cra = marl.get("customerAgentObservation") or {} if isinstance(marl, dict) else {}
    if isinstance(cra, dict) and cra:
        sigs.cra_risk_probability     = cra.get("probability")
        sigs.cra_risk_score           = cra.get("riskScore")
        sigs.cra_risk_level           = cra.get("confidence")
        sigs.cra_is_high_risk         = cra.get("isSuspicious")
        sigs.cra_contributing_factors = history.get("fraudIndicators") or []

    # ---- NAA — networkAgentObservation ----
    naa = marl.get("networkAgentObservation") or {} if isinstance(marl, dict) else {}
    if isinstance(naa, dict) and naa:
        sigs.naa_suspicion_probability = naa.get("probability")
        sigs.naa_risk_score            = naa.get("riskScore")
        sigs.naa_risk_level            = naa.get("confidence")
        sigs.naa_is_suspicious         = naa.get("isSuspicious")
        sigs.naa_network_indicators    = marl.get("agentContributions") if isinstance(marl, dict) else {}


async def _collect_single(
    client: PaymentHistoryClient,
    record: TransactionRecord,
    semaphore: asyncio.Semaphore,
) -> None:
    """Poll for one payment's history record and map the results."""
    if not record.payment_id:
        logger.debug(
            "[%d] No payment_id — skipping result collection (payment failed to submit)",
            record.scenario_index,
        )
        return

    async with semaphore:
        history = await client.poll_until_ready(record.payment_id)
        if history:
            _map_history_response(record, history)
            record.finalise_detection()
            logger.debug(
                "[%d] %s → label=%-12s  detection=%s  orchestrator=%s",
                record.scenario_index,
                record.payment_id[:8],
                record.fraud_label.value,
                record.detection_result,
                record.agent_signals.orchestrator_action,
            )
        else:
            logger.warning(
                "[%d] payment_id=%s timed out — result will be UNKNOWN",
                record.scenario_index, record.payment_id,
            )


class ResultCollector:
    """
    Polls payment-history-svc for every transaction that received a payment_id.

    Usage (async):
        collector = ResultCollector()
        await collector.collect(transactions)
        # transactions[*].agent_signals and detection_result are now populated
    """

    def __init__(self):
        self._cfg = SIM_CONFIG

    async def collect(self, transactions: List[TransactionRecord]) -> None:
        """
        Concurrently poll all transactions. Modifies `transactions` in-place.
        """
        with_id = [t for t in transactions if t.payment_id]
        without_id = len(transactions) - len(with_id)

        logger.info(
            "Collecting results for %d transactions "
            "(%d skipped — no payment_id)...",
            len(with_id), without_id,
        )

        semaphore = asyncio.Semaphore(self._cfg.max_concurrency)

        async with PaymentHistoryClient() as client:
            tasks = [
                asyncio.create_task(
                    _collect_single(client, record, semaphore)
                )
                for record in with_id
            ]
            await asyncio.gather(*tasks)

        # Summary
        tp = sum(1 for t in transactions if t.detection_result == "TP")
        fp = sum(1 for t in transactions if t.detection_result == "FP")
        tn = sum(1 for t in transactions if t.detection_result == "TN")
        fn = sum(1 for t in transactions if t.detection_result == "FN")
        unknown = sum(1 for t in transactions if not t.detection_result)

        logger.info(
            "Result collection complete — "
            "TP=%d  FP=%d  TN=%d  FN=%d  UNKNOWN=%d",
            tp, fp, tn, fn, unknown,
        )
