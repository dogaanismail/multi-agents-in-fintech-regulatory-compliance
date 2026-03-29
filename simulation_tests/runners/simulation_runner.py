"""
Simulation Runner
=================
Phase 2: fire all M payments concurrently through payment-svc.

THIS IS THE CORE OF THE DISSERTATION EVALUATION.

Architecture:
  - payment-svc (port 5003) is the SINGLE entry point
  - Each payment triggers the full Kafka chain automatically:
      payment-svc → PaymentCreatedEvent → risk-engine-svc
          → TPA + CRA + NAA + MADDPG orchestrator
          → RiskAssessmentCompletedEvent → payment-history-svc
  - We record: send timestamp, HTTP status, payment_id, latency
  - The Kafka async results are collected separately by ResultCollector

Concurrency model:
  - asyncio.Semaphore throttles to max_concurrency (default 50)
  - All tasks launched together — truly concurrent burst
  - Measures real wall-clock latency per request
"""

import asyncio
import logging
import time
from typing import List

from simulation_tests.clients.base_client import ClientError
from simulation_tests.clients.payment_client import PaymentClient
from simulation_tests.config import SIM_CONFIG
from simulation_tests.domain.transaction.factory import TransactionFactory
from simulation_tests.domain.transaction.models import TransactionRecord
from simulation_tests.runners.setup_runner import SimulationState

logger = logging.getLogger(__name__)


async def _fire_single(
    client: PaymentClient,
    record: TransactionRecord,
    semaphore: asyncio.Semaphore,
) -> None:
    """
    Fire one payment through payment-svc and record timing + response.
    Uses the semaphore to cap concurrency.
    """
    async with semaphore:
        record.send_timestamp_ms = time.time() * 1_000
        try:
            resp = await client.submit_payment(record.payment_data)
            record.response_timestamp_ms = time.time() * 1_000
            record.latency_ms = record.response_timestamp_ms - record.send_timestamp_ms
            record.payment_api_status = 201
            # payment-svc returns the payment ID for downstream polling
            record.payment_id = (
                str(resp.get("id") or resp.get("paymentId") or "")
            )
            logger.debug(
                "[%d] payment_id=%s  label=%-12s  latency=%.0fms",
                record.scenario_index,
                record.payment_id,
                record.fraud_label.value,
                record.latency_ms,
            )

        except ClientError as exc:
            record.response_timestamp_ms = time.time() * 1_000
            record.latency_ms = record.response_timestamp_ms - record.send_timestamp_ms
            record.payment_api_status = exc.status
            record.payment_api_error = exc.body[:200]
            logger.warning(
                "[%d] payment FAILED HTTP %s  label=%s  error=%s",
                record.scenario_index, exc.status,
                record.fraud_label.value, exc.body[:80],
            )

        except Exception as exc:
            record.response_timestamp_ms = time.time() * 1_000
            record.latency_ms = record.response_timestamp_ms - record.send_timestamp_ms
            record.payment_api_status = 0
            record.payment_api_error = str(exc)
            logger.error(
                "[%d] payment EXCEPTION: %s", record.scenario_index, exc
            )


class SimulationRunner:
    """
    Fires all transactions concurrently through payment-svc.

    Usage (async):
        runner = SimulationRunner(state)
        transactions = await runner.run()
    """

    def __init__(self, state: SimulationState):
        self._cfg = SIM_CONFIG
        self._state = state
        self._factory = TransactionFactory(seed=self._cfg.random_seed)

    async def run(self) -> List[TransactionRecord]:
        """
        1. Build all TransactionRecord objects from the simulation state.
        2. Fire all of them concurrently through payment-svc.
        3. Return the list with send/response timestamps and payment_ids set.
        """
        # ---- Build scenario list ----
        logger.info(
            "Building %d transaction scenarios "
            "(%d legit | %d smurfing | %d fan-out | %d layering "
            "| %d high-value | %d round-trip)...",
            self._cfg.num_payments,
            self._cfg.legitimate_count,
            self._cfg.fraud_smurfing_count,
            self._cfg.fraud_fanout_count,
            self._cfg.fraud_layering_count,
            self._cfg.fraud_high_value_count,
            self._cfg.fraud_round_trip_count,
        )
        transactions = self._factory.build(
            customers=self._state.customers,
            accounts=self._state.accounts,
        )
        logger.info(
            "Scenario built: %d transactions total.", len(transactions)
        )

        # ---- Concurrent fire ----
        semaphore = asyncio.Semaphore(self._cfg.max_concurrency)
        t_start = time.perf_counter()

        logger.info(
            "Firing %d payments concurrently (max_concurrency=%d)...",
            len(transactions), self._cfg.max_concurrency,
        )

        async with PaymentClient() as client:
            tasks = [
                asyncio.create_task(
                    _fire_single(client, record, semaphore)
                )
                for record in transactions
            ]
            await asyncio.gather(*tasks)

        wall_time = time.perf_counter() - t_start
        self._state.simulation_duration_seconds = wall_time

        succeeded = sum(1 for t in transactions if t.payment_id)
        failed = len(transactions) - succeeded
        logger.info(
            "Simulation complete: %d fired, %d had payment_id, %d failed — "
            "wall-clock %.2fs (avg %.0fms/req)",
            len(transactions), succeeded, failed,
            wall_time,
            (wall_time * 1_000) / max(len(transactions), 1),
        )

        return transactions
