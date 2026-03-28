"""
DB Backfill Script
==================
The 10k simulation run collected only ~129/10000 agent scores because
the Kafka pipeline hadn't finished draining when result_collector.py polled.

However, risk_engine_db has ALL 10,081 assessments fully stored:
  - risk_check_request   (payment_id lookup)
  - risk_assessment      (risk_action, risk_score, risk_level, fraud_indicators)
  - marl_assessment      (MARL action, confidence, q_value)
  - agent_observation    (TPA / CRA / NAA per-agent scores, 3 rows per payment)

This script:
  1. Reads the existing results.csv (has payment_id + fraud_label + latency)
  2. Bulk-queries risk_engine_db for ALL payment_ids in one SQL round-trip
  3. Backfills every row that has missing agent scores
  4. Recomputes detection_result (TP/FP/TN/FN) based on agent flags
  5. Rewrites results.csv and summary.json
  6. Regenerates all 10 dissertation charts

Usage:
  python backfill_from_db.py
  python backfill_from_db.py --csv simulation_tests/reports/results.csv
"""

import argparse
import csv
import json
import logging
import os
import statistics
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional

import psycopg2
import psycopg2.extras

# ── Config ────────────────────────────────────────────────────────────────────
DB_CONFIG = {
    "host": "localhost",
    "port": 5433,
    "dbname": "risk_engine_db",
    "user": "default",
    "password": "default",
}

CSV_PATH = Path("simulation_tests/reports/results.csv")
SUMMARY_PATH = Path("simulation_tests/reports/summary.json")
CHARTS_DIR = Path("simulation_tests/reports/charts")

AGENT_TYPE_MAP = {
    "TRANSACTION": "tpa",
    "CUSTOMER":    "cra",
    "NETWORK":     "naa",
}

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("backfill")


# ── DB query ──────────────────────────────────────────────────────────────────

def fetch_risk_data(payment_ids: List[str]) -> Dict[str, dict]:
    """
    Fetch all risk/MARL/agent data for the given payment_ids in one query.
    Returns a dict keyed by payment_id.
    """
    if not payment_ids:
        return {}

    log.info("Connecting to risk_engine_db on localhost:%d ...", DB_CONFIG["port"])
    conn = psycopg2.connect(**DB_CONFIG)
    cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

    try:
        # ── Main join: request + risk_assessment + marl_assessment ──────────
        cur.execute("""
            SELECT
                rcr.payment_id,
                rcr.amount,
                rcr.from_currency,
                rcr.to_currency,
                rcr.payment_type,
                ra.risk_score,
                ra.risk_level,
                ra.risk_action,
                ra.fraud_indicators,
                ra.processing_time_ms   AS risk_processing_ms,
                ma.id                   AS marl_id,
                ma.action               AS marl_action,
                ma.confidence           AS marl_confidence,
                ma.maddpg_q_value,
                ma.mode                 AS marl_mode,
                ma.processing_time_ms   AS marl_processing_ms
            FROM risk_check_request rcr
            JOIN risk_assessment  ra ON ra.risk_check_request_id = rcr.id
            JOIN marl_assessment  ma ON ma.risk_check_request_id = rcr.id
            WHERE rcr.payment_id = ANY(%s)
        """, (payment_ids,))

        rows = cur.fetchall()
        log.info("Main join returned %d rows for %d payment_ids", len(rows), len(payment_ids))

        # Index by payment_id
        data: Dict[str, dict] = {}
        marl_id_to_pid: Dict[str, str] = {}
        for row in rows:
            pid = row["payment_id"]
            data[pid] = dict(row)
            data[pid]["agents"] = {}
            marl_id_to_pid[str(row["marl_id"])] = pid

        # ── Agent observations ────────────────────────────────────────────────
        if marl_id_to_pid:
            # Cast string list to UUID[] explicitly — psycopg2 sends as text[]
            uuid_ids = [str(k) for k in marl_id_to_pid.keys()]
            cur.execute("""
                SELECT
                    marl_assessment_id,
                    agent_type,
                    is_suspicious,
                    probability,
                    risk_score,
                    confidence,
                    response_time_ms,
                    contribution
                FROM agent_observation
                WHERE marl_assessment_id = ANY(%s::uuid[])
            """, (uuid_ids,))

            for obs in cur.fetchall():
                mid = str(obs["marl_assessment_id"])
                pid = marl_id_to_pid.get(mid)
                if pid:
                    data[pid]["agents"][obs["agent_type"]] = dict(obs)

        log.info(
            "Fetched agent observations. Sample — %d payment_ids have all 3 agents.",
            sum(1 for v in data.values() if len(v["agents"]) == 3)
        )
        return data

    finally:
        cur.close()
        conn.close()


