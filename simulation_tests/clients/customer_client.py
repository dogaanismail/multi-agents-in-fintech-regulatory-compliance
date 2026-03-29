"""
Customer Service Client
=======================
Wraps POST /api/v1/customers (customer-svc, port 5001).
"""

from simulation_tests.clients.base_client import BaseClient
from simulation_tests.config import ServiceURLs
from simulation_tests.domain.customer.models import CustomerCreateData, CustomerRecord


class CustomerClient(BaseClient):
    """Async client for customer-svc (port 5001)."""

    def __init__(self):
        super().__init__(base_url=ServiceURLs.CUSTOMER_SVC)

    async def create_customer(self, create_data: CustomerCreateData) -> str:
        """
        POST /api/v1/customers

        Returns the UUID string of the newly created customer.
        """
        payload = create_data.to_api_payload()
        response = await self.post("/api/v1/customers", payload)
        # Response contains {"id": "<uuid>", "firstName": ..., ...}
        return str(response["id"])

    async def create_batch(self, customer_records) -> list:
        """
        Create multiple customers sequentially (avoids email uniqueness races).

        Returns a list of CustomerRecord objects with their server-assigned IDs set.
        """
        import logging
        logger = logging.getLogger(__name__)
        created = []
        for i, record in enumerate(customer_records):
            try:
                customer_id = await self.create_customer(record.create_data)
                record.customer_id = customer_id
                created.append(record)
                if (i + 1) % 100 == 0:
                    logger.info("Customer registration progress: %d / %d", i + 1, len(customer_records))
            except Exception as exc:
                logger.warning("Customer creation failed (index %d): %s", i, exc)
        return created

    async def get_customer(self, customer_id: str) -> dict:
        """GET /api/v1/customers/{id}"""
        return await self.get(f"/api/v1/customers/{customer_id}")
