"""
Fraud Patterns
==============
Defines the three AML typologies seeded in the simulation.
Each builder returns a list of PaymentRequestData objects that together
form one fraud scenario.

The three patterns are aligned with real FATF typologies and match the
SAML-D dataset's laundering categories:

1. SMURFING (structuring)
   Multiple small WITHDRAWAL transactions from the same originator,
   each just below a reporting threshold (< $10,000). Designed to
   avoid triggering large-cash-transaction rules.

2. FAN-OUT (layering — dispersal)
   One high-value TRANSFER_OUT from a mule account spread across
   many different destination accounts in one burst. Creates a wide
   network fan that the NAA picks up via high out-degree centrality.

3. CROSS-BORDER LAYERING
   A chain of TRANSFER_OUT payments through a high-risk corridor
   (e.g. GB → TR → AE). Each hop uses a different currency to
   obscure the trail. The TPA picks up the cross-border flag;
   the NAA picks up the betweenness centrality increase.
"""

import random
from typing import List, Tuple

from simulation_tests.config import HIGH_RISK_CORRIDORS, SIM_CONFIG
from simulation_tests.domain.account.models import AccountRecord
from simulation_tests.domain.transaction.models import (
    FraudLabel, PaymentRequestData
)


def build_smurfing_payments(
    sender_customer_id: str,
    sender_account_id: str,
    count: int,
    receiver_accounts: List[AccountRecord],
) -> List[Tuple[PaymentRequestData, FraudLabel]]:
    """
    Structuring / smurfing: many small TRANSFER_OUTs from same origin
    to different receiver accounts, each just below $10k threshold.
    """
    payments = []
    if not receiver_accounts:
        return payments
    for i in range(count):
        amount = round(random.uniform(500.0, 9_500.0), 2)
        receiver = random.choice(receiver_accounts)
        currency = random.choice(["USD", "EUR"])
        data = PaymentRequestData(
            customer_id=sender_customer_id,
            amount=amount,
            from_currency=currency,
            to_currency=currency,
            payment_type="TRANSFER_OUT",
            source_account_id=sender_account_id,
            destination_account_id=receiver.account_id,
            description=f"SMURF-{i:03d} Routine transfer",
        )
        payments.append((data, FraudLabel.SMURFING))
    return payments


def build_fanout_payments(
    sender_customer_id: str,
    sender_account_id: str,
    receiver_accounts: List[AccountRecord],
) -> List[Tuple[PaymentRequestData, FraudLabel]]:
    """
    Fan-out / layering dispersal: one sender → many different receivers.
    Uses a high aggregate amount split across destinations.
    Total ~$150,000 spread across receivers.
    """
    payments = []
    n = len(receiver_accounts)
    if n == 0:
        return payments

    for i, receiver in enumerate(receiver_accounts):
        amount = round(random.uniform(3_000.0, 15_000.0), 2)
        src_currency = random.choice(["USD", "EUR"])
        dst_currency = random.choice(receiver.open_data.currencies or ["USD"])
        data = PaymentRequestData(
            customer_id=sender_customer_id,
            amount=amount,
            from_currency=src_currency,
            to_currency=dst_currency,
            payment_type="TRANSFER_OUT",
            source_account_id=sender_account_id,
            destination_account_id=receiver.account_id,
            description=f"FANOUT-{i:03d} Inter-account transfer",
        )
        payments.append((data, FraudLabel.FAN_OUT))
    return payments


