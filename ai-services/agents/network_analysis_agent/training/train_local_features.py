"""
Trains the network analysis agent's CatBoost model on SAML-D with the local
ego-net / money-flow feature set that replaced global centralities.

Why the replacement: cross-dataset experiments (simulation_tests/datasets/
ibm_aml/naa_local_experiments.py) showed local features double F1 over the
centrality set and multiply cycle-ring recall by six — laundering rings are
small local subgraphs with no global prominence. The feature computation here
is a line-for-line port of prepare_naa_local_features.py from that harness,
applied to the SAML-D transaction log the deployed agents are trained on.

Protocol mirrors the harness: temporal 80/20 split, features built per window
so no test-window structure leaks into training, metrics reported on the test
window only. Artifacts overwrite trained_models/ in place:
  network_analysis_catboost_model.pkl / _scaler.pkl / _metadata.json

SAML-D contains zero directed 3-cycles, so a model trained on it alone gives
cycle3_count no weight at all. The optional --augment flag mixes in a random
sample of IBM AML account-level features (built by prepare_naa_local_features
in the harness), whose graph does contain cycles (71% of accounts), so the
cycle feature gets learnable variance across both classes. A uniform random
sample is deliberate: sampling only cycle-bearing accounts would teach the
model "augmented row = cycle = whatever those labels say" instead of the
cycle's real, context-dependent meaning.

Run from this directory with the agent's pinned library versions:
  python train_local_features.py [path-to-SAML-D-csv]
      [--augment path/to/naa_local_features_train.parquet]
      [--augment-sample 200000]
"""

import argparse
import json
import pathlib
import pickle
import time
from datetime import datetime, timezone

import numpy as np
import pandas as pd
import scipy.sparse as sp
from catboost import CatBoostClassifier
from sklearn.metrics import (average_precision_score, confusion_matrix,
                             f1_score, precision_score, recall_score,
                             roc_auc_score)
from sklearn.preprocessing import StandardScaler

HERE = pathlib.Path(__file__).parent
MODELS_DIR = HERE.parent / "trained_models"
DEFAULT_CSV = HERE.parents[3] / "data" / "Anti Money Laundering Transaction Data (SAML-D).csv"

FEATURE_NAMES = [
    "unique_in_counterparties",
    "unique_out_counterparties",
    "reciprocity",
    "cycle3_count",
    "two_hop_out_reach",
    "in_out_amount_ratio",
    "in_concentration",
    "out_concentration",
    "forwarding_gap_hours",
    "peak_day_share",
]

HUB_DEGREE_CAP = 200
GAP_CAP_HOURS = 168.0
RATIO_CAP = 1e6

CATBOOST_PARAMS = dict(
    iterations=300,
    depth=8,
    learning_rate=0.1,
    random_seed=42,
    verbose=50,
)


def log(message: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] {message}", flush=True)


def load_transactions(csv_path: pathlib.Path) -> pd.DataFrame:
    df = pd.read_csv(
        csv_path,
        usecols=["Time", "Date", "Sender_account", "Receiver_account",
                 "Amount", "Is_laundering"],
        dtype={"Sender_account": str, "Receiver_account": str},
    )
    df["timestamp"] = pd.to_datetime(df["Date"] + " " + df["Time"], format="%Y-%m-%d %H:%M:%S")
    df["date"] = df["timestamp"].dt.date
    df = df.rename(columns={
        "Sender_account": "payer",
        "Receiver_account": "payee",
        "Amount": "amount_paid",
        "Is_laundering": "is_laundering",
    })
    cutoff = df["timestamp"].quantile(0.8)
    df["split"] = np.where(df["timestamp"] <= cutoff, "train", "test")
    return df


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

    features = structural_features(edges, accounts).set_index("account")

    total_out = window_df.groupby("payer")["amount_paid"].sum()
    total_in = window_df.groupby("payee")["amount_paid"].sum()
    features["in_out_amount_ratio"] = (
            total_out.reindex(features.index).fillna(0.0)
            / (total_in.reindex(features.index).fillna(0.0) + 1.0)
    ).clip(upper=RATIO_CAP)
    features["in_concentration"] = herfindahl_by_counterparty(
        window_df, "payee", "payer").reindex(features.index).fillna(0.0)
    features["out_concentration"] = herfindahl_by_counterparty(
        window_df, "payer", "payee").reindex(features.index).fillna(0.0)
    features["forwarding_gap_hours"] = forwarding_gap_hours(
        window_df).reindex(features.index).fillna(GAP_CAP_HOURS)

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

    log(f"{window} features done: {features['is_laundering'].mean():.4%} illicit accounts")
    return features.reset_index()


def evaluate(y_true: np.ndarray, y_score: np.ndarray, threshold: float) -> dict:
    y_pred = (y_score >= threshold).astype(int)
    tn, fp, fn, tp = confusion_matrix(y_true, y_pred).ravel()
    return {
        "roc_auc": float(roc_auc_score(y_true, y_score)),
        "pr_auc": float(average_precision_score(y_true, y_score)),
        "threshold": threshold,
        "precision": float(precision_score(y_true, y_pred, zero_division=0)),
        "recall": float(recall_score(y_true, y_pred, zero_division=0)),
        "f1_score": float(f1_score(y_true, y_pred, zero_division=0)),
        "confusion": {"tp": int(tp), "fp": int(fp), "fn": int(fn), "tn": int(tn)},
    }


