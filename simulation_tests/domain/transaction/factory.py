"""
Transaction Factory
===================
Builds the full list of 1,000 TransactionRecord objects:
  - N legitimate payments (varied types, currencies, customers)
  - M fraud payments seeded across three typologies

The output list is in random order (not grouped by type), which
simulates realistic concurrent submission by many customers.
"""

import random
from typing import List, Tuple

from simulation_tests.config import (
    CURRENCIES, PAYMENT_TYPES, SIM_CONFIG
)
from simulation_tests.domain.account.models import AccountRecord
from simulation_tests.domain.customer.models import CustomerRecord
from simulation_tests.domain.transaction.fraud_patterns import (
    build_fanout_payments,
    build_high_value_single_payments,
    build_layering_payments,
    build_round_trip_payments,
    build_smurfing_payments,
)
from simulation_tests.domain.transaction.models import (
    FraudLabel, PaymentRequestData, TransactionRecord
)


def _legitimate_payment(
    customer: CustomerRecord,
    account: AccountRecord,
    all_accounts: List[AccountRecord],
) -> Tuple[PaymentRequestData, FraudLabel]:
    """Single realistic TRANSFER_OUT — both source and destination always set."""
    currency = random.choice(account.open_data.currencies or ["USD"])
    amount = round(random.uniform(50.0, 5_000.0), 2)

    # Pick destination first so to_currency can be drawn from its actual currencies
    other_accounts = [a for a in all_accounts if a.account_id and a.account_id != account.account_id]
    destination = random.choice(other_accounts) if other_accounts else None
    to_currency = random.choice(destination.open_data.currencies or ["USD"]) if destination else currency

    return (
        PaymentRequestData(
            customer_id=customer.customer_id,
            amount=amount,
            from_currency=currency,
            to_currency=to_currency,
            payment_type="TRANSFER_OUT",
            source_account_id=account.account_id,
            destination_account_id=destination.account_id if destination else None,
            description="Routine payment",
        ),
        FraudLabel.LEGITIMATE,
    )