def build_layering_payments(
    sender_customer_id: str,
    sender_account: AccountRecord,
    intermediate_customer_id: str,
    intermediate_account: AccountRecord,
    final_account: AccountRecord,
) -> List[Tuple[PaymentRequestData, FraudLabel]]:
    """
    Cross-border layering: two-hop chain through a high-risk corridor.

    Hop 1: sender → intermediate (e.g. GB→TR, large amount in GBP)
    Hop 2: intermediate → final destination (e.g. TR→AE, converted to AED)

    The currency change at each hop is a classic placement→layering signal.
    """
    corridor = random.choice(HIGH_RISK_CORRIDORS)
    origin_country, dest_country = corridor

    src_ccy = random.choice(sender_account.open_data.currencies or ["USD"])
    mid_ccy = random.choice(intermediate_account.open_data.currencies or ["USD"])
    final_ccy = random.choice(final_account.open_data.currencies or ["USD"])

    amount_hop1 = round(random.uniform(25_000.0, 80_000.0), 2)
    amount_hop2 = round(amount_hop1 * random.uniform(0.85, 0.99), 2)

    hop1 = PaymentRequestData(
        customer_id=sender_customer_id,
        amount=amount_hop1,
        from_currency=src_ccy,
        to_currency=mid_ccy,
        payment_type="TRANSFER_OUT",
        source_account_id=sender_account.account_id,
        destination_account_id=intermediate_account.account_id,
        description=f"LAYER-HOP1 {origin_country}→{dest_country} wire",
    )
    hop2 = PaymentRequestData(
        customer_id=intermediate_customer_id,
        amount=amount_hop2,
        from_currency=mid_ccy,
        to_currency=final_ccy,
        payment_type="TRANSFER_OUT",
        source_account_id=intermediate_account.account_id,
        destination_account_id=final_account.account_id,
        description=f"LAYER-HOP2 {dest_country} outward remittance",
    )
    return [(hop1, FraudLabel.LAYERING), (hop2, FraudLabel.LAYERING)]


def build_high_value_single_payments(
    sender_customer_id: str,
    sender_account_id: str,
    receiver_accounts: List[AccountRecord],
    count: int,
) -> List[Tuple[PaymentRequestData, FraudLabel]]:
    """
    High-value single transfer: one large cross-border TRANSFER_OUT.
    Amount range $50,000–$500,000 per payment — directly tests TPA’s
    amount-threshold learning. Uses exotic corridors to add cross-border signal.
    """
    if not receiver_accounts:
        return []

    _COUNTRY_CCY = {
        "US": "USD", "GB": "GBP", "DE": "EUR", "TR": "TRY",
        "AE": "AED", "CH": "CHF", "JP": "JPY", "NG": "NGN",
    }
    high_risk_pairs = [("GB", "AE"), ("US", "NG"), ("DE", "TR"), ("CH", "PK")]

    payments = []
    for i in range(count):
        receiver = random.choice(receiver_accounts)
        origin, dest = random.choice(high_risk_pairs)
        src_ccy = random.choice(["USD", "EUR"])
        dst_ccy = random.choice(receiver.open_data.currencies or ["USD"])
        amount = round(random.uniform(50_000.0, 500_000.0), 2)
        data = PaymentRequestData(
            customer_id=sender_customer_id,
            amount=amount,
            from_currency=src_ccy,
            to_currency=dst_ccy,
            payment_type="TRANSFER_OUT",
            source_account_id=sender_account_id,
            destination_account_id=receiver.account_id,
            description=f"HVS-{i:03d} {origin}→{dest} large value wire",
        )
        payments.append((data, FraudLabel.HIGH_VALUE_SINGLE))
    return payments


def build_round_trip_payments(
    customer_a_id: str,
    account_a_id: str,
    customer_b_id: str,
    account_b_id: str,
    pair_count: int,
) -> List[Tuple[PaymentRequestData, FraudLabel]]:
    """
    Round-trip / boomerang: A→B immediately followed by B→A.
    The returned amount is 95–99% of the original (small fee deducted).
    NAA detects this as a closed cycle (betweenness / cycle centrality).
    TPA detects the rapid back-transfer velocity.
    """
    payments = []
    for i in range(pair_count):
        currency = random.choice(["USD", "EUR"])
        amount_out = round(random.uniform(5_000.0, 50_000.0), 2)
        amount_back = round(amount_out * random.uniform(0.95, 0.99), 2)  # slight ‘fee’

        hop_out = PaymentRequestData(
            customer_id=customer_a_id,
            amount=amount_out,
            from_currency=currency,
            to_currency=currency,
            payment_type="TRANSFER_OUT",
            source_account_id=account_a_id,
            destination_account_id=account_b_id,
            description=f"RT-{i:03d}-OUT outward leg",
        )
        hop_back = PaymentRequestData(
            customer_id=customer_b_id,
            amount=amount_back,
            from_currency=currency,
            to_currency=currency,
            payment_type="TRANSFER_OUT",
            source_account_id=account_b_id,
            destination_account_id=account_a_id,
            description=f"RT-{i:03d}-BACK return leg",
        )
        payments.append((hop_out,  FraudLabel.ROUND_TRIP))
        payments.append((hop_back, FraudLabel.ROUND_TRIP))
    return payments
