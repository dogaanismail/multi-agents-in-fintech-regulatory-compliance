"""
Maps the IBM AML HI-Small dataset (Altman et al., NeurIPS 2023) onto the feature
schemas of the three detection agents, for cross-dataset generalization
experiments.

Outputs (parquet, under data/):
  tpa_transactions.parquet  transaction-level features + label, with temporal split
  cra_accounts.parquet      per-account aggregates (19 CRA features) + label
  naa_edges.parquet         directed edge list + per-account labels for graph features

Mapping decisions (all counted and reported at the end):
  - Currencies map IBM names -> ISO codes the model was trained on; currencies the
    model never saw (Yuan, Ruble, Bitcoin, ...) stay unmapped and one-hot to zero
    vectors (the preprocessor uses handle_unknown='ignore').
  - IBM has bank codes, not countries: both location features are set to the
    unseen constant 'XX' and therefore contribute nothing. This is a deliberate,
    reported schema gap, not an attempt to fabricate locations.
  - Payment formats: Wire -> 'Cross-border', Cash -> 'Cash Deposit' (both
    approximations), Bitcoin/Reinvestment stay unmapped.
  - CRA's cross_border_ratio and unique_receiver_countries are proxied with
    cross-bank equivalents; amount_consistency uses std/mean since the original
    training definition is not recoverable.
"""

import json
import pathlib

import numpy as np
import pandas as pd

DATA_DIR = pathlib.Path(__file__).parent / "data"
CSV = DATA_DIR / "HI-Small_Trans.csv"

CURRENCY_MAP = {
    "US Dollar": "USD", "Euro": "EUR", "UK Pound": "GBP", "Yen": "JPY",
    "Swiss Franc": "CHF", "Rupee": "INR", "Mexican Peso": "MXN",
    "Yuan": "CNY", "Ruble": "RUB", "Australian Dollar": "AUD",
    "Canadian Dollar": "CAD", "Brazil Real": "BRL", "Saudi Riyal": "SAR",
    "Shekel": "ILS", "Bitcoin": "BTC",
}
KNOWN_CURRENCIES = {"AED", "ALL", "CHF", "EUR", "GBP", "INR", "JPY", "MAD",
                    "MXN", "NGN", "PKR", "TRY", "USD"}

PAYMENT_FORMAT_MAP = {
    "Cheque": "Cheque", "Credit Card": "Credit card", "ACH": "ACH",
    "Wire": "Cross-border", "Cash": "Cash Deposit",
    "Bitcoin": "Bitcoin", "Reinvestment": "Reinvestment",
}
KNOWN_PAYMENT_TYPES = {"ACH", "Cash Deposit", "Cash Withdrawal", "Cheque",
                       "Credit card", "Cross-border", "Debit card"}


def load_transactions() -> pd.DataFrame:
    df = pd.read_csv(
        CSV,
        header=0,
        names=["timestamp", "from_bank", "from_account", "to_bank", "to_account",
               "amount_received", "receiving_currency", "amount_paid",
               "payment_currency", "payment_format", "is_laundering"],
        dtype={"from_bank": str, "from_account": str, "to_bank": str,
               "to_account": str},
    )
    df["timestamp"] = pd.to_datetime(df["timestamp"], format="%Y/%m/%d %H:%M")
    df["date"] = df["timestamp"].dt.date
    df["hour"] = df["timestamp"].dt.hour
    df["day_of_week"] = df["timestamp"].dt.dayofweek
    df["payer"] = df["from_bank"] + "_" + df["from_account"]
    df["payee"] = df["to_bank"] + "_" + df["to_account"]
    return df


def add_temporal_split(df: pd.DataFrame) -> pd.Timestamp:
    cutoff = df["timestamp"].quantile(0.8)
    df["split"] = np.where(df["timestamp"] <= cutoff, "train", "test")
    return cutoff


def build_tpa_table(df: pd.DataFrame) -> pd.DataFrame:
    tpa = pd.DataFrame({
        "Time": df["timestamp"].dt.strftime("%H:%M:%S"),
        "Date": df["timestamp"].dt.strftime("%Y-%m-%d"),
        "Amount": df["amount_paid"],
        "Payment_currency": df["payment_currency"].map(CURRENCY_MAP).fillna("XXX"),
        "Received_currency": df["receiving_currency"].map(CURRENCY_MAP).fillna("XXX"),
        "Sender_bank_location": "XX",
        "Receiver_bank_location": "XX",
        "Payment_type": df["payment_format"].map(PAYMENT_FORMAT_MAP).fillna("Other"),
        "is_laundering": df["is_laundering"],
        "split": df["split"],
    })
    return tpa


