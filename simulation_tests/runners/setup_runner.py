"""
Setup Runner
============
Phase 1 of the simulation: create all synthetic customers and accounts.

This is the ONLY phase that calls customer-svc and account-svc directly.
Everything after this goes through payment-svc → Kafka → downstream.

Design:
  - Customers are created sequentially (avoids DB constraint races on email)
  - Accounts are created concurrently in batches (no uniqueness constraints)
  - Progress is logged every 100 items
  - Failed items are skipped, not retried (simulation proceeds with survivors)

Returns a SimulationState object with all created customers + accounts,
ready to be passed to the simulation_runner.
"""

import asyncio
import logging
import time
from dataclasses import dataclass, field
from typing import Dict, List

from simulation_tests.clients.account_client import AccountClient
from simulation_tests.clients.customer_client import CustomerClient
from simulation_tests.config import SIM_CONFIG
from simulation_tests.domain.account.factory import AccountFactory
from simulation_tests.domain.account.models import AccountRecord
from simulation_tests.domain.customer.factory import CustomerFactory
from simulation_tests.domain.customer.models import CustomerRecord

logger = logging.getLogger(__name__)


@dataclass
class SimulationState:
    """
    Shared state passed between runners.
    Populated by SetupRunner, consumed by SimulationRunner and ResultCollector.
    """
    customers: List[CustomerRecord] = field(default_factory=list)
    accounts: List[AccountRecord] = field(default_factory=list)

    # account_by_customer[customer_id] = AccountRecord
    account_by_customer: Dict[str, AccountRecord] = field(default_factory=dict)

    setup_duration_seconds: float = 0.0
    simulation_duration_seconds: float = 0.0

    @property
    def customer_count(self) -> int:
        return len(self.customers)

    @property
    def account_count(self) -> int:
        return len(self.accounts)

    @property
    def mule_customers(self) -> List[CustomerRecord]:
        return [c for c in self.customers if c.is_fraud_seed]


async def _open_accounts_concurrently(
    account_records: List[AccountRecord],
    batch_size: int = 50,
) -> List[AccountRecord]:
    """Open accounts in concurrent batches — safe because no email uniqueness."""
    opened: List[AccountRecord] = []

    async with AccountClient() as client:
        for i in range(0, len(account_records), batch_size):
            batch = account_records[i : i + batch_size]
            tasks = [asyncio.create_task(client.open(rec)) for rec in batch]
            results = await asyncio.gather(*tasks, return_exceptions=True)

            for rec, result in zip(batch, results):
                if isinstance(result, Exception):
                    logger.warning("Account open failed: %s", result)
                elif result is not None:
                    opened.append(rec)

            logger.info(
                "Account setup progress: %d / %d",
                min(i + batch_size, len(account_records)),
                len(account_records),
            )

    return opened


class SetupRunner:
    """
    Creates N customers and their accounts before the simulation begins.

    Usage (async):
        state = await SetupRunner().run()
    """

    def __init__(self):
        self._cfg = SIM_CONFIG
        self._customer_factory = CustomerFactory(seed=self._cfg.random_seed)
        self._account_factory = AccountFactory(seed=self._cfg.random_seed)

    async def run(self) -> SimulationState:
        state = SimulationState()
        t_start = time.perf_counter()

        # ------------------------------------------------------------------
        # 1. Build customer data objects (no I/O yet)
        # ------------------------------------------------------------------
        logger.info(
            "Building %d synthetic customers...", self._cfg.num_customers
        )
        customer_records = self._customer_factory.build(self._cfg.num_customers)

        # ------------------------------------------------------------------
        # 2. Register customers with customer-svc (sequential — avoids
        #    email uniqueness constraint races)
        # ------------------------------------------------------------------
        logger.info("Registering customers with customer-svc (port 5001)...")
        async with CustomerClient() as client:
            created_customers = await client.create_batch(customer_records)

        state.customers = created_customers
        logger.info(
            "%d / %d customers created successfully.",
            len(created_customers), self._cfg.num_customers,
        )

        # Allow customer-svc DB transactions to fully commit before
        # account-svc's Feign calls verify the customer IDs.
        logger.info("Waiting 2s for customer-svc to settle...")
        await asyncio.sleep(2)

        if not created_customers:
            raise RuntimeError(
                "Zero customers were created. "
                "Is customer-svc running on port 5001?"
            )

        # ------------------------------------------------------------------
        # 3. Build account data objects (needs customer_ids from step 2)
        # ------------------------------------------------------------------
        logger.info("Opening accounts with account-svc (port 5002)...")
        account_records = self._account_factory.build_for_customers(created_customers)

        # ------------------------------------------------------------------
        # 4. Open accounts concurrently (no uniqueness constraint)
        # ------------------------------------------------------------------
        opened_accounts = await _open_accounts_concurrently(
            account_records, batch_size=50
        )
        state.accounts = opened_accounts
        state.account_by_customer = {
            a.customer_id: a for a in opened_accounts if a.account_id
        }

        logger.info(
            "%d / %d accounts opened successfully.",
            len(opened_accounts), len(account_records),
        )

        state.setup_duration_seconds = time.perf_counter() - t_start
        logger.info(
            "Setup complete in %.1f seconds.", state.setup_duration_seconds
        )

        return state
