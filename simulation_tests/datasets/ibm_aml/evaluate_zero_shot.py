"""
Zero-shot generalization: runs the agents' currently-trained models, unchanged,
on the IBM AML HI-Small data mapped to their feature schemas.

Measures out-of-distribution transfer: the models were trained on SAML-D-style
data and have never seen a single IBM transaction. Expect degradation — the
honest question is how much, and which agent survives the domain shift best.

Metrics are reported on the temporal test window (last 20% of the timeline) for
TPA so future retraining experiments compare like-for-like; account-level
agents are evaluated on all accounts (no training happens here, so there is no
leakage concern, but retraining experiments must re-aggregate with temporal
discipline).
"""

import json
import pathlib
import pickle
import warnings

import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import (average_precision_score, confusion_matrix,
                             f1_score, precision_score, recall_score,
                             roc_auc_score)

warnings.filterwarnings("ignore")

HERE = pathlib.Path(__file__).parent
DATA_DIR = HERE / "data"
RESULTS_DIR = HERE / "results"
AGENTS_DIR = HERE.parents[2] / "ai-services" / "agents"

TPA_THRESHOLD = 0.13
CRA_THRESHOLD = 0.5
BATCH = 250_000


def metric_block(y_true, y_score, threshold) -> dict:
    y_pred = (y_score >= threshold).astype(int)
    tn, fp, fn, tp = confusion_matrix(y_true, y_pred).ravel()
    return {
        "roc_auc": float(roc_auc_score(y_true, y_score)),
        "pr_auc": float(average_precision_score(y_true, y_score)),
        "threshold": threshold,
        "precision": float(precision_score(y_true, y_pred, zero_division=0)),
        "recall": float(recall_score(y_true, y_pred, zero_division=0)),
        "f1": float(f1_score(y_true, y_pred, zero_division=0)),
        "confusion": {"tp": int(tp), "fp": int(fp), "fn": int(fn), "tn": int(tn)},
        "positives": int(y_true.sum()),
        "n": int(len(y_true)),
    }


def evaluate_tpa() -> dict:
    model = joblib.load(AGENTS_DIR / "transaction_pattern_agent/trained_models/xgboost_transaction_pattern_agent.pkl")
    preprocessor = joblib.load(AGENTS_DIR / "transaction_pattern_agent/trained_models/preprocessor.pkl")

    df = pd.read_parquet(DATA_DIR / "tpa_transactions.parquet")
    df = df[df["split"] == "test"].reset_index(drop=True)

    frame = pd.DataFrame({
        "Time": pd.to_datetime(df["Time"], format="%H:%M:%S"),
        "Date": pd.to_datetime(df["Date"]),
        "Amount": df["Amount"],
        "Payment_currency": df["Payment_currency"],
        "Received_currency": df["Received_currency"],
        "Sender_bank_location": df["Sender_bank_location"],
        "Receiver_bank_location": df["Receiver_bank_location"],
        "Payment_type": df["Payment_type"],
    })
    frame["hour"] = frame["Time"].dt.hour
    frame["day_of_week"] = frame["Date"].dt.day_name()
    frame["date"] = frame["Date"].dt.date

    scores = np.empty(len(frame), dtype=np.float64)
    for start in range(0, len(frame), BATCH):
        chunk = frame.iloc[start:start + BATCH]
        scores[start:start + len(chunk)] = model.predict_proba(preprocessor.transform(chunk))[:, 1]

    return metric_block(df["is_laundering"].to_numpy(), scores, TPA_THRESHOLD)


def evaluate_cra() -> dict:
    bundle = pickle.load(open(AGENTS_DIR / "customer_risk_agent/trained_models/customer_risk_model.pkl", "rb"))
    model, scaler, feature_names = bundle["model"], bundle["scaler"], bundle["feature_names"]

    accounts = pd.read_parquet(DATA_DIR / "cra_accounts.parquet")
    X = scaler.transform(accounts[feature_names])
    scores = model.predict_proba(X)[:, 1]

    return metric_block(accounts["is_laundering"].to_numpy(), scores, CRA_THRESHOLD)


def main() -> None:
    import sys
    RESULTS_DIR.mkdir(exist_ok=True)
    agents = sys.argv[1:] or ["tpa", "cra"]
    results = {}

    if "tpa" in agents:
        print("Evaluating transaction pattern agent (zero-shot)...")
        results["transaction_pattern_agent"] = evaluate_tpa()
    if "cra" in agents:
        print("Evaluating customer risk agent (zero-shot)...")
        results["customer_risk_agent"] = evaluate_cra()

    merged_path = RESULTS_DIR / "zero_shot_metrics.json"
    merged = json.load(open(merged_path)) if merged_path.exists() else {}
    merged.update(results)
    json.dump(merged, open(merged_path, "w"), indent=2)
    results = merged

    print(f"\n{'agent':<28} {'ROC-AUC':>8} {'PR-AUC':>8} {'P':>7} {'R':>7} {'F1':>7} {'pos/n'}")
    for agent, m in results.items():
        print(f"{agent:<28} {m['roc_auc']:>8.4f} {m['pr_auc']:>8.4f} "
              f"{m['precision']:>7.4f} {m['recall']:>7.4f} {m['f1']:>7.4f} "
              f"{m['positives']:,}/{m['n']:,}")


if __name__ == "__main__":
    main()
