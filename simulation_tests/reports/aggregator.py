"""
Result Aggregator
=================
Computes all dissertation metrics from the list of TransactionRecords.

Produces:
  1. A flat CSV (one row per transaction)  →  results.csv
  2. A summary JSON with all headline stats →  summary.json

Metrics computed:
  - TP / FP / TN / FN counts
  - Precision, Recall (Sensitivity), Specificity, F1, Accuracy
  - Per-typology detection rate (Smurfing / Fan-Out / Layering)
  - Latency: mean, p50, p95, p99, max (payment-svc round-trip)
  - Kafka end-to-end processing time (where available in history)
  - Orchestrator action distribution (APPROVE / FLAG / BLOCK)
  - Average agent risk scores (TPA / CRA / NAA)

What is SHAP? (Plain English for the dissertation reader)
---------------------------------------------------------
SHAP stands for SHapley Additive exPlanations. For each prediction,
SHAP assigns a contribution score to every input feature, telling us
exactly HOW MUCH each feature pushed the prediction toward "fraud" or
"legitimate". Think of it like splitting the credit/blame for a verdict
across all the evidence available.

Example from our TPA agent:
  Payment Amount    → +0.058  (pushed score toward FRAUD)
  Receiver_GB       → +0.039  (cross-border to GB raised suspicion)
  Cash Withdrawal   → +0.023  (payment type added suspicion)
  Transaction Time  → -0.011  (timing was typical, slightly reassuring)

The contributing_factors field in CRA and NAA responses contain the
human-readable SHAP top features. We aggregate them here to show which
features drove detections across the 1,000 simulation payments.
"""

import json
import logging
import os
import statistics
from dataclasses import asdict, dataclass, field
from typing import Dict, List, Optional

import pandas as pd

from simulation_tests.config import SIM_CONFIG
from simulation_tests.domain.transaction.models import FraudLabel, TransactionRecord

logger = logging.getLogger(__name__)


@dataclass
class SimulationSummary:
    """All headline statistics — written to summary.json."""

    # Population
    total_transactions: int = 0
    legitimate_count: int = 0
    fraud_count: int = 0

    # Detection accuracy
    true_positives: int = 0
    false_positives: int = 0
    true_negatives: int = 0
    false_negatives: int = 0
    unknown_count: int = 0

    precision: float = 0.0
    recall: float = 0.0     # also called Sensitivity / Detection Rate
    specificity: float = 0.0
    f1_score: float = 0.0
    accuracy: float = 0.0

    # Per-typology detection rates
    smurfing_detection_rate: float = 0.0
    fanout_detection_rate: float = 0.0
    layering_detection_rate: float = 0.0
    high_value_detection_rate: float = 0.0
    round_trip_detection_rate: float = 0.0

    # Per-typology counts (for tables)
    smurfing_count: int = 0
    fanout_count: int = 0
    layering_count: int = 0
    high_value_count: int = 0
    round_trip_count: int = 0

    # Latency (payment-svc round-trip, milliseconds)
    latency_mean_ms: float = 0.0
    latency_p50_ms: float = 0.0
    latency_p95_ms: float = 0.0
    latency_p99_ms: float = 0.0
    latency_max_ms: float = 0.0

    # Orchestrator action distribution
    orchestrator_approve_count: int = 0
    orchestrator_flag_count: int = 0
    orchestrator_block_count: int = 0

    # Average agent risk scores
    tpa_avg_risk_score: float = 0.0
    cra_avg_risk_score: float = 0.0
    naa_avg_risk_score: float = 0.0

    # Setup / run timing
    setup_duration_seconds: float = 0.0
    simulation_duration_seconds: float = 0.0


def _safe_div(numerator: float, denominator: float, default: float = 0.0) -> float:
    return numerator / denominator if denominator else default


def _percentile(data: list, pct: float) -> float:
    if not data:
        return 0.0
    sorted_data = sorted(data)
    idx = int(len(sorted_data) * pct / 100)
    return sorted_data[min(idx, len(sorted_data) - 1)]