class TransactionFactory:
    """
    Builds the complete scenario list of TransactionRecords.

    Usage
    -----
    factory = TransactionFactory(seed=42)
    records = factory.build(
        customers=customers,
        accounts=accounts,
        account_by_customer={c.customer_id: a for c, a in zip(...)},
    )
    """

    def __init__(self, seed: int = SIM_CONFIG.random_seed):
        random.seed(seed)
        self._cfg = SIM_CONFIG

    def build(
        self,
        customers: List[CustomerRecord],
        accounts: List[AccountRecord],
    ) -> List[TransactionRecord]:
        """
        Assembles the full scenario list.

        Parameters
        ----------
        customers : list of CustomerRecord with customer_id set
        accounts  : list of AccountRecord with account_id set (1:1 with customers)
        """

        # Index accounts by customer_id for fast lookup
        acct_by_cust: dict = {a.customer_id: a for a in accounts if a.account_id}

        # Split population
        normal_customers = [c for c in customers if not c.is_fraud_seed and c.customer_id]
        mule_customers   = [c for c in customers if c.is_fraud_seed and c.customer_id]

        if not mule_customers:
            raise ValueError("No MULE customers found — cannot seed fraud patterns.")

        pairs: List[Tuple[PaymentRequestData, FraudLabel]] = []

        # All accounts with IDs — used as destination pool throughout
        all_accounts = [a for a in accounts if a.account_id]
        # Normal (non-mule) accounts — receiver pool for fraud patterns
        normal_accounts = [
            a for a in all_accounts if not a.is_fraud_seed
        ]

        # ----------------------------------------------------------------
        # 1. Legitimate payments
        # ----------------------------------------------------------------
        legitimate_pool = normal_customers + [
            c for c in customers if c.risk_profile == "HIGH_RISK" and c.customer_id
        ]
        for i in range(self._cfg.legitimate_count):
            customer = random.choice(legitimate_pool)
            account  = acct_by_cust.get(customer.customer_id)
            if account:
                pairs.append(_legitimate_payment(customer, account, all_accounts))

        # ----------------------------------------------------------------
        # 2. Smurfing (structuring)
        # ----------------------------------------------------------------
        smurf_sender = random.choice(mule_customers)
        smurf_account = acct_by_cust.get(smurf_sender.customer_id)
        smurf_receivers = [a for a in normal_accounts if a.account_id != (smurf_account.account_id if smurf_account else None)]
        if smurf_account and smurf_receivers:
            pairs.extend(
                build_smurfing_payments(
                    sender_customer_id=smurf_sender.customer_id,
                    sender_account_id=smurf_account.account_id,
                    count=self._cfg.fraud_smurfing_count,
                    receiver_accounts=smurf_receivers,
                )
            )

        # ----------------------------------------------------------------
        # 3. Fan-out
        # ----------------------------------------------------------------
        fanout_sender = random.choice(mule_customers)
        fanout_account = acct_by_cust.get(fanout_sender.customer_id)
        fanout_receivers = [a for a in normal_accounts if a.account_id][:self._cfg.fraud_fanout_count]

        if fanout_account and fanout_receivers:
            pairs.extend(
                build_fanout_payments(
                    sender_customer_id=fanout_sender.customer_id,
                    sender_account_id=fanout_account.account_id,
                    receiver_accounts=fanout_receivers,
                )
            )

        # ----------------------------------------------------------------
        # 4. Cross-border layering
        # ----------------------------------------------------------------
        # Need two mules: one as sender, one as intermediary
        if len(mule_customers) >= 2:
            layer_sender = mule_customers[0]
            layer_intermediate = mule_customers[1]
            layer_sender_acct = acct_by_cust.get(layer_sender.customer_id)
            layer_intermediate_acct = acct_by_cust.get(layer_intermediate.customer_id)

            # Final destination for layering hop2: a random normal account
            layer_final_accounts = [a for a in normal_accounts
                                     if a.account_id not in (
                                         layer_sender_acct.account_id if layer_sender_acct else None,
                                         layer_intermediate_acct.account_id if layer_intermediate_acct else None,
                                     )]
            if layer_sender_acct and layer_intermediate_acct and layer_final_accounts:
                # Each call produces 2 hops; call until we reach the target count
                hops_needed = self._cfg.fraud_layering_count
                while len([p for p in pairs if p[1] == FraudLabel.LAYERING]) < hops_needed:
                    final_acct = random.choice(layer_final_accounts)
                    pairs.extend(
                        build_layering_payments(
                            sender_customer_id=layer_sender.customer_id,
                            sender_account=layer_sender_acct,
                            intermediate_customer_id=layer_intermediate.customer_id,
                            intermediate_account=layer_intermediate_acct,
                            final_account=final_acct,
                        )
                    )

        # ----------------------------------------------------------------
        # 5. High-value single transfers
        # ----------------------------------------------------------------
        hv_sender = random.choice(mule_customers)
        hv_account = acct_by_cust.get(hv_sender.customer_id)
        hv_receivers = [a for a in normal_accounts if a.account_id][:self._cfg.fraud_high_value_count]
        if hv_account and hv_receivers:
            pairs.extend(
                build_high_value_single_payments(
                    sender_customer_id=hv_sender.customer_id,
                    sender_account_id=hv_account.account_id,
                    receiver_accounts=hv_receivers,
                    count=self._cfg.fraud_high_value_count,
                )
            )

        # ----------------------------------------------------------------
        # 6. Round-trip (boomerang) pairs
        # ----------------------------------------------------------------
        if len(mule_customers) >= 2:
            rt_a = mule_customers[0]
            rt_b = mule_customers[1]
            rt_acct_a = acct_by_cust.get(rt_a.customer_id)
            rt_acct_b = acct_by_cust.get(rt_b.customer_id)
            # pair_count * 2 payments; keep within budget
            pair_count = self._cfg.fraud_round_trip_count // 2
            if rt_acct_a and rt_acct_b and pair_count > 0:
                pairs.extend(
                    build_round_trip_payments(
                        customer_a_id=rt_a.customer_id,
                        account_a_id=rt_acct_a.account_id,
                        customer_b_id=rt_b.customer_id,
                        account_b_id=rt_acct_b.account_id,
                        pair_count=pair_count,
                    )
                )

        # ----------------------------------------------------------------
        # Wrap in TransactionRecord and shuffle
        # ----------------------------------------------------------------
        random.shuffle(pairs)

        records = [
            TransactionRecord(
                payment_data=payment_data,
                fraud_label=fraud_label,
                scenario_index=idx,
            )
            for idx, (payment_data, fraud_label) in enumerate(pairs)
        ]

        return records