def build_cra_table(df: pd.DataFrame) -> pd.DataFrame:
    g = df.groupby("payer")
    amounts = g["amount_paid"]

    accounts = pd.DataFrame({
        "transaction_count": g.size(),
        "total_amount": amounts.sum(),
        "avg_amount": amounts.mean(),
        "median_amount": amounts.median(),
        "max_amount": amounts.max(),
        "min_amount": amounts.min(),
        "std_amount": amounts.std().fillna(0.0),
        "active_days": g["date"].nunique(),
        "cross_border_ratio": g.apply(lambda x: (x["from_bank"] != x["to_bank"]).mean(), include_groups=False),
        "cash_transaction_ratio": g.apply(lambda x: (x["payment_format"] == "Cash").mean(), include_groups=False),
        "large_transaction_ratio": g.apply(lambda x: (x["amount_paid"] > 10_000).mean(), include_groups=False),
        "unique_receivers": g["payee"].nunique(),
        "unique_receiver_countries": g["to_bank"].nunique(),
        "night_transaction_ratio": g.apply(lambda x: (x["hour"] < 6).mean(), include_groups=False),
        "weekend_transaction_ratio": g.apply(lambda x: (x["day_of_week"] >= 5).mean(), include_groups=False),
        "unique_currencies": g["payment_currency"].nunique(),
        "is_laundering": g["is_laundering"].max(),
    })
    accounts["transactions_per_day"] = accounts["transaction_count"] / accounts["active_days"]
    accounts["amount_consistency"] = (accounts["std_amount"] / accounts["avg_amount"]).fillna(0.0)
    accounts["receiver_diversity"] = accounts["unique_receivers"] / accounts["transaction_count"]
    return accounts.reset_index()


def build_naa_edges(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame]:
    edges = df.groupby(["payer", "payee"]).size().reset_index(name="weight")
    labels = pd.concat([
        df.groupby("payer")["is_laundering"].max().rename("is_laundering"),
        df.groupby("payee")["is_laundering"].max().rename("is_laundering"),
    ]).groupby(level=0).max().rename_axis("account").reset_index()
    return edges, labels


def main() -> None:
    print("Loading transactions...")
    df = load_transactions()
    cutoff = add_temporal_split(df)
    print(f"  {len(df):,} transactions, {df['is_laundering'].mean():.4%} illicit")
    print(f"  temporal cutoff (80%): {cutoff}")

    tpa = build_tpa_table(df)
    tpa.to_parquet(DATA_DIR / "tpa_transactions.parquet", index=False)
    ccy_coverage = tpa["Payment_currency"].isin(KNOWN_CURRENCIES).mean()
    fmt_coverage = tpa["Payment_type"].isin(KNOWN_PAYMENT_TYPES).mean()
    print(f"TPA table: {len(tpa):,} rows | currency coverage {ccy_coverage:.1%} | "
          f"payment-type coverage {fmt_coverage:.1%} | locations: 0% (schema gap)")

    print("Aggregating accounts (this is the slow step)...")
    cra = build_cra_table(df)
    cra.to_parquet(DATA_DIR / "cra_accounts.parquet", index=False)
    print(f"CRA table: {len(cra):,} accounts | {cra['is_laundering'].mean():.4%} illicit")

    edges, labels = build_naa_edges(df)
    edges.to_parquet(DATA_DIR / "naa_edges.parquet", index=False)
    labels.to_parquet(DATA_DIR / "naa_account_labels.parquet", index=False)
    print(f"NAA graph: {labels.shape[0]:,} accounts, {len(edges):,} distinct edges")

    stats = {
        "transactions": len(df),
        "illicit_rate": float(df["is_laundering"].mean()),
        "temporal_cutoff": str(cutoff),
        "tpa_currency_coverage": float(ccy_coverage),
        "tpa_payment_type_coverage": float(fmt_coverage),
        "accounts": int(cra.shape[0]),
        "account_illicit_rate": float(cra["is_laundering"].mean()),
    }
    json.dump(stats, open(DATA_DIR / "prepare_stats.json", "w"), indent=2)
    print("Done.")


if __name__ == "__main__":
    main()
