"""
Builds the network agent's 11 graph features over the IBM AML account graph,
separately for the train and test windows (same 80% temporal cutoff as the
other agents) so retraining never sees test-window structure.

The full graph is ~500K accounts, so three features are approximated — exact
computation is O(V*E) or worse and infeasible in networkx at this size:
  - betweenness_centrality: Brandes sampled over BETWEENNESS_SAMPLES pivots
  - closeness_centrality:   landmark approximation, L = CLOSENESS_LANDMARKS
    BFS sources, closeness(v) = reached_landmarks / sum of distances
  - clustering_coefficient: exact up to CLUSTERING_DEGREE_CAP neighbours,
    sampled neighbour pairs above it (hub accounts have degree in the
    thousands and exact triangle counting is quadratic in degree)
Community ids come from Louvain and are re-indexed by descending community
size so the id carries a weak "how mainstream is this cluster" ordering
instead of being arbitrary.

Outputs: data/naa_features_train.parquet, data/naa_features_test.parquet
"""

import pathlib
import random
import time

import networkx as nx
import numpy as np
import pandas as pd

import prepare_ibm_aml as prep

DATA_DIR = pathlib.Path(__file__).parent / "data"

BETWEENNESS_SAMPLES = 32
CLOSENESS_LANDMARKS = 32
CLUSTERING_DEGREE_CAP = 100
CLUSTERING_SAMPLE_PAIRS = 200
SEED = 42


def log(message: str) -> None:
    print(f"  [{time.strftime('%H:%M:%S')}] {message}", flush=True)


def landmark_closeness(undirected: nx.Graph, rng: random.Random) -> dict:
    landmarks = rng.sample(list(undirected.nodes), CLOSENESS_LANDMARKS)
    distance_sum = {}
    reached = {}
    for landmark in landmarks:
        for node, distance in nx.single_source_shortest_path_length(undirected, landmark).items():
            distance_sum[node] = distance_sum.get(node, 0) + distance
            reached[node] = reached.get(node, 0) + 1
    return {
        node: (reached[node] / distance_sum[node]) if distance_sum.get(node) else 0.0
        for node in undirected.nodes
    }


def sampled_clustering(undirected: nx.Graph, rng: random.Random) -> dict:
    adjacency = {node: set(undirected[node]) for node in undirected.nodes}
    clustering = {}
    for node, neighbours in adjacency.items():
        neighbour_list = list(neighbours)
        degree = len(neighbour_list)
        if degree < 2:
            clustering[node] = 0.0
        elif degree <= CLUSTERING_DEGREE_CAP:
            links = sum(
                1
                for i in range(degree)
                for j in range(i + 1, degree)
                if neighbour_list[j] in adjacency[neighbour_list[i]]
            )
            clustering[node] = 2.0 * links / (degree * (degree - 1))
        else:
            hits = sum(
                1
                for _ in range(CLUSTERING_SAMPLE_PAIRS)
                if (pair := rng.sample(neighbour_list, 2))
                and pair[1] in adjacency[pair[0]]
            )
            clustering[node] = hits / CLUSTERING_SAMPLE_PAIRS
    return clustering


def eigenvector_centrality_with_fallback(undirected: nx.Graph) -> dict:
    try:
        return nx.eigenvector_centrality_numpy(undirected, max_iter=100)
    except Exception as first_error:
        log(f"eigenvector_centrality_numpy failed ({first_error}); using power iteration")
        try:
            return nx.eigenvector_centrality(undirected, max_iter=500, tol=1e-3)
        except Exception as second_error:
            log(f"power iteration failed too ({second_error}); eigenvector set to 0")
            return dict.fromkeys(undirected.nodes, 0.0)


def community_ids_by_size(undirected: nx.Graph) -> dict:
    communities = nx.community.louvain_communities(undirected, weight="weight", seed=SEED)
    ids = {}
    for community_id, members in enumerate(sorted(communities, key=len, reverse=True)):
        for node in members:
            ids[node] = community_id
    return ids


def build_window_features(window_df: pd.DataFrame, window: str) -> pd.DataFrame:
    edges = window_df.groupby(["payer", "payee"]).size().reset_index(name="weight")
    graph = nx.from_pandas_edgelist(
        edges, "payer", "payee", edge_attr="weight", create_using=nx.DiGraph
    )
    undirected = graph.to_undirected()
    node_count = graph.number_of_nodes()
    log(f"{window} graph: {node_count:,} accounts, {graph.number_of_edges():,} edges")
    rng = random.Random(SEED)

    in_degree = dict(graph.in_degree())
    out_degree = dict(graph.out_degree())

    log("pagerank...")
    pagerank = nx.pagerank(graph, weight="weight")
    log(f"betweenness (k={BETWEENNESS_SAMPLES} pivots)...")
    betweenness = nx.betweenness_centrality(graph, k=BETWEENNESS_SAMPLES, seed=SEED)
    log(f"closeness ({CLOSENESS_LANDMARKS} landmarks)...")
    closeness = landmark_closeness(undirected, rng)
    log("eigenvector...")
    eigenvector = eigenvector_centrality_with_fallback(undirected)
    log("clustering...")
    clustering = sampled_clustering(undirected, rng)
    log("louvain communities...")
    community = community_ids_by_size(undirected)

    labels = pd.concat([
        window_df.groupby("payer")["is_laundering"].max(),
        window_df.groupby("payee")["is_laundering"].max(),
    ]).groupby(level=0).max()

    nodes = list(graph.nodes)
    scale = 1.0 / max(1, node_count - 1)
    features = pd.DataFrame({
        "account": nodes,
        "in_degree": [in_degree[n] for n in nodes],
        "out_degree": [out_degree[n] for n in nodes],
        "degree_centrality": [(in_degree[n] + out_degree[n]) * scale for n in nodes],
        "in_degree_centrality": [in_degree[n] * scale for n in nodes],
        "out_degree_centrality": [out_degree[n] * scale for n in nodes],
        "betweenness_centrality": [betweenness[n] for n in nodes],
        "closeness_centrality": [closeness[n] for n in nodes],
        "pagerank": [pagerank[n] for n in nodes],
        "eigenvector_centrality": [eigenvector.get(n, 0.0) for n in nodes],
        "clustering_coefficient": [clustering[n] for n in nodes],
        "community": [community[n] for n in nodes],
        "is_laundering": [labels[n] for n in nodes],
    })
    log(f"{window} features: {len(features):,} accounts, "
        f"{features['is_laundering'].mean():.4%} illicit")
    return features


def main() -> None:
    print("Loading transactions...", flush=True)
    df = prep.load_transactions()
    prep.add_temporal_split(df)

    for window in ("train", "test"):
        features = build_window_features(df[df["split"] == window], window)
        features.to_parquet(DATA_DIR / f"naa_features_{window}.parquet", index=False)
    print("Done.", flush=True)


if __name__ == "__main__":
    main()
