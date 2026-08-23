"""
Builds the PROPOSED replacement feature set for the network agent: local
ego-net structure and money-flow features, per temporal window, on the IBM AML
account graph. The global-centrality baseline (prepare_naa_features.py) scored
F1 0.05 retrained — laundering rings are small local subgraphs with no global
prominence, so this set looks at the account's immediate neighbourhood and the
tempo of money through it instead.

Features per account:
  reciprocity                fraction of counterparties transacted with in both
                             directions (2-cycles are the shortest rings)
  cycle3_count               directed 3-cycles through the account, sparse
                             diag(A^3) with hub rows/columns capped at
                             HUB_DEGREE_CAP (super-hubs are institutions, not
                             rings, and make A^2 explode)
  two_hop_out_reach          distinct accounts reachable in exactly 2 hops
                             downstream (scatter-gather widens fast)
  in_out_amount_ratio        total out / (total in + 1): pass-through accounts
                             forward what they receive
  forwarding_gap_hours       median hours between an outgoing payment and the
                             most recent incoming one (mules forward quickly),
                             capped at GAP_CAP_HOURS when a side is missing
  in_concentration           Herfindahl of incoming amounts by counterparty
  out_concentration          Herfindahl of outgoing amounts by counterparty
                             (fan-in/fan-out have low concentration)
  peak_day_share             busiest day's share of all the account's payments
                             (injected patterns are bursty)
  unique_in_counterparties   distinct senders (Neo4j's inDegree counts
  unique_out_counterparties  payments, not counterparties — these disambiguate)

Outputs: data/naa_local_features_{train,test}.parquet
"""

import pathlib
import time

import numpy as np
import pandas as pd
import scipy.sparse as sp

import prepare_ibm_aml as prep

DATA_DIR = pathlib.Path(__file__).parent / "data"

HUB_DEGREE_CAP = 200
GAP_CAP_HOURS = 168.0


def log(message: str) -> None:
    print(f"  [{time.strftime('%H:%M:%S')}] {message}", flush=True)


def herfindahl_by_counterparty(window_df: pd.DataFrame, account_col: str, counterparty_col: str) -> pd.Series:
    edge_amounts = window_df.groupby([account_col, counterparty_col])["amount_paid"].sum()
    totals = edge_amounts.groupby(level=0).sum()
    return ((edge_amounts / totals) ** 2).groupby(level=0).sum()


def forwarding_gap_hours(window_df: pd.DataFrame) -> pd.Series:
    incoming = window_df.groupby("payee")["timestamp"].apply(
        lambda s: np.sort(s.to_numpy().astype("datetime64[m]").astype(np.int64))
    )
    outgoing = window_df.groupby("payer")["timestamp"].apply(
        lambda s: np.sort(s.to_numpy().astype("datetime64[m]").astype(np.int64))
    )
    gaps = {}
    for account, out_minutes in outgoing.items():
        in_minutes = incoming.get(account)
        if in_minutes is None:
            gaps[account] = GAP_CAP_HOURS
            continue
        positions = np.searchsorted(in_minutes, out_minutes, side="right")
        valid = positions > 0
        if not valid.any():
            gaps[account] = GAP_CAP_HOURS
            continue
        deltas = (out_minutes[valid] - in_minutes[positions[valid] - 1]) / 60.0
        gaps[account] = float(min(np.median(deltas), GAP_CAP_HOURS))
    return pd.Series(gaps, name="forwarding_gap_hours")


def structural_features(edges: pd.DataFrame, accounts: pd.Index) -> pd.DataFrame:
    index = pd.Index(accounts, name="account")
    position = pd.Series(np.arange(len(index)), index=index)
    rows = position[edges["payer"]].to_numpy()
    cols = position[edges["payee"]].to_numpy()
    n = len(index)
    adjacency = sp.csr_matrix((np.ones(len(edges), dtype=np.int8), (rows, cols)), shape=(n, n))
    adjacency.data[:] = 1

    out_degree = adjacency.getnnz(axis=1)
    in_degree = adjacency.getnnz(axis=0)

    reciprocal = adjacency.multiply(adjacency.T)
    reciprocal_count = reciprocal.getnnz(axis=1)
    union = out_degree + in_degree - reciprocal_count
    reciprocity = np.divide(reciprocal_count, union, out=np.zeros(n), where=union > 0)

    hub_rows = out_degree > HUB_DEGREE_CAP
    hub_cols = in_degree > HUB_DEGREE_CAP
    capped = adjacency.copy().tolil()
    capped[hub_rows, :] = 0
    capped[:, hub_cols] = 0
    capped = capped.tocsr().astype(np.int32)

    two_hop = (capped @ capped).astype(bool).tocsr()
    two_hop_reach = two_hop.getnnz(axis=1)
    cycle3 = np.asarray(two_hop.multiply(capped.T.astype(bool)).sum(axis=1)).ravel()

    return pd.DataFrame({
        "account": index,
        "unique_in_counterparties": in_degree,
        "unique_out_counterparties": out_degree,
        "reciprocity": reciprocity,
        "cycle3_count": cycle3,
        "two_hop_out_reach": two_hop_reach,
    })


def build_window_features(window_df: pd.DataFrame, window: str) -> pd.DataFrame:
    edges = window_df.groupby(["payer", "payee"]).size().reset_index(name="weight")
    accounts = pd.Index(pd.concat([edges["payer"], edges["payee"]]).unique(), name="account")
    log(f"{window}: {len(accounts):,} accounts, {len(edges):,} distinct edges")

    log("structural features (reciprocity, 3-cycles, two-hop reach)...")
    features = structural_features(edges, accounts).set_index("account")

    log("money-flow features...")
    total_out = window_df.groupby("payer")["amount_paid"].sum()
    total_in = window_df.groupby("payee")["amount_paid"].sum()
    features["in_out_amount_ratio"] = (
            total_out.reindex(features.index).fillna(0.0)
            / (total_in.reindex(features.index).fillna(0.0) + 1.0)
    ).clip(upper=1e6)
    features["in_concentration"] = herfindahl_by_counterparty(
        window_df, "payee", "payer").reindex(features.index).fillna(0.0)
    features["out_concentration"] = herfindahl_by_counterparty(
        window_df, "payer", "payee").reindex(features.index).fillna(0.0)

    log("forwarding gaps...")
    features["forwarding_gap_hours"] = forwarding_gap_hours(
        window_df).reindex(features.index).fillna(GAP_CAP_HOURS)

    log("burstiness...")
    events = pd.concat([
        window_df[["payer", "date"]].rename(columns={"payer": "account"}),
        window_df[["payee", "date"]].rename(columns={"payee": "account"}),
    ])
    daily = events.groupby(["account", "date"]).size()
    features["peak_day_share"] = (
            daily.groupby(level=0).max() / daily.groupby(level=0).sum()
    ).reindex(features.index).fillna(0.0)

    labels = pd.concat([
        window_df.groupby("payer")["is_laundering"].max(),
        window_df.groupby("payee")["is_laundering"].max(),
    ]).groupby(level=0).max()
    features["is_laundering"] = labels.reindex(features.index).fillna(0).astype(int)

    log(f"{window} done: {features['is_laundering'].mean():.4%} illicit")
    return features.reset_index()


def main() -> None:
    print("Loading transactions...", flush=True)
    df = prep.load_transactions()
    prep.add_temporal_split(df)

    for window in ("train", "test"):
        features = build_window_features(df[df["split"] == window], window)
        features.to_parquet(DATA_DIR / f"naa_local_features_{window}.parquet", index=False)
    print("Done.", flush=True)


if __name__ == "__main__":
    main()
