"""Finds the best-F1 operating threshold on the target domain, quantifying how
much of the zero-shot failure is calibration shift rather than lost signal."""
import json
import pathlib
import sys

import numpy as np
from sklearn.metrics import precision_recall_curve

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import evaluate_zero_shot as ez


def recalibrated(y, scores):
    precision, recall, thresholds = precision_recall_curve(y, scores)
    f1 = 2 * precision * recall / np.clip(precision + recall, 1e-12, None)
    best = int(np.nanargmax(f1[:-1]))
    return ez.metric_block(y, scores, float(thresholds[best]))


results = {}
agent = sys.argv[1]
if agent == "tpa":
    import joblib, pandas as pd

    model = joblib.load(
        ez.AGENTS_DIR / "transaction_pattern_agent/trained_models/xgboost_transaction_pattern_agent.pkl")
    pre = joblib.load(ez.AGENTS_DIR / "transaction_pattern_agent/trained_models/preprocessor.pkl")
    df = pd.read_parquet(ez.DATA_DIR / "tpa_transactions.parquet")
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
    scores = np.empty(len(frame))
    for s in range(0, len(frame), ez.BATCH):
        c = frame.iloc[s:s + ez.BATCH]
        scores[s:s + len(c)] = model.predict_proba(pre.transform(c))[:, 1]
    results["transaction_pattern_agent_recalibrated"] = recalibrated(df["is_laundering"].to_numpy(), scores)
else:
    import pickle, pandas as pd

    b = pickle.load(open(ez.AGENTS_DIR / "customer_risk_agent/trained_models/customer_risk_model.pkl", "rb"))
    accounts = pd.read_parquet(ez.DATA_DIR / "cra_accounts.parquet")
    scores = b["model"].predict_proba(b["scaler"].transform(accounts[b["feature_names"]]))[:, 1]
    results["customer_risk_agent_recalibrated"] = recalibrated(accounts["is_laundering"].to_numpy(), scores)

path = ez.RESULTS_DIR / "zero_shot_metrics.json"
merged = json.load(open(path))
merged.update(results)
json.dump(merged, open(path, "w"), indent=2)
for k, m in results.items():
    print(f"{k}: thr={m['threshold']:.4f} P={m['precision']:.4f} R={m['recall']:.4f} F1={m['f1']:.4f}")
