"""
Retrains the network topology CatBoost model served by this agent.

Reproduces notebooks/01_NetworkAnalysisAgent_Baseline.ipynb end to end
(graph construction -> 11 topology features -> volume-percentile labels ->
StandardScaler -> SMOTE -> CatBoost) and overwrites the three artifacts in
trained_models/ that app/services/model_loader.py loads at startup.

Run from this directory:  python train_topology_model.py [--data-path <csv>]
"""

import argparse
import json
import pickle
import time
from datetime import datetime
from pathlib import Path

import networkx as nx
import numpy as np
import pandas as pd
from catboost import CatBoostClassifier
from imblearn.over_sampling import SMOTE
from sklearn.metrics import (
    average_precision_score,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

DEFAULT_DATA_PATH = (
        Path(__file__).resolve().parents[4]
        / "data"
        / "Anti Money Laundering Transaction Data (SAML-D).csv"
)
MODEL_OUTPUT_DIR = Path(__file__).resolve().parents[1] / "trained_models"

CONFIG = {
    "top_n_accounts": 5000,
    "min_transactions": 5,
    "suspicious_percentile": 90,
    "test_size": 0.2,
    "random_state": 42,
    "use_smote": True,
    "smote_k_neighbors": 5,
}


def log(message: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] {message}", flush=True)


def build_transaction_graph(df: pd.DataFrame, from_col: str, to_col: str, amount_col: str) -> nx.DiGraph:
    account_activity = (
        pd.concat([df[from_col].value_counts(), df[to_col].value_counts()])
        .groupby(level=0)
        .sum()
        .sort_values(ascending=False)
    )
    top_accounts = set(account_activity.head(CONFIG["top_n_accounts"]).index)
    df_network = df[(df[from_col].isin(top_accounts)) | (df[to_col].isin(top_accounts))].copy()
    log(f"Network transactions: {len(df_network):,} ({len(df_network) / len(df) * 100:.1f}% of data)")

    edge_data = (
        df_network.groupby([from_col, to_col])
        .agg({amount_col: ["sum", "mean", "count"]})
        .reset_index()
    )
    edge_data.columns = ["from", "to", "total_amount", "avg_amount", "count"]

    graph = nx.DiGraph()
    for from_account, to_account, total_amount, avg_amount, count in edge_data.itertuples(
            index=False, name=None):
        graph.add_edge(
            from_account,
            to_account,
            weight=total_amount,
            count=count,
            avg_amount=avg_amount,
        )
    log(f"Graph built: {graph.number_of_nodes():,} nodes, {graph.number_of_edges():,} edges")
    return graph, df_network


def compute_topology_features(graph: nx.DiGraph) -> pd.DataFrame:
    in_degree = dict(graph.in_degree())
    out_degree = dict(graph.out_degree())

    log("Computing degree centralities...")
    degree_centrality = nx.degree_centrality(graph)
    in_degree_centrality = nx.in_degree_centrality(graph)
    out_degree_centrality = nx.out_degree_centrality(graph)

    log("Computing betweenness centrality (sampled)...")
    betweenness = nx.betweenness_centrality(graph, k=min(1000, graph.number_of_nodes()))

    log("Computing closeness centrality...")
    closeness = nx.closeness_centrality(graph)

    log("Computing PageRank...")
    pagerank = nx.pagerank(graph, max_iter=100)

    log("Computing clustering coefficients...")
    graph_undirected = graph.to_undirected()
    clustering = nx.clustering(graph_undirected)

    log("Computing eigenvector centrality...")
    try:
        eigenvector = nx.eigenvector_centrality(graph, max_iter=1000)
    except nx.PowerIterationFailedConvergence:
        eigenvector = {node: 0 for node in graph.nodes()}
        log("Eigenvector centrality did not converge - using zeros")

    log("Detecting communities (greedy modularity)...")
    from networkx.algorithms import community

    communities = list(community.greedy_modularity_communities(graph_undirected))
    node_community = {node: i for i, comm in enumerate(communities) for node in comm}
    log(f"Communities detected: {len(communities)}")

    return pd.DataFrame(
        [
            {
                "account": node,
                "in_degree": in_degree[node],
                "out_degree": out_degree[node],
                "degree_centrality": degree_centrality[node],
                "in_degree_centrality": in_degree_centrality[node],
                "out_degree_centrality": out_degree_centrality[node],
                "betweenness_centrality": betweenness[node],
                "closeness_centrality": closeness[node],
                "pagerank": pagerank[node],
                "eigenvector_centrality": eigenvector[node],
                "clustering_coefficient": clustering[node],
                "community": node_community[node],
            }
            for node in graph.nodes()
        ]
    )


def label_by_volume(df_network: pd.DataFrame, from_col: str, to_col: str) -> pd.DataFrame:
    account_volumes = pd.concat(
        [
            df_network.groupby(from_col).size().rename("sent"),
            df_network.groupby(to_col).size().rename("received"),
        ],
        axis=1,
    ).fillna(0)
    account_volumes["total_transactions"] = account_volumes["sent"] + account_volumes["received"]
    account_volumes = account_volumes.reset_index().rename(columns={"index": "account"})
    account_volumes = account_volumes[
        account_volumes["total_transactions"] >= CONFIG["min_transactions"]
        ]

    threshold = account_volumes["total_transactions"].quantile(CONFIG["suspicious_percentile"] / 100)
    account_volumes["is_suspicious"] = (account_volumes["total_transactions"] >= threshold).astype(int)
    log(
        f"Labels: {account_volumes['is_suspicious'].sum():,} suspicious "
        f"of {len(account_volumes):,} accounts (threshold {threshold:.0f} transactions)"
    )
    return account_volumes


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-path", type=Path, default=DEFAULT_DATA_PATH)
    args = parser.parse_args()

    log(f"Loading dataset: {args.data_path}")
    df = pd.read_csv(args.data_path)
    log(f"Dataset loaded: {len(df):,} transactions")

    from_col = next(col for col in df.columns if any(p in col.lower() for p in ["sender", "from"]))
    to_col = next(col for col in df.columns if any(p in col.lower() for p in ["receiver", "to", "recipient"]))
    amount_col = next(col for col in df.columns if "amount" in col.lower())

    graph, df_network = build_transaction_graph(df, from_col, to_col, amount_col)
    df_features = compute_topology_features(graph)
    account_volumes = label_by_volume(df_network, from_col, to_col)

    df_model = df_features.merge(
        account_volumes[["account", "is_suspicious"]], on="account", how="inner"
    )
    feature_cols = [col for col in df_model.columns if col not in ["account", "is_suspicious"]]
    X = df_model[feature_cols]
    y = df_model["is_suspicious"]
    log(f"Training data: {X.shape[0]:,} samples, {len(feature_cols)} features")

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=CONFIG["test_size"],
        random_state=CONFIG["random_state"],
        stratify=y,
    )

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    if CONFIG["use_smote"]:
        smote = SMOTE(k_neighbors=CONFIG["smote_k_neighbors"], random_state=CONFIG["random_state"])
        X_train_balanced, y_train_balanced = smote.fit_resample(X_train_scaled, y_train)
        log(f"SMOTE applied: {len(y_train):,} -> {len(y_train_balanced):,} samples")
    else:
        X_train_balanced, y_train_balanced = X_train_scaled, y_train

    log("Training CatBoost...")
    train_start = time.time()
    model = CatBoostClassifier(
        iterations=200,
        depth=6,
        learning_rate=0.1,
        random_state=CONFIG["random_state"],
        verbose=False,
    )
    model.fit(X_train_balanced, y_train_balanced)
    train_time = time.time() - train_start

    y_prob = model.predict_proba(X_test_scaled)[:, 1]
    y_pred = (y_prob > 0.5).astype(int)
    performance = {
        "roc_auc": float(roc_auc_score(y_test, y_prob)),
        "pr_auc": float(average_precision_score(y_test, y_prob)),
        "f1_score": float(f1_score(y_test, y_pred)),
        "precision": float(precision_score(y_test, y_pred)),
        "recall": float(recall_score(y_test, y_pred)),
    }
    log(f"CatBoost trained in {train_time:.1f}s: {performance}")

    MODEL_OUTPUT_DIR.mkdir(exist_ok=True)
    with open(MODEL_OUTPUT_DIR / "network_analysis_catboost_model.pkl", "wb") as f:
        pickle.dump(model, f)
    with open(MODEL_OUTPUT_DIR / "network_analysis_catboost_scaler.pkl", "wb") as f:
        pickle.dump(scaler, f)

    metadata = {
        "model_info": {
            "name": "CatBoost",
            "type": "Network Topology Anomaly Detector",
            "created_at": datetime.now().isoformat(),
            "version": "2.1_retrain",
        },
        "config": {**CONFIG, "data_path": str(args.data_path), "model_output_dir": str(MODEL_OUTPUT_DIR)},
        "network_stats": {
            "nodes": graph.number_of_nodes(),
            "edges": graph.number_of_edges(),
            "density": nx.density(graph),
        },
        "training_data": {
            "total_samples": len(df_model),
            "train_samples": len(X_train),
            "test_samples": len(X_test),
            "suspicious_rate": float(y.mean()),
        },
        "features": {
            "count": len(feature_cols),
            "list": feature_cols,
            "note": "Topology only - volume features excluded",
        },
        "performance": performance,
    }
    with open(MODEL_OUTPUT_DIR / "network_analysis_catboost_metadata.json", "w") as f:
        json.dump(metadata, f, indent=2)

    log(f"Artifacts written to {MODEL_OUTPUT_DIR}")


if __name__ == "__main__":
    main()
