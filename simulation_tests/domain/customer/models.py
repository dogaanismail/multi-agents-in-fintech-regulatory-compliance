"""
Customer Domain Models
======================
Python dataclasses that mirror the Java CustomerCreateRequest and
CustomerResponse. These are used internally by the factory and clients.
"""

from dataclasses import dataclass, field
from typing import Optional
from datetime import date
import uuid


@dataclass
class AddressData:
    """Mirrors Java AddressRequest — city + 2-letter country code."""
    city: str
    country_code: str  # ISO 3166-1 alpha-2, e.g. "GB", "US", "TR"


@dataclass
class CustomerCreateData:
    """
    Mirrors Java CustomerCreateRequest.
    Used to POST /api/v1/customers.
    """
    first_name: str
    last_name: str
    email: str
    phone_number: str          # must match ^\+?[1-9]\d{1,14}$
    date_of_birth: date        # must be in the past
    nationality: str           # 2-letter ISO country code
    customer_type: str         # "INDIVIDUAL" | "BUSINESS"
    address: AddressData
    middle_name: Optional[str] = None

    def to_api_payload(self) -> dict:
        """Serialise to the exact JSON body expected by customer-svc."""
        payload = {
            "firstName": self.first_name,
            "lastName": self.last_name,
            "email": self.email,
            "phoneNumber": self.phone_number,
            "dateOfBirth": self.date_of_birth.isoformat(),
            "nationality": self.nationality,
            "customerType": self.customer_type,
            "address": {
                "city": self.address.city,
                "countryCode": self.address.country_code,
            },
        }
        if self.middle_name:
            payload["middleName"] = self.middle_name
        return payload


@dataclass
class CustomerRecord:
    """
    Holds both the original create data and the UUID returned by the API.
    This is the runtime state object passed through the simulation pipeline.
    """
    create_data: CustomerCreateData
    customer_id: Optional[str] = None    # UUID assigned by customer-svc
    risk_profile: str = "NORMAL"         # "NORMAL" | "HIGH_RISK" | "MULE"
    is_fraud_seed: bool = False          # True if this customer is a fraud actor

    @property
    def display_name(self) -> str:
        return f"{self.create_data.first_name} {self.create_data.last_name}"
