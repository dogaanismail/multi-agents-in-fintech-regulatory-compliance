"""
AML Multi-Agent Simulation
==========================
End-to-End Integration Test for Chapter 5 — Evaluation

Dissertation: Multi-Agents in Fintech Regulatory Compliance
Author:       Ismail Dogan
University:   University of Liverpool, 2026

─────────────────────────────────────────────────────────────────────────────
WHAT THIS SCRIPT DOES
─────────────────────────────────────────────────────────────────────────────
1. SETUP     Create N synthetic customers + accounts via customer-svc and
             account-svc. Seeds ~2% as fraudulent MULE actors.

2. SIMULATE  Fire M payments CONCURRENTLY through payment-svc (port 5003).
             ~950 legitimate + ~50 seeded fraud (smurfing, fan-out, layering).
             The Kafka chain handles everything downstream automatically:
               payment-svc → risk-engine-svc → TPA + CRA + NAA + MADDPG
               → payment-history-svc

3. COLLECT   Poll payment-history-svc for each payment_id to retrieve
             the final risk verdicts and agent scores.

4. REPORT    Compute TP/FP/TN/FN, precision, recall, F1, latency percentiles.
             Write results.csv, summary.json, and 7 dissertation figures.

─────────────────────────────────────────────────────────────────────────────
HOW TO RUN
─────────────────────────────────────────────────────────────────────────────
  # Make sure all Docker services are running:
  # bank-solution-backend:  docker-compose up -d   (ports 5001-5009)
  # ai-services:            docker-compose up -d   (ports 1001-1004)

  cd /path/to/project
  source .venv/bin/activate
  pip install -r simulation_tests/requirements.txt

  python run_simulation.py
  python run_simulation.py --customers 100 --payments 100   # quick smoke test

─────────────────────────────────────────────────────────────────────────────
"""

import argparse
import asyncio
import logging
import sys
import time
from pathlib import Path

# Ensure the project root is on the path
sys.path.insert(0, str(Path(__file__).parent))

from simulation_tests.config import SIM_CONFIG
from simulation_tests.reports.aggregator import ResultAggregator
from simulation_tests.reports.chart_generator import ChartGenerator
from simulation_tests.runners.result_collector import ResultCollector
from simulation_tests.runners.setup_runner import SetupRunner
from simulation_tests.runners.simulation_runner import SimulationRunner


def _configure_logging(verbose: bool = False) -> None:
    level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(
        level=level,
        format="%(asctime)s  %(levelname)-8s  %(name)s  %(message)s",
        datefmt="%H:%M:%S",
        handlers=[
            logging.StreamHandler(sys.stdout),
            logging.FileHandler("simulation_tests/logs/simulation.log", mode="w"),
        ],
    )


