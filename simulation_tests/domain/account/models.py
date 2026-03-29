"""
Account Domain Models
=====================
Mirrors Java OpenAccountRequest and AccountResponse.
"""

from dataclasses import dataclass, field
from typing import Optional, List


@dataclass
class OpenAccountData:
    """
    Mirrors Java OpenAccountRequest.
    Used to POST /api/v1/accounts/open-account.
    """
    customer_id: str           # UUID string
    account_type: str          # "CHECKING" | "SAVINGS" | "DEPOSIT"
    bank_location: str         # 2-letter country code, e.g. "GB"
    currencies: List[str]      # e.g. ["USD", "EUR"]

    def to_api_payload(self) -> dict:
        return {
            "customerId": self.customer_id,
            "accountType": self.account_type,
            "bankLocation": self.bank_location,
            "currencies": self.currencies,
        }


@dataclass
class AccountRecord:
    """
    Runtime account record — holds open request + API-assigned IDs.
    """
    open_data: OpenAccountData
    account_id: Optional[str] = None    # UUID from account-svc
    customer_id: Optional[str] = None   # denormalised for quick lookup
    is_fraud_seed: bool = False         # True if account belongs to a fraud actor
