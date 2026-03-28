"""
Payment Service Client
======================
Wraps POST /api/v1/payments/request (payment-svc, port 5003).
"""

from simulation_tests.clients.base_client import BaseClient
from simulation_tests.config import ServiceURLs
from simulation_tests.domain.transaction.models import PaymentRequestData


class PaymentClient(BaseClient):
    """Async client for payment-svc (port 5003)."""

    def __init__(self):
        super().__init__(base_url=ServiceURLs.PAYMENT_SVC)

    async def submit_payment(
        self, payment_data: PaymentRequestData
    ) -> dict:
        """
        POST /api/v1/payments/request

        Returns the response body dict on success.
        Raises ClientError on HTTP errors, Exception on network errors.
        The caller (simulation_runner._fire_single) handles its own timing.
        """
        payload = payment_data.to_api_payload()
        return await self.post("/api/v1/payments/request", payload)
