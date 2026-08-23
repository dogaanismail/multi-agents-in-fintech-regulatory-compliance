"""
Head-to-head on the IBM test window: the network agent's current global
centralities (F1 0.05 retrained) vs the proposed local ego-net/money-flow set
vs both combined. Same CatBoost architecture and protocol as naa_experiments.py
so the only variable is the feature family. This is the evidence gate before
wiring the new features into network-topology-service and the agent.
"""

import json
import pathlib
import warnings

import pandas as pd

import naa_experiments as naa

warnings.filterwarnings("ignore")

HERE = pathlib.Path(__file__).parent
DATA_DIR = HERE / "data"
RESULTS_DIR = HERE / "results"

GLOBAL_FEATURES = [
    "in_degree", "out_degree", "degree_centrality", "in_degree_centrality",
    "out_degree_centrality", "betweenness_centrality", "closeness_centrality",
    "pagerank", "eigenvector_centrality", "clustering_coefficient", "community",
]
LOCAL_FEATURES = [
    "unique_in_counterparties", "unique_out_counterparties", "reciprocity",
    "cycle3_count", "two_hop_out_reach", "in_out_amount_ratio",
    "in_concentration", "out_concentration", "forwarding_gap_hours",
    "peak_day_share",
]


def load_joined_windows() -> tuple[pd.DataFrame, pd.DataFrame]:
    def join(window: str) -> pd.DataFrame:
        global_features = pd.read_parquet(DATA_DIR / f"naa_features_{window}.parquet")
        local_features = pd.read_parquet(
            DATA_DIR / f"naa_local_features_{window}.parquet"
        ).drop(columns=["is_laundering"])
        return global_features.merge(local_features, on="account", how="inner")

    return join("train"), join("test")


def main() -> None:
    RESULTS_DIR.mkdir(exist_ok=True)
    train, test = load_joined_windows()
    print(f"joined windows: train {len(train):,} / test {len(test):,} accounts")

    variants = {
        "naa_global_centralities": GLOBAL_FEATURES,
        "naa_local_features": LOCAL_FEATURES,
        "naa_combined": GLOBAL_FEATURES + LOCAL_FEATURES,
    }

    results = {}
    for name, feature_names in variants.items():
        print(f"Training {name} ({len(feature_names)} features)...")
        results[name] = naa.evaluate_retrained(train, test, feature_names)

    merged_path = RESULTS_DIR / "naa_feature_comparison.json"
    json.dump(results, open(merged_path, "w"), indent=2)

    print(f"\n{'variant':<28} {'ROC-AUC':>8} {'PR-AUC':>8} {'P':>7} {'R':>7} {'F1':>7}")
    for name, m in results.items():
        print(f"{name:<28} {m['roc_auc']:>8.4f} {m['pr_auc']:>8.4f} "
              f"{m['precision']:>7.4f} {m['recall']:>7.4f} {m['f1']:>7.4f}")

    for name in ("naa_local_features", "naa_combined"):
        print(f"\n{name} account-level recall per typology:")
        for typology, t in sorted(results[name]["per_typology_recall"].items(),
                                  key=lambda kv: -kv[1]["recall"]):
            print(f"  {typology:<22} recall {t['recall']:.2%}  (n={t['n']})")


if __name__ == "__main__":
    main()