# ── Detection logic ───────────────────────────────────────────────────────────

def _is_flagged(row: dict) -> bool:
    """
    Replicates AgentSignals.is_flagged:
      any(tpa_is_suspicious, cra_is_high_risk, naa_is_suspicious,
          orchestrator_action in ("FLAG", "BLOCK"))
    """
    tpa_susp  = str(row.get("tpa_is_suspicious", "")).lower() in ("true", "1", "yes")
    cra_high  = str(row.get("cra_is_high_risk", "")).lower() in ("true", "1", "yes")
    naa_susp  = str(row.get("naa_is_suspicious", "")).lower() in ("true", "1", "yes")
    orch_flag = row.get("orchestrator_action", "") in ("FLAG", "BLOCK")
    return tpa_susp or cra_high or naa_susp or orch_flag


def _detection_result(row: dict) -> str:
    predicted_fraud = _is_flagged(row)
    actual_fraud = row.get("fraud_label", "LEGITIMATE") != "LEGITIMATE"
    if actual_fraud and predicted_fraud:
        return "TP"
    if actual_fraud and not predicted_fraud:
        return "FN"
    if not actual_fraud and predicted_fraud:
        return "FP"
    return "TN"


# ── Backfill ──────────────────────────────────────────────────────────────────

def backfill_rows(rows: List[dict], risk_data: Dict[str, dict]) -> int:
    updated = 0
    for row in rows:
        pid = row.get("payment_id", "").strip()
        if not pid or pid not in risk_data:
            continue

        d = risk_data[pid]
        tpa = d["agents"].get("TRANSACTION") or {}
        cra = d["agents"].get("CUSTOMER") or {}
        naa = d["agents"].get("NETWORK") or {}

        # Only backfill rows that are missing scores
        if row.get("tpa_risk_score") and row["tpa_risk_score"] != "":
            continue

        # TPA  (risk_score already on 0-100 scale in DB — verified against CSV)
        tpa_prob  = float(tpa.get("probability", 0) or 0)
        tpa_score = float(tpa.get("risk_score", 0) or 0)
        row["tpa_fraud_prob"]    = tpa_prob
        row["tpa_risk_score"]    = tpa_score
        row["tpa_is_suspicious"] = bool(tpa.get("is_suspicious", False))
        row["tpa_confidence"]    = tpa.get("confidence", "LOW")

        # CRA
        cra_prob  = float(cra.get("probability", 0) or 0)
        cra_score = float(cra.get("risk_score", 0) or 0)
        row["cra_risk_prob"]    = cra_prob
        row["cra_risk_score"]   = cra_score
        row["cra_risk_level"]   = cra.get("confidence", "LOW")
        row["cra_is_high_risk"] = bool(cra.get("is_suspicious", False))

        # NAA
        naa_prob  = float(naa.get("probability", 0) or 0)
        naa_score = float(naa.get("risk_score", 0) or 0)
        row["naa_suspicion_prob"] = naa_prob
        row["naa_risk_score"]     = naa_score
        row["naa_risk_level"]     = naa.get("confidence", "LOW")
        row["naa_is_suspicious"]  = bool(naa.get("is_suspicious", False))

        # Orchestrator (MARL) — keep original action names (ALLOW/REVIEW/BLOCK)
        row["orchestrator_action"] = d.get("marl_action", "")
        row["orchestrator_conf"]   = float(d.get("marl_confidence", 0) or 0)

        # CRA contributing factors (fraud_indicators from risk_assessment)
        indicators = d.get("fraud_indicators") or []
        if isinstance(indicators, list):
            row["cra_shap_factors"] = "|".join(indicators)
        else:
            row["cra_shap_factors"] = str(indicators)

        # Recompute detection_result
        row["detection_result"] = _detection_result(row)

        updated += 1

    return updated


# ── Summary & charts ──────────────────────────────────────────────────────────

def _safe_div(n, d, default=0.0):
    return n / d if d else default

def _pct(data, p):
    if not data:
        return 0.0
    s = sorted(data)
    return s[min(int(len(s) * p / 100), len(s) - 1)]


