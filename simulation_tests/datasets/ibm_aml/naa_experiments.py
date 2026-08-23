"""
Network agent generalization on the IBM AML account graph: zero-shot with the
deployed CatBoost model, then retrained with the same architecture, evaluated
on test-window graph features from prepare_naa_features.py.

Zero-shot caveat that belongs in any writeup: centrality magnitudes depend on
graph SIZE (degree_centrality on a 500K-node graph is ~1e-5 where the training
graphs produced ~1e-2), so the deployed scaler pushes most features far outside
its training range. Graph-size dependence of centrality features is itself a
known transfer failure mode, and it is measured here rather than patched.

Per-typology recall is at ACCOUNT level (an account counts as caught if it is
flagged and participated in that typology), so numbers are not directly
comparable to TPA's transaction-level recall — but the ranking of typologies is.
"""

import json
import pathlib
import pickle
import re
import warnings

import numpy as np
import pandas as pd
from catboost import CatBoostClassifier
from sklearn.preprocessing import StandardScaler

import evaluate_zero_shot as ez
import retrain_on_ibm as retrain

warnings.filterwarnings("ignore")

HERE = pathlib.Path(__file__).parent
DATA_DIR = HERE / "data"
RESULTS_DIR = HERE / "results"
NAA_MODELS = HERE.parents[2] / "ai-services" / "agents" / "network_analysis_agent" / "trained_models"

NAA_THRESHOLD = 0.5

CATBOOST_PARAMS = dict(
    iterations=300,
    depth=8,
    learning_rate=0.1,
    random_seed=42,
    verbose=0,
)


def load_feature_windows() -> tuple[pd.DataFrame, pd.DataFrame, list]:
    metadata = json.load(open(NAA_MODELS / "network_analysis_catboost_metadata.json"))
    feature_names = metadata["features"]["list"]
    train = pd.read_parquet(DATA_DIR / "naa_features_train.parquet")
    test = pd.read_parquet(DATA_DIR / "naa_features_test.parquet")
    return train, test, feature_names


def load_account_typologies() -> dict:
    typologies_by_account = {}
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
                for account in (parts[1] + "_" + parts[2], parts[3] + "_" + parts[4]):
                    typologies_by_account.setdefault(account, set()).add(current)
    return typologies_by_account


def per_typology_account_recall(test: pd.DataFrame, flagged: np.ndarray) -> dict:
    typologies_by_account = load_account_typologies()
    positives = test["is_laundering"].to_numpy() == 1
    account_typologies = test["account"].map(
        lambda account: typologies_by_account.get(account, set())
    )
    all_typologies = sorted({t for ts in account_typologies for t in ts})
    per_typology = {}
    for typology in all_typologies:
        mask = account_typologies.map(lambda ts: typology in ts).to_numpy() & positives
        if mask.sum() > 0:
            per_typology[typology] = {
                "n": int(mask.sum()),
                "recall": float(flagged[mask].mean()),
            }
    return per_typology


def evaluate_zero_shot(test: pd.DataFrame, feature_names: list) -> dict:
    model = pickle.load(open(NAA_MODELS / "network_analysis_catboost_model.pkl", "rb"))
    scaler = pickle.load(open(NAA_MODELS / "network_analysis_catboost_scaler.pkl", "rb"))

    scores = model.predict_proba(scaler.transform(test[feature_names]))[:, 1]
    return ez.metric_block(test["is_laundering"].to_numpy(), scores, NAA_THRESHOLD)


def evaluate_retrained(train_full: pd.DataFrame, test: pd.DataFrame, feature_names: list) -> dict:
    train_full = train_full.sample(frac=1.0, random_state=42)
    val_cut = int(len(train_full) * 0.75)
    train, val = train_full.iloc[:val_cut], train_full.iloc[val_cut:]

    scaler = StandardScaler().fit(train[feature_names])
    y_train = train["is_laundering"].to_numpy()
    scale = float((y_train == 0).sum() / max(1, (y_train == 1).sum()))
    model = CatBoostClassifier(scale_pos_weight=scale, **CATBOOST_PARAMS)
    model.fit(scaler.transform(train[feature_names]), y_train)

    val_scores = model.predict_proba(scaler.transform(val[feature_names]))[:, 1]
    threshold = retrain.best_f1_threshold(val["is_laundering"].to_numpy(), val_scores)

    test_scores = model.predict_proba(scaler.transform(test[feature_names]))[:, 1]
    metrics = ez.metric_block(test["is_laundering"].to_numpy(), test_scores, threshold)
    metrics["per_typology_recall"] = per_typology_account_recall(
        test, test_scores >= threshold
    )
    return metrics


def merge_results(path: pathlib.Path, key: str, metrics: dict) -> None:
    merged = json.load(open(path)) if path.exists() else {}
    merged[key] = metrics
    json.dump(merged, open(path, "w"), indent=2)


def main() -> None:
    RESULTS_DIR.mkdir(exist_ok=True)
    train, test, feature_names = load_feature_windows()

    print("Evaluating network analysis agent (zero-shot)...")
    zero_shot = evaluate_zero_shot(test, feature_names)
    merge_results(RESULTS_DIR / "zero_shot_metrics.json", "network_analysis_agent", zero_shot)

    print("Retraining network analysis architecture on IBM train window...")
    retrained = evaluate_retrained(train, test, feature_names)
    merge_results(RESULTS_DIR / "retrained_metrics.json",
                  "network_analysis_agent_retrained", retrained)

    print(f"\n{'model':<38} {'ROC-AUC':>8} {'PR-AUC':>8} {'P':>7} {'R':>7} {'F1':>7}")
    for name, m in (("network_analysis_agent (zero-shot)", zero_shot),
                    ("network_analysis_agent_retrained", retrained)):
        print(f"{name:<38} {m['roc_auc']:>8.4f} {m['pr_auc']:>8.4f} "
              f"{m['precision']:>7.4f} {m['recall']:>7.4f} {m['f1']:>7.4f}")

    print("\nNAA account-level recall per laundering typology (test window):")
    for name, t in sorted(retrained["per_typology_recall"].items(),
                          key=lambda kv: -kv[1]["recall"]):
        print(f"  {name:<22} recall {t['recall']:.2%}  (n={t['n']})")


if __name__ == "__main__":
    main()