def load_augmentation_sample(parquet_path: pathlib.Path, sample_size: int) -> pd.DataFrame:
    augmentation = pd.read_parquet(parquet_path)[FEATURE_NAMES + ["is_laundering"]]
    if len(augmentation) > sample_size:
        augmentation = augmentation.sample(n=sample_size, random_state=42)
    cycle_share = (augmentation["cycle3_count"] > 0).mean()
    log(f"augmentation: {len(augmentation):,} accounts from {parquet_path.name}, "
        f"{cycle_share:.1%} with cycles, {augmentation['is_laundering'].mean():.4%} illicit")
    return augmentation


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("csv", nargs="?", type=pathlib.Path, default=DEFAULT_CSV)
    parser.add_argument("--augment", type=pathlib.Path, default=None)
    parser.add_argument("--augment-sample", type=int, default=200_000)
    args = parser.parse_args()

    csv_path = args.csv
    log(f"Loading SAML-D from {csv_path}")
    df = load_transactions(csv_path)
    log(f"{len(df):,} transactions, {df['is_laundering'].mean():.4%} illicit")

    train = build_window_features(df[df["split"] == "train"], "train")
    test = build_window_features(df[df["split"] == "test"], "test")

    augmentation_info = None
    if args.augment:
        augmentation = load_augmentation_sample(args.augment, args.augment_sample)
        augmentation_info = {
            "source": str(args.augment),
            "accounts": int(len(augmentation)),
            "cycle_share": float((augmentation["cycle3_count"] > 0).mean()),
            "illicit_rate": float(augmentation["is_laundering"].mean()),
        }
        train = pd.concat([train, augmentation], ignore_index=True).sample(frac=1.0, random_state=42)

    scaler = StandardScaler().fit(train[FEATURE_NAMES])
    y_train = train["is_laundering"].to_numpy()
    scale_pos_weight = float((y_train == 0).sum() / max(1, (y_train == 1).sum()))
    log(f"Training CatBoost (scale_pos_weight={scale_pos_weight:.1f})")
    model = CatBoostClassifier(scale_pos_weight=scale_pos_weight, **CATBOOST_PARAMS)
    model.fit(scaler.transform(train[FEATURE_NAMES]), y_train)

    test_scores = model.predict_proba(scaler.transform(test[FEATURE_NAMES]))[:, 1]
    performance = evaluate(test["is_laundering"].to_numpy(), test_scores, threshold=0.5)
    log(f"Test window: ROC-AUC {performance['roc_auc']:.4f}  "
        f"PR-AUC {performance['pr_auc']:.4f}  F1 {performance['f1_score']:.4f}  "
        f"P {performance['precision']:.4f}  R {performance['recall']:.4f}")
    log("feature importances:")
    for name, importance in sorted(zip(FEATURE_NAMES, model.get_feature_importance()),
                                   key=lambda pair: -pair[1]):
        log(f"  {name:<28} {importance:6.2f}")

    MODELS_DIR.mkdir(exist_ok=True)
    pickle.dump(model, open(MODELS_DIR / "network_analysis_catboost_model.pkl", "wb"))
    pickle.dump(scaler, open(MODELS_DIR / "network_analysis_catboost_scaler.pkl", "wb"))

    metadata = {
        "model_info": {
            "name": "CatBoost",
            "type": "Local Ego-net Money-flow Anomaly Detector",
            "created_at": datetime.now(timezone.utc).isoformat(),
            "version": "3.1_cycle_augmented" if augmentation_info else "3.0_local_features",
        },
        "augmentation": augmentation_info,
        "config": {
            "data_path": str(csv_path),
            "temporal_split": 0.8,
            "hub_degree_cap": HUB_DEGREE_CAP,
            "forwarding_gap_cap_hours": GAP_CAP_HOURS,
            "catboost_params": {k: v for k, v in CATBOOST_PARAMS.items() if k != "verbose"},
            "scale_pos_weight": scale_pos_weight,
        },
        "training_data": {
            "transactions": int(len(df)),
            "train_accounts": int(len(train)),
            "test_accounts": int(len(test)),
            "train_illicit_rate": float(train["is_laundering"].mean()),
            "test_illicit_rate": float(test["is_laundering"].mean()),
        },
        "features": {
            "count": len(FEATURE_NAMES),
            "list": FEATURE_NAMES,
            "note": "Local ego-net + money-flow; replaced global centralities "
                    "after cross-dataset evidence (see simulation_tests/datasets/ibm_aml)",
        },
        "performance": performance,
    }
    json.dump(metadata, open(MODELS_DIR / "network_analysis_catboost_metadata.json", "w"), indent=2)
    log(f"Artifacts written to {MODELS_DIR}")


if __name__ == "__main__":
    main()
