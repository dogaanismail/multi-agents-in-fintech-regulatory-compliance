"""
Account Service Client
======================
Wraps POST /api/v1/accounts/open-account (account-svc, port 5002).
"""

from simulation_tests.clients.base_client import BaseClient
from simulation_tests.config import ServiceURLs
from simulation_tests.domain.account.models import AccountRecord, OpenAccountData


class AccountClient(BaseClient):
    """Async client for account-svc (port 5002)."""

    def __init__(self):
        super().__init__(base_url=ServiceURLs.ACCOUNT_SVC)

    async def open_account(self, open_data: OpenAccountData) -> str:
        """
        POST /api/v1/accounts/open-account

        Returns the UUID string of the newly opened account.
        """
        payload = open_data.to_api_payload()
        response = await self.post("/api/v1/accounts/open-account", payload)
        # Response contains {"id": "<uuid>", "customerId": ..., ...}
        return str(response["id"])

    async def open(self, account_record: AccountRecord) -> "AccountRecord":
        """
        Convenience wrapper: accepts an AccountRecord, calls open_account,
        sets account_record.account_id and returns the record.
        """
        account_id = await self.open_account(account_record.open_data)
        account_record.account_id = account_id
        return account_record

    async def get_accounts_by_customer(self, customer_id: str) -> list:
        """GET /api/v1/accounts/customer/{customerId}"""
        return await self.get(f"/api/v1/accounts/customer/{customer_id}")

    async def get_balances(self, account_id: str) -> list:
        """GET /api/v1/accounts/{id}/balances"""
        return await self.get(f"/api/v1/accounts/{account_id}/balances")
