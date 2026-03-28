"""
Account Factory
===============
Opens one (or more) accounts per customer via account-svc.

Account type and currencies are varied to reflect realistic
portfolio diversity. Fraud-seed (MULE) customers receive
accounts in high-risk bank locations to support layering patterns.
"""

import random
from typing import List

from simulation_tests.config import (
    ACCOUNT_TYPES, BANK_LOCATIONS, CURRENCIES, SIM_CONFIG,
    HIGH_RISK_CORRIDORS
)
from simulation_tests.domain.account.models import AccountRecord, OpenAccountData
from simulation_tests.domain.customer.models import CustomerRecord

# Locations associated with cross-border layering
_HIGH_RISK_LOCS = list({loc for pair in HIGH_RISK_CORRIDORS for loc in pair})
_NORMAL_LOCS = [l for l in BANK_LOCATIONS if l not in _HIGH_RISK_LOCS]


def _pick_currencies(n: int = 2) -> List[str]:
    """Pick 1–3 currencies. USD/EUR always included for normal accounts."""
    base = ["USD", "EUR"]
    extra = random.sample([c for c in CURRENCIES if c not in base], k=min(n - 2, len(CURRENCIES) - 2))
    return base + extra


class AccountFactory:
    """
    Builds OpenAccountData for each CustomerRecord.

    Usage
    -----
    factory = AccountFactory(seed=42)
    account_records = factory.build_for_customers(customers)
    """

    def __init__(self, seed: int = SIM_CONFIG.random_seed):
        random.seed(seed)

    def _build_one(self, customer: CustomerRecord) -> AccountRecord:

        # MULE customers get accounts in high-risk corridors;
        # USD+EUR are always included so cross-pattern transfers work,
        # plus one exotic high-risk currency for layering corridor signals.
        _EXOTIC = [c for c in CURRENCIES if c not in ("USD", "EUR")]
        if customer.is_fraud_seed and _HIGH_RISK_LOCS:
            bank_location = random.choice(_HIGH_RISK_LOCS)
            exotic = random.choice(_EXOTIC) if _EXOTIC else "GBP"
            currencies = ["USD", "EUR", exotic]
        else:
            bank_location = random.choice(_NORMAL_LOCS or BANK_LOCATIONS)
            currencies = _pick_currencies(n=random.randint(2, 3))

        open_data = OpenAccountData(
            customer_id=customer.customer_id or "UNKNOWN",
            account_type=random.choices(
                ACCOUNT_TYPES, weights=[60, 30, 10]  # mostly CHECKING
            )[0],
            bank_location=bank_location,
            currencies=currencies,
        )

        return AccountRecord(
            open_data=open_data,
            customer_id=customer.customer_id,
            is_fraud_seed=customer.is_fraud_seed,
        )

    def build_for_customers(
        self, customers: List[CustomerRecord]
    ) -> List[AccountRecord]:
        """
        Create one AccountRecord per customer.
        Requires customers to already have customer_id assigned
        (i.e. SetupRunner must have called the API first).
        """
        accounts: List[AccountRecord] = []
        for customer in customers:
            if not customer.customer_id:
                continue  # skip if customer creation failed
            account = self._build_one(customer)
            accounts.append(account)
        return accounts