class ResultAggregator:
    """
    Aggregates TransactionRecords into CSV + JSON reports.

    Usage:
        aggregator = ResultAggregator()
        summary = aggregator.aggregate(transactions, state)
        aggregator.write_csv(transactions)
        aggregator.write_summary(summary)
    """

    def __init__(self):
        os.makedirs(os.path.dirname(SIM_CONFIG.results_csv), exist_ok=True)
        os.makedirs(SIM_CONFIG.charts_dir, exist_ok=True)

    def aggregate(
        self,
        transactions: List[TransactionRecord],
        setup_duration: float = 0.0,
        simulation_duration: float = 0.0,
    ) -> SimulationSummary:
        s = SimulationSummary()
        s.setup_duration_seconds = setup_duration
        s.simulation_duration_seconds = simulation_duration

        s.total_transactions = len(transactions)
        s.legitimate_count = sum(
            1 for t in transactions if t.fraud_label == FraudLabel.LEGITIMATE
        )
        s.fraud_count = s.total_transactions - s.legitimate_count

        # ---- Confusion matrix ----
        s.true_positives  = sum(1 for t in transactions if t.detection_result == "TP")
        s.false_positives = sum(1 for t in transactions if t.detection_result == "FP")
        s.true_negatives  = sum(1 for t in transactions if t.detection_result == "TN")
        s.false_negatives = sum(1 for t in transactions if t.detection_result == "FN")
        s.unknown_count   = sum(1 for t in transactions if not t.detection_result)

        s.precision    = _safe_div(s.true_positives, s.true_positives + s.false_positives)
        s.recall       = _safe_div(s.true_positives, s.true_positives + s.false_negatives)
        s.specificity  = _safe_div(s.true_negatives, s.true_negatives + s.false_positives)
        s.f1_score     = _safe_div(2 * s.precision * s.recall, s.precision + s.recall)
        s.accuracy     = _safe_div(
            s.true_positives + s.true_negatives, s.total_transactions
        )

        # ---- Per-typology detection ----
        for label, rate_attr, count_attr in [
            (FraudLabel.SMURFING,         "smurfing_detection_rate",   "smurfing_count"),
            (FraudLabel.FAN_OUT,          "fanout_detection_rate",     "fanout_count"),
            (FraudLabel.LAYERING,         "layering_detection_rate",   "layering_count"),
            (FraudLabel.HIGH_VALUE_SINGLE,"high_value_detection_rate", "high_value_count"),
            (FraudLabel.ROUND_TRIP,       "round_trip_detection_rate", "round_trip_count"),
        ]:
            typology_txns = [t for t in transactions if t.fraud_label == label]
            detected = sum(1 for t in typology_txns if t.detection_result == "TP")
            setattr(s, rate_attr,  _safe_div(detected, len(typology_txns)))
            setattr(s, count_attr, len(typology_txns))

        # ---- Latency ----
        latencies = [
            t.latency_ms for t in transactions
            if t.latency_ms is not None and t.latency_ms > 0
        ]
        if latencies:
            s.latency_mean_ms = statistics.mean(latencies)
            s.latency_p50_ms  = _percentile(latencies, 50)
            s.latency_p95_ms  = _percentile(latencies, 95)
            s.latency_p99_ms  = _percentile(latencies, 99)
            s.latency_max_ms  = max(latencies)

        # ---- Orchestrator action distribution ----
        for t in transactions:
            action = (t.agent_signals.orchestrator_action or "").upper()
            if "APPROVE" in action or action == "LOW":
                s.orchestrator_approve_count += 1
            elif "FLAG" in action or "REVIEW" in action or action == "MEDIUM":
                s.orchestrator_flag_count += 1
            elif "BLOCK" in action or action in ("HIGH", "CRITICAL"):
                s.orchestrator_block_count += 1

        # ---- Average agent risk scores ----
        tpa_scores = [t.agent_signals.tpa_risk_score for t in transactions
                      if t.agent_signals.tpa_risk_score is not None]
        cra_scores = [t.agent_signals.cra_risk_score for t in transactions
                      if t.agent_signals.cra_risk_score is not None]
        naa_scores = [t.agent_signals.naa_risk_score for t in transactions
                      if t.agent_signals.naa_risk_score is not None]

        s.tpa_avg_risk_score = statistics.mean(tpa_scores) if tpa_scores else 0.0
        s.cra_avg_risk_score = statistics.mean(cra_scores) if cra_scores else 0.0
        s.naa_avg_risk_score = statistics.mean(naa_scores) if naa_scores else 0.0

        logger.info(
            "Summary — TP=%d FP=%d TN=%d FN=%d | "
            "Precision=%.3f Recall=%.3f F1=%.3f | "
            "LatencyMean=%.0fms P95=%.0fms",
            s.true_positives, s.false_positives,
            s.true_negatives, s.false_negatives,
            s.precision, s.recall, s.f1_score,
            s.latency_mean_ms, s.latency_p95_ms,
        )

        return s

    def write_csv(self, transactions: List[TransactionRecord]) -> str:
        """Write flat CSV with one row per transaction. Returns file path."""
        rows = []
        for t in transactions:
            sigs = t.agent_signals
            rows.append({
                # Identity
                "scenario_index":       t.scenario_index,
                "payment_id":           t.payment_id or "",
                "fraud_label":          t.fraud_label.value,
                "detection_result":     t.detection_result or "UNKNOWN",
                # Timing
                "latency_ms":           round(t.latency_ms or 0, 2),
                "payment_api_status":   t.payment_api_status or 0,
                # Payment details
                "amount":               t.payment_data.amount,
                "from_currency":        t.payment_data.from_currency,
                "to_currency":          t.payment_data.to_currency,
                "payment_type":         t.payment_data.payment_type,
                # TPA signals
                "tpa_fraud_prob":       sigs.tpa_fraud_probability,
                "tpa_risk_score":       sigs.tpa_risk_score,
                "tpa_is_suspicious":    sigs.tpa_is_suspicious,
                "tpa_confidence":       sigs.tpa_confidence,
                # CRA signals
                "cra_risk_prob":        sigs.cra_risk_probability,
                "cra_risk_score":       sigs.cra_risk_score,
                "cra_risk_level":       sigs.cra_risk_level,
                "cra_is_high_risk":     sigs.cra_is_high_risk,
                # NAA signals
                "naa_suspicion_prob":   sigs.naa_suspicion_probability,
                "naa_risk_score":       sigs.naa_risk_score,
                "naa_risk_level":       sigs.naa_risk_level,
                "naa_is_suspicious":    sigs.naa_is_suspicious,
                # Orchestrator (MADDPG)
                "orchestrator_action":  sigs.orchestrator_action or "",
                "orchestrator_conf":    sigs.orchestrator_confidence,
                # SHAP: top contributing factors from CRA (human-readable)
                "cra_shap_factors":     "; ".join(sigs.cra_contributing_factors or []),
            })

        df = pd.DataFrame(rows)
        df.to_csv(SIM_CONFIG.results_csv, index=False)
        logger.info("CSV written → %s  (%d rows)", SIM_CONFIG.results_csv, len(rows))
        return SIM_CONFIG.results_csv

    def write_summary(self, summary: SimulationSummary) -> str:
        """Write summary JSON. Returns file path."""
        # Round all floats for readability
        data = {}
        for key, val in vars(summary).items():
            data[key] = round(val, 4) if isinstance(val, float) else val

        with open(SIM_CONFIG.summary_json, "w") as f:
            json.dump(data, f, indent=2)
        logger.info("Summary JSON written → %s", SIM_CONFIG.summary_json)
        return SIM_CONFIG.summary_json
