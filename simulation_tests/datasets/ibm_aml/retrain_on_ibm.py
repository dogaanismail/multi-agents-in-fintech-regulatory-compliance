"""
Retrains the transaction and customer agents' architectures on the IBM AML
train window and evaluates on the same held-out test window the zero-shot run
used, giving the in-domain ceiling that contextualises the zero-shot floor.

Discipline:
  - identical architectures and feature schemas to the deployed agents
  - preprocessing refit on the IBM train window (domain vocabularies)
  - threshold chosen by best F1 on a validation slice carved from the END of
    the train window, never on test
  - CRA aggregates are built per window so no test-window behaviour leaks into
    training features
  - per-typology recall on the test window via the patterns file

The retrained models are research artifacts only and are never deployed to the
agents: the zero-shot results are precisely the evidence that models do not
carry across domains.
"""

import json
import pathlib
import re
import warnings

import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.metrics import precision_recall_curve
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from xgboost import XGBClassifier

import evaluate_zero_shot as ez
import prepare_ibm_aml as prep

warnings.filterwarnings("ignore")

HERE = pathlib.Path(__file__).parent
DATA_DIR = HERE / "data"
RESULTS_DIR = HERE / "results"

TPA_FEATURES_NUM = ["Amount"]
TPA_FEATURES_CAT = ["Payment_type", "Payment_currency",
                    "Sender_bank_location", "Receiver_bank_location"]

XGB_PARAMS = dict(
    n_estimators=300,
    max_depth=8,
    learning_rate=0.1,
    subsample=0.8,
    colsample_bytree=0.8,
    tree_method="hist",
    n_jobs=-1,
    eval_metric="aucpr",
)


def best_f1_threshold(y_true, y_score) -> float:
    precision, recall, thresholds = precision_recall_curve(y_true, y_score)
    f1 = 2 * precision * recall / np.clip(precision + recall, 1e-12, None)
    return float(thresholds[int(np.nanargmax(f1[:-1]))])


def fit_xgboost(X_train, y_train) -> XGBClassifier:
    scale = float((y_train == 0).sum() / max(1, (y_train == 1).sum()))
    model = XGBClassifier(scale_pos_weight=scale, **XGB_PARAMS)
    model.fit(X_train, y_train)
    return model


def load_typologies() -> dict:
    typology_by_key = {}
    current = None
    pattern_header = re.compile(r"BEGIN LAUNDERING ATTEMPT - ([A-Z\- ]+?)(?::|$)")
    for line in open(DATA_DIR / "HI-Small_Trans_patterns.txt"):
        header = pattern_header.match(line)
        if header:
            current = header.group(1).strip()
            continue
        if line.startswith("END"):
            current = None
            continue
        if current and "," in line:
            parts = line.strip().split(",")
            if len(parts) >= 5:
                key = (parts[0], parts[1], parts[2], parts[3], parts[4])
                typology_by_key[key] = current
    return typology_by_key


def retrain_tpa() -> dict:
    df = pd.read_parquet(DATA_DIR / "tpa_transactions.parquet")
    raw = prep.load_transactions()
    df["typology_key"] = list(zip(
        raw["timestamp"].dt.strftime("%Y/%m/%d %H:%M"),
        raw["from_bank"], raw["from_account"], raw["to_bank"], raw["to_account"],
    ))

    train_full = df[df["split"] == "train"]
    val_cut = int(len(train_full) * 0.75)
    train, val = train_full.iloc[:val_cut], train_full.iloc[val_cut:]
    test = df[df["split"] == "test"]

    preprocessor = ColumnTransformer([
        ("num", StandardScaler(), TPA_FEATURES_NUM),
        ("cat", OneHotEncoder(handle_unknown="ignore"), TPA_FEATURES_CAT),
    ])
    columns = TPA_FEATURES_NUM + TPA_FEATURES_CAT

    X_train = preprocessor.fit_transform(train[columns])
    model = fit_xgboost(X_train, train["is_laundering"].to_numpy())

    val_scores = model.predict_proba(preprocessor.transform(val[columns]))[:, 1]
    threshold = best_f1_threshold(val["is_laundering"].to_numpy(), val_scores)

    test_scores = model.predict_proba(preprocessor.transform(test[columns]))[:, 1]
    metrics = ez.metric_block(test["is_laundering"].to_numpy(), test_scores, threshold)

    typologies = load_typologies()
    flagged = test_scores >= threshold
    positives = test["is_laundering"].to_numpy() == 1
    typology_series = test["typology_key"].map(typologies)
    per_typology = {}
    for typology in sorted(typology_series.dropna().unique()):
        mask = (typology_series == typology).to_numpy() & positives
        if mask.sum() > 0:
            per_typology[typology] = {
                "n": int(mask.sum()),
                "recall": float(flagged[mask].mean()),
            }
    metrics["per_typology_recall"] = per_typology
    return metrics


def retrain_cra() -> dict:
    raw = prep.load_transactions()
    cutoff = raw["timestamp"].quantile(0.8)
    raw["split"] = np.where(raw["timestamp"] <= cutoff, "train", "test")

    def windowed_accounts(window: str) -> pd.DataFrame:
        return prep.build_cra_table(raw[raw["split"] == window].copy())

    print("  aggregating train-window accounts...")
    train_full = windowed_accounts("train").sample(frac=1.0, random_state=42)
    print("  aggregating test-window accounts...")
    test = windowed_accounts("test")

    feature_names = [c for c in train_full.columns if c not in ("payer", "is_laundering")]
    val_cut = int(len(train_full) * 0.75)
    train, val = train_full.iloc[:val_cut], train_full.iloc[val_cut:]

    scaler = StandardScaler().fit(train[feature_names])
    model = fit_xgboost(scaler.transform(train[feature_names]),
                        train["is_laundering"].to_numpy())

    val_scores = model.predict_proba(scaler.transform(val[feature_names]))[:, 1]
    threshold = best_f1_threshold(val["is_laundering"].to_numpy(), val_scores)

    test_scores = model.predict_proba(scaler.transform(test[feature_names]))[:, 1]
    return ez.metric_block(test["is_laundering"].to_numpy(), test_scores, threshold)


def main() -> None:
    RESULTS_DIR.mkdir(exist_ok=True)
    results = {}

    print("Retraining transaction pattern architecture on IBM train window...")
    results["transaction_pattern_agent_retrained"] = retrain_tpa()
    print("Retraining customer risk architecture on IBM train window...")
    results["customer_risk_agent_retrained"] = retrain_cra()

    json.dump(results, open(RESULTS_DIR / "retrained_metrics.json", "w"), indent=2)

    print(f"\n{'model':<38} {'ROC-AUC':>8} {'PR-AUC':>8} {'P':>7} {'R':>7} {'F1':>7}")
    for name, m in results.items():
        print(f"{name:<38} {m['roc_auc']:>8.4f} {m['pr_auc']:>8.4f} "
              f"{m['precision']:>7.4f} {m['recall']:>7.4f} {m['f1']:>7.4f}")
    typology = results["transaction_pattern_agent_retrained"].get("per_typology_recall", {})
    if typology:
        print("\nTPA recall per laundering typology (test window):")
        for name, t in sorted(typology.items(), key=lambda kv: -kv[1]["recall"]):
            print(f"  {name:<22} recall {t['recall']:.2%}  (n={t['n']})")


if __name__ == "__main__":
    main()
