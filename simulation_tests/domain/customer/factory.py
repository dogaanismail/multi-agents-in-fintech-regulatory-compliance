"""
Customer Factory
================
Generates N synthetic customers with realistic demographic data.

Risk profile distribution (mirrors SAML-D fraud rate ~0.10%):
  - 90% NORMAL  — regular retail banking customers
  -  8% HIGH_RISK — elevated behavioural flags but not fraud actors
  -  2% MULE    — designated fraud actors (used as fraud senders)
"""

import random
from datetime import date, timedelta
from typing import List
import time

from faker import Faker

from simulation_tests.config import (
    BANK_LOCATIONS, CUSTOMER_TYPES, SIM_CONFIG
)
from simulation_tests.domain.customer.models import (
    AddressData, CustomerCreateData, CustomerRecord
)

# City lists keyed by country code — makes addresses realistic
_CITIES: dict = {
    "US": ["New York", "Los Angeles", "Chicago", "Houston", "Phoenix"],
    "GB": ["London", "Manchester", "Birmingham", "Leeds", "Glasgow"],
    "DE": ["Berlin", "Hamburg", "Munich", "Frankfurt", "Cologne"],
    "FR": ["Paris", "Lyon", "Marseille", "Toulouse", "Nice"],
    "TR": ["Istanbul", "Ankara", "Izmir", "Bursa", "Antalya"],
    "JP": ["Tokyo", "Osaka", "Kyoto", "Nagoya", "Fukuoka"],
    "IN": ["Mumbai", "Delhi", "Bangalore", "Chennai", "Hyderabad"],
    "CH": ["Zurich", "Geneva", "Basel", "Bern", "Lausanne"],
    "NL": ["Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Eindhoven"],
    "IT": ["Rome", "Milan", "Naples", "Turin", "Florence"],
    "ES": ["Madrid", "Barcelona", "Valencia", "Seville", "Bilbao"],
    "AE": ["Dubai", "Abu Dhabi", "Sharjah", "Ajman", "Al Ain"],
    "MX": ["Mexico City", "Guadalajara", "Monterrey", "Puebla", "Tijuana"],
    "NG": ["Lagos", "Abuja", "Kano", "Ibadan", "Port Harcourt"],
    "PK": ["Karachi", "Lahore", "Islamabad", "Faisalabad", "Rawalpindi"],
    "MA": ["Casablanca", "Rabat", "Fes", "Marrakech", "Tangier"],
}


def _city_for(country: str) -> str:
    cities = _CITIES.get(country, ["Unknown City"])
    return random.choice(cities)


def _random_dob() -> date:
    """Random date of birth — 21 to 75 years ago."""
    days_ago = random.randint(21 * 365, 75 * 365)
    return date.today() - timedelta(days=days_ago)


def _phone() -> str:
    r"""Generate a phone number that passes ^\+?[1-9]\d{1,14}$"""
    prefix = random.choice(["+1", "+44", "+49", "+90", "+33", "+81"])
    digits = "".join(str(random.randint(0, 9)) for _ in range(9))
    return f"{prefix}{digits}"


class CustomerFactory:
    """
    Produces CustomerRecord objects with fully populated create payloads.

    Usage
    -----
    factory = CustomerFactory(seed=42)
    customers = factory.build(n=1_000)
    # customers[0].risk_profile -> "NORMAL" | "HIGH_RISK" | "MULE"
    # customers[0].is_fraud_seed -> True if MULE
    """

    def __init__(self, seed: int = SIM_CONFIG.random_seed):
        random.seed(seed)
        self._fake = Faker()
        Faker.seed(seed)
        self._counter = 0
        # Short 6-char hex suffix unique to this process run — ensures emails
        # never collide across simulation runs on the same DB.
        self._run_tag = format(int(time.time()) % 0xFFFFFF, "06x")

    def _build_one(self, index: int, risk_profile: str) -> CustomerRecord:
        country = random.choice(BANK_LOCATIONS)
        city = _city_for(country)

        create_data = CustomerCreateData(
            first_name=self._fake.first_name(),
            last_name=self._fake.last_name(),
            email=f"sim.{self._run_tag}.u{index}@aml-sim-presentation.test",  # unique per run
            phone_number=_phone(),
            date_of_birth=_random_dob(),
            nationality=country,
            customer_type=random.choices(
                CUSTOMER_TYPES, weights=[85, 15]  # 85% INDIVIDUAL
            )[0],
            address=AddressData(city=city, country_code=country),
        )

        return CustomerRecord(
            create_data=create_data,
            risk_profile=risk_profile,
            is_fraud_seed=(risk_profile == "MULE"),
        )

    def build(self, n: int) -> List[CustomerRecord]:
        """
        Build n CustomerRecord objects.

        Risk distribution:
          90% NORMAL, 8% HIGH_RISK, 2% MULE (fraud actors)
        """
        records: List[CustomerRecord] = []

        # Determine counts — minimum 2 mules so layering + round-trip always run
        mule_count = max(2, int(n * 0.02))
        high_risk_count = max(1, int(n * 0.08))
        normal_count = max(0, n - mule_count - high_risk_count)

        profiles = (
            ["NORMAL"] * normal_count
            + ["HIGH_RISK"] * high_risk_count
            + ["MULE"] * mule_count
        )
        random.shuffle(profiles)

        for i, profile in enumerate(profiles):
            records.append(self._build_one(index=i, risk_profile=profile))

        return records
