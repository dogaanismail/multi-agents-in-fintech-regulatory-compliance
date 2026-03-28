"""
Payment History Client
======================
GET /api/v1/payment-history/{paymentId}

Used by result_collector.py after the simulation fires payments.
The Kafka chain (payment-svc → risk-engine → payment-history) is async,
so the collector polls with retries until the record appears or times out.

Why payment-history-svc?
  After a payment is submitted, risk-engine-svc processes it and publishes
  RiskAssessmentCompletedEvent. payment-history-svc consumes that event and
  stores the final enriched record including the risk verdict.
  This is the single read-side source of truth for our simulation results.
"""

import asyncio
import logging
from typing import Optional

from simulation_tests.clients.base_client import BaseClient, ClientError
from simulation_tests.config import ServiceURLs

logger = logging.getLogger(__name__)


class PaymentHistoryClient(BaseClient):
    """Async client for payment-history-svc (port 5005)."""

    def __init__(self):
        super().__init__(base_url=ServiceURLs.PAYMENT_HISTORY)

    async def get_by_payment_id(self, payment_id: str) -> Optional[dict]:
        """
        GET /api/v1/payment-history/{paymentId}
        Returns the history record dict, or None if not found yet.
        """
        try:
            return await self.get(f"/api/v1/payment-history/{payment_id}")
        except ClientError as exc:
            if exc.status == 404:
                return None   # not processed yet — caller should retry
            logger.warning(
                "Unexpected error fetching history for %s: HTTP %s",
                payment_id, exc.status,
            )
            return None

    async def poll_until_ready(
        self,
        payment_id: str,
        max_attempts: int = 40,
        interval_seconds: float = 3.0,
    ) -> Optional[dict]:
        """
        Poll for a payment history record until it appears or times out.

        After the global drain wait in Phase 2.5 most records are already
        present, so the first poll attempt will usually succeed.
        The 40 × 3s = 120s safety-net timeout catches any stragglers.

        Returns the dict on success, or None on timeout.
        """
        for attempt in range(1, max_attempts + 1):
            record = await self.get_by_payment_id(payment_id)
            if record:
                logger.debug(
                    "Payment %s ready after %d poll(s)", payment_id, attempt
                )
                return record
            await asyncio.sleep(interval_seconds)

        logger.warning(
            "Payment %s not in history after %d attempts (%.0fs timeout)",
            payment_id, max_attempts, max_attempts * interval_seconds,
        )
        return None