def _compute_summary(rows: List[dict]) -> dict:
    tp = sum(1 for r in rows if r.get("detection_result") == "TP")
    fp = sum(1 for r in rows if r.get("detection_result") == "FP")
    tn = sum(1 for r in rows if r.get("detection_result") == "TN")
    fn = sum(1 for r in rows if r.get("detection_result") == "FN")
    unknown = sum(1 for r in rows if r.get("detection_result") not in ("TP","FP","TN","FN"))

    total   = len(rows)
    fraud_c = tp + fn
    legit_c = fp + tn

    precision   = _safe_div(tp, tp + fp)
    recall      = _safe_div(tp, tp + fn)
    specificity = _safe_div(tn, tn + fp)
    f1          = _safe_div(2 * precision * recall, precision + recall)
    accuracy    = _safe_div(tp + tn, total)

    latencies = [float(r["latency_ms"]) for r in rows if r.get("latency_ms") not in ("", None)]

    # Per-typology
    def detrate(label):
        txns = [r for r in rows if r.get("fraud_label") == label]
        det  = sum(1 for r in txns if r.get("detection_result") == "TP")
        return txns, det

    smurfing_txns, smurfing_det = detrate("SMURFING")
    fanout_txns,   fanout_det   = detrate("FAN_OUT")
    layering_txns, layering_det = detrate("LAYERING")
    hv_txns,       hv_det       = detrate("HIGH_VALUE_SINGLE")
    rt_txns,       rt_det       = detrate("ROUND_TRIP")

    # Agent averages
    tpa_scores = [float(r["tpa_risk_score"]) for r in rows if r.get("tpa_risk_score") not in ("", None, "")]
    cra_scores = [float(r["cra_risk_score"]) for r in rows if r.get("cra_risk_score") not in ("", None, "")]
    naa_scores = [float(r["naa_risk_score"]) for r in rows if r.get("naa_risk_score") not in ("", None, "")]

    # Orchestrator distribution
    allow_c  = sum(1 for r in rows if r.get("orchestrator_action") == "ALLOW")
    review_c = sum(1 for r in rows if r.get("orchestrator_action") == "REVIEW")
    block_c  = sum(1 for r in rows if r.get("orchestrator_action") == "BLOCK")

    return {
        "total_transactions":         total,
        "legitimate_count":           legit_c,
        "fraud_count":                fraud_c,
        "true_positives":             tp,
        "false_positives":            fp,
        "true_negatives":             tn,
        "false_negatives":            fn,
        "unknown_count":              unknown,
        "precision":                  round(precision, 6),
        "recall":                     round(recall, 6),
        "specificity":                round(specificity, 6),
        "f1_score":                   round(f1, 6),
        "accuracy":                   round(accuracy, 6),
        "smurfing_detection_rate":    _safe_div(smurfing_det, len(smurfing_txns)),
        "fanout_detection_rate":      _safe_div(fanout_det, len(fanout_txns)),
        "layering_detection_rate":    _safe_div(layering_det, len(layering_txns)),
        "high_value_detection_rate":  _safe_div(hv_det, len(hv_txns)),
        "round_trip_detection_rate":  _safe_div(rt_det, len(rt_txns)),
        "smurfing_count":             len(smurfing_txns),
        "fanout_count":               len(fanout_txns),
        "layering_count":             len(layering_txns),
        "high_value_count":           len(hv_txns),
        "round_trip_count":           len(rt_txns),
        "latency_mean_ms":            round(statistics.mean(latencies), 2) if latencies else 0,
        "latency_p50_ms":             round(_pct(latencies, 50), 2),
        "latency_p95_ms":             round(_pct(latencies, 95), 2),
        "latency_p99_ms":             round(_pct(latencies, 99), 2),
        "latency_max_ms":             round(max(latencies), 2) if latencies else 0,
        "orchestrator_approve_count": allow_c,
        "orchestrator_flag_count":    review_c,
        "orchestrator_block_count":   block_c,
        "tpa_avg_risk_score":         round(statistics.mean(tpa_scores), 4) if tpa_scores else 0,
        "cra_avg_risk_score":         round(statistics.mean(cra_scores), 4) if cra_scores else 0,
        "naa_avg_risk_score":         round(statistics.mean(naa_scores), 4) if naa_scores else 0,
        "setup_duration_seconds":     0.0,
        "simulation_duration_seconds": 0.0,
    }


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Backfill agent scores from risk_engine_db")
    parser.add_argument("--csv", default=str(CSV_PATH), help="Path to results.csv")
    parser.add_argument("--no-charts", action="store_true", help="Skip chart generation")
    args = parser.parse_args()

    csv_path = Path(args.csv)
    if not csv_path.exists():
        log.error("results.csv not found at %s", csv_path)
        sys.exit(1)

    # ── Step 1: read existing CSV ──────────────────────────────────────────────
    log.info("Reading %s ...", csv_path)
    with open(csv_path, newline="") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        rows = list(reader)

    log.info("Loaded %d rows. Fields: %s", len(rows), ", ".join(fieldnames or []))
    missing_before = sum(1 for r in rows if not r.get("tpa_risk_score"))
    log.info("Rows missing agent scores: %d", missing_before)

    # ── Step 2: bulk-fetch all payment_ids from DB ─────────────────────────────
    payment_ids = [r["payment_id"].strip() for r in rows if r.get("payment_id")]
    log.info("Querying risk_engine_db for %d payment_ids ...", len(payment_ids))
    risk_data = fetch_risk_data(payment_ids)
    log.info("DB returned data for %d / %d payment_ids", len(risk_data), len(payment_ids))

    # ── Step 3: backfill ───────────────────────────────────────────────────────
    updated = backfill_rows(rows, risk_data)
    log.info("Backfilled %d rows (already-scored rows left unchanged)", updated)

    # Also recompute detection_result for rows that already had scores
    # (in case the detection logic needs to be consistent)
    for row in rows:
        if row.get("tpa_risk_score") and row.get("detection_result"):
            row["detection_result"] = _detection_result(row)

    # ── Step 4: write updated CSV ──────────────────────────────────────────────
    log.info("Writing updated %s ...", csv_path)
    with open(csv_path, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
    log.info("CSV written — %d rows.", len(rows))

    # ── Step 5: compute summary and write JSON ─────────────────────────────────
    summary = _compute_summary(rows)
    SUMMARY_PATH.parent.mkdir(parents=True, exist_ok=True)

    # Preserve timing from original summary.json if it exists
    if SUMMARY_PATH.exists():
        with open(SUMMARY_PATH) as f:
            orig = json.load(f)
        summary["setup_duration_seconds"]       = orig.get("setup_duration_seconds", 0)
        summary["simulation_duration_seconds"]  = orig.get("simulation_duration_seconds", 0)

    with open(SUMMARY_PATH, "w") as f:
        json.dump(summary, f, indent=2)

    # ── Print results ──────────────────────────────────────────────────────────
    tp = summary["true_positives"]
    fp = summary["false_positives"]
    tn = summary["true_negatives"]
    fn = summary["false_negatives"]
    fraud_c = summary["fraud_count"]
    print("\n" + "═" * 68)
    print("  BACKFILL COMPLETE — UPDATED RESULTS")
    print("═" * 68)
    print(f"  Rows backfilled      : {updated:,}")
    print(f"  DB coverage          : {len(risk_data):,} / {len(payment_ids):,} payment_ids found")
    print()
    print(f"  Total payments       : {summary['total_transactions']:,}")
    print(f"  Fraud seeded         : {fraud_c}")
    print(f"  ┌─ True  Positives   : {tp}  (detected fraud)")
    print(f"  ├─ False Negatives   : {fn}  (missed fraud)")
    print(f"  ├─ True  Negatives   : {tn}  (correct legit)")
    print(f"  └─ False Positives   : {fp}  (false alarms)")
    print()
    print(f"  Precision            : {summary['precision']*100:.1f}%")
    print(f"  Recall (Detection)   : {summary['recall']*100:.1f}%")
    print(f"  F1 Score             : {summary['f1_score']*100:.1f}%")
    print(f"  Specificity          : {summary['specificity']*100:.1f}%")
    print()
    print(f"  Smurfing detection   : {summary['smurfing_detection_rate']*100:.1f}%  ({summary['smurfing_count']} transactions)")
    print(f"  Fan-out detection    : {summary['fanout_detection_rate']*100:.1f}%  ({summary['fanout_count']} transactions)")
    print(f"  Layering detection   : {summary['layering_detection_rate']*100:.1f}%  ({summary['layering_count']} transactions)")
    print(f"  High-Value detection : {summary['high_value_detection_rate']*100:.1f}%  ({summary['high_value_count']} transactions)")
    print(f"  Round-Trip detection : {summary['round_trip_detection_rate']*100:.1f}%  ({summary['round_trip_count']} transactions)")
    print()
    print(f"  Latency (mean)       : {summary['latency_mean_ms']:.0f} ms")
    print(f"  Latency (P95)        : {summary['latency_p95_ms']:.0f} ms")
    print(f"  Latency (P99)        : {summary['latency_p99_ms']:.0f} ms")
    print()
    print(f"  TPA avg risk score   : {summary['tpa_avg_risk_score']:.2f}")
    print(f"  CRA avg risk score   : {summary['cra_avg_risk_score']:.4f}")
    print(f"  NAA avg risk score   : {summary['naa_avg_risk_score']:.2f}")
    print()
    print(f"  Orchestrator — ALLOW : {summary['orchestrator_approve_count']:,}")
    print(f"  Orchestrator — REVIEW: {summary['orchestrator_flag_count']:,}")
    print(f"  Orchestrator — BLOCK : {summary['orchestrator_block_count']:,}")
    print()
    print(f"  Results CSV          : {csv_path}")
    print(f"  Summary JSON         : {SUMMARY_PATH}")

    # ── Step 6: regenerate charts ──────────────────────────────────────────────
    if not args.no_charts:
        log.info("Generating charts ...")
        CHARTS_DIR.mkdir(parents=True, exist_ok=True)
        sys.path.insert(0, str(Path(__file__).parent))
        try:
            from simulation_tests.reports.chart_generator import ChartGenerator

            # Build lightweight proxy objects that ChartGenerator can consume
            # ChartGenerator.generate_all() accepts (transactions, summary_dict_or_obj)
            # We pass it the summary dict directly (it supports both via getattr/dict.get)
            class _Proxy:
                """Proxy a CSV row as a minimal object for chart_generator compatibility."""
                def __init__(self, row: dict):
                    self._r = row
                    from types import SimpleNamespace
                    s = self._r
                    self.fraud_label   = s.get("fraud_label", "LEGITIMATE")
                    self.detection_result = s.get("detection_result", "")
                    self.latency_ms    = float(s["latency_ms"]) if s.get("latency_ms") else None
                    self.payment_id    = s.get("payment_id")
                    self.payment_data  = SimpleNamespace(
                        amount=float(s["amount"]) if s.get("amount") else 0,
                        from_currency=s.get("from_currency", ""),
                        to_currency=s.get("to_currency", ""),
                        payment_type=s.get("payment_type", "TRANSFER_OUT"),
                    )
                    # agent_signals proxy
                    def _f(key, fallback=None):
                        v = s.get(key)
                        if v in ("", None): return fallback
                        return v
                    self.agent_signals = SimpleNamespace(
                        tpa_fraud_probability  = float(_f("tpa_fraud_prob", 0)),
                        tpa_risk_score         = float(_f("tpa_risk_score", 0)),
                        tpa_is_suspicious      = str(_f("tpa_is_suspicious","False")).lower() in ("true","1"),
                        tpa_confidence         = _f("tpa_confidence", "LOW"),
                        tpa_recommendation     = None,   # not in DB — Fig J uses keyword fallback
                        cra_risk_probability   = float(_f("cra_risk_prob", 0)),
                        cra_risk_score         = float(_f("cra_risk_score", 0)),
                        cra_risk_level         = _f("cra_risk_level", "LOW"),
                        cra_is_high_risk       = str(_f("cra_is_high_risk","False")).lower() in ("true","1"),
                        naa_suspicion_probability = float(_f("naa_suspicion_prob", 0)),
                        naa_risk_score         = float(_f("naa_risk_score", 0)),
                        naa_risk_level         = _f("naa_risk_level", "LOW"),
                        naa_is_suspicious      = str(_f("naa_is_suspicious","False")).lower() in ("true","1"),
                        orchestrator_action    = _f("orchestrator_action", ""),
                        orchestrator_confidence = float(_f("orchestrator_conf", 0)),
                        orchestrator_agent_contributions = None,
                        cra_contributing_factors = [x for x in (_f("cra_shap_factors","") or "").split("|") if x],
                        naa_network_indicators   = None,   # not stored per-row in CSV
                    )

            proxies = [_Proxy(r) for r in rows]

            class SummaryObj:
                """Allow both attribute and dict-style access for summary."""
                def __init__(self, d): self.__dict__.update(d)

            cg = ChartGenerator()
            cg.generate_all(proxies, SummaryObj(summary))
            print(f"  Charts               : {CHARTS_DIR}/")
        except Exception as exc:
            log.error("Chart generation failed: %s", exc, exc_info=True)
    print("═" * 68 + "\n")


if __name__ == "__main__":
    main()