async def run(args: argparse.Namespace) -> None:
    # Apply CLI overrides to the singleton config
    if args.customers:
        SIM_CONFIG.num_customers = args.customers
    if args.payments:
        n = args.payments
        SIM_CONFIG.num_payments = n
        # Proportional 5-typology fraud split (matches full-run ratios)
        s  = max(1, round(n * 0.030))   # smurfing
        fo = max(1, round(n * 0.025))   # fan-out
        la = max(1, round(n * 0.025))   # layering
        hv = max(1, round(n * 0.010))   # high-value single
        rt = max(2, round(n * 0.010))   # round-trip (must be even — 2 legs per pair)
        rt = rt if rt % 2 == 0 else rt + 1
        SIM_CONFIG.fraud_smurfing_count      = s
        SIM_CONFIG.fraud_fanout_count        = fo
        SIM_CONFIG.fraud_layering_count      = la
        SIM_CONFIG.fraud_high_value_count    = hv
        SIM_CONFIG.fraud_round_trip_count    = rt
        SIM_CONFIG.legitimate_count          = max(0, n - s - fo - la - hv - rt)

    if args.concurrency:
        SIM_CONFIG.max_concurrency = args.concurrency
    if args.drain_wait is not None:
        SIM_CONFIG.kafka_drain_wait_seconds = args.drain_wait

    logger = logging.getLogger("simulation")

    fraud_total = (SIM_CONFIG.fraud_smurfing_count + SIM_CONFIG.fraud_fanout_count
                   + SIM_CONFIG.fraud_layering_count + SIM_CONFIG.fraud_high_value_count
                   + SIM_CONFIG.fraud_round_trip_count)
    print("\n" + "═" * 68)
    print("  AML Multi-Agent Simulation — End-to-End Integration Test")
    print("═" * 68)
    print(f"  Customers      : {SIM_CONFIG.num_customers:,}")
    print(f"  Payments       : {SIM_CONFIG.num_payments:,}")
    print(f"  Fraud seeded   : {fraud_total}  ({fraud_total/SIM_CONFIG.num_payments*100:.1f}%)")
    print(f"    › Smurfing   : {SIM_CONFIG.fraud_smurfing_count}")
    print(f"    › Fan-out    : {SIM_CONFIG.fraud_fanout_count}")
    print(f"    › Layering   : {SIM_CONFIG.fraud_layering_count}")
    print(f"    › High-Value : {SIM_CONFIG.fraud_high_value_count}")
    print(f"    › Round-Trip : {SIM_CONFIG.fraud_round_trip_count}")
    print(f"  Legitimate     : {SIM_CONFIG.legitimate_count:,}")
    print(f"  Concurrency    : {SIM_CONFIG.max_concurrency}")
    print(f"  Drain wait     : {SIM_CONFIG.kafka_drain_wait_seconds}s  (Kafka pipeline settle)")
    print("═" * 68 + "\n")

    t_total = time.perf_counter()

    # ──────────────────────────────────────────────────────────────────
    # PHASE 1: Setup
    # ──────────────────────────────────────────────────────────────────
    logger.info("PHASE 1 ── Setup (customer-svc + account-svc)")
    state = await SetupRunner().run()

    # ──────────────────────────────────────────────────────────────────
    # PHASE 2: Simulation — all payments through payment-svc
    # ──────────────────────────────────────────────────────────────────
    logger.info("PHASE 2 ── Simulation (payment-svc → Kafka → AI agents)")
    transactions = await SimulationRunner(state).run()

    # ──────────────────────────────────────────────────────────────────
    # PHASE 2.5: Kafka drain wait
    # All payments are in Kafka. Give the async pipeline time to process
    # before we start polling payment-history-svc. Without this, most
    # results arrive as UNKNOWN because the risk verdict hasn't landed yet.
    # ──────────────────────────────────────────────────────────────────
    drain_secs = SIM_CONFIG.kafka_drain_wait_seconds
    logger.info(
        "PHASE 2.5 ── Waiting %ds for Kafka pipeline to drain "
        "(payment-svc → risk-engine → TPA/CRA/NAA/MARL → payment-history)",
        drain_secs,
    )
    for remaining in range(drain_secs, 0, -10):
        logger.info("  Pipeline drain: %ds remaining...", remaining)
        await asyncio.sleep(min(10, remaining))
    logger.info("  Drain complete — starting result collection.")

    # ──────────────────────────────────────────────────────────────────
    # PHASE 3: Result Collection — poll payment-history-svc
    # ──────────────────────────────────────────────────────────────────
    logger.info("PHASE 3 ── Result collection (payment-history-svc)")
    await ResultCollector().collect(transactions)

    # ──────────────────────────────────────────────────────────────────
    # PHASE 4: Reports + Charts
    # ──────────────────────────────────────────────────────────────────
    logger.info("PHASE 4 ── Aggregating results and generating charts")
    aggregator = ResultAggregator()
    summary = aggregator.aggregate(
        transactions,
        setup_duration=state.setup_duration_seconds,
        simulation_duration=state.simulation_duration_seconds,
    )
    aggregator.write_csv(transactions)
    aggregator.write_summary(summary)

    if not args.no_charts:
        ChartGenerator().generate_all(transactions, summary)

    # ──────────────────────────────────────────────────────────────────
    # Final summary
    # ──────────────────────────────────────────────────────────────────
    total_time = time.perf_counter() - t_total
    fraud_total = summary.true_positives + summary.false_negatives

    print("\n" + "═" * 68)
    print("  SIMULATION RESULTS")
    print("═" * 68)
    print(f"  Total payments       : {summary.total_transactions:,}")
    print(f"  Fraud seeded         : {fraud_total}")
    print(f"  ┌─ True  Positives   : {summary.true_positives}  (detected fraud)")
    print(f"  ├─ False Negatives   : {summary.false_negatives}  (missed fraud)")
    print(f"  ├─ True  Negatives   : {summary.true_negatives}  (correct legit)")
    print(f"  └─ False Positives   : {summary.false_positives}  (false alarms)")
    print()
    print(f"  Precision            : {summary.precision * 100:.1f}%")
    print(f"  Recall (Detection)   : {summary.recall * 100:.1f}%")
    print(f"  F1 Score             : {summary.f1_score * 100:.1f}%")
    print(f"  Specificity          : {summary.specificity * 100:.1f}%")
    print()
    print(f"  Smurfing detection   : {summary.smurfing_detection_rate * 100:.1f}%")
    print(f"  Fan-out detection    : {summary.fanout_detection_rate * 100:.1f}%")
    print(f"  Layering detection   : {summary.layering_detection_rate * 100:.1f}%")
    print()
    print(f"  Latency (mean)       : {summary.latency_mean_ms:.0f} ms")
    print(f"  Latency (P95)        : {summary.latency_p95_ms:.0f} ms")
    print(f"  Latency (P99)        : {summary.latency_p99_ms:.0f} ms")
    print()
    print(f"  Setup time           : {state.setup_duration_seconds:.1f}s")
    print(f"  Simulation time      : {state.simulation_duration_seconds:.1f}s")
    print(f"  Total wall-clock     : {total_time:.1f}s")
    print()
    print(f"  Results CSV          : {SIM_CONFIG.results_csv}")
    print(f"  Summary JSON         : {SIM_CONFIG.summary_json}")
    print(f"  Charts               : {SIM_CONFIG.charts_dir}/")
    print("═" * 68 + "\n")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="AML Multi-Agent End-to-End Simulation"
    )
    parser.add_argument(
        "--customers", type=int, default=None,
        help="Number of synthetic customers to create (default: 1000)",
    )
    parser.add_argument(
        "--payments", type=int, default=None,
        help="Number of concurrent payments to fire (default: 1000)",
    )
    parser.add_argument(
        "--concurrency", type=int, default=None,
        help="Max simultaneous HTTP connections (default: 50)",
    )
    parser.add_argument(
        "--drain-wait", type=int, default=None, dest="drain_wait",
        help="Seconds to wait after payments are fired before polling results "
             "(default: 120 for full run; use 15 for smoke tests)",
    )
    parser.add_argument(
        "--no-charts", action="store_true",
        help="Skip chart generation (faster, for CI environments)",
    )
    parser.add_argument(
        "--verbose", "-v", action="store_true",
        help="Enable DEBUG-level logging",
    )
    args = parser.parse_args()

    # Create output directories
    Path("simulation_tests/reports/charts").mkdir(parents=True, exist_ok=True)
    Path("simulation_tests/logs").mkdir(parents=True, exist_ok=True)

    _configure_logging(verbose=args.verbose)

    asyncio.run(run(args))


if __name__ == "__main__":
    main()
