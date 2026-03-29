"""
Figure Z  (Fig 5.21): Aggregated Feature Importance — All Three Agents
Combines TreeSHAP importance from real models into a single grouped view.
Top 8 features per agent, horizontally grouped with agent colour coding.
For: Chapter 5, Section 5.5.3
"""

import warnings
warnings.filterwarnings("ignore")

import pathlib, pickle, json
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import joblib
import shap

# ── paths ───────────────────────────────────────────────────────────────
ROOT   = pathlib.Path(__file__).resolve().parents[3]
OUT    = ROOT / "simulation_tests" / "reports" / "charts"
MODELS = {
    "TPA": ROOT / "ai-services/agents/transaction_pattern_agent/trained_models/xgboost_transaction_pattern_agent.pkl",
    "CRA": ROOT / "ai-services/agents/customer_risk_agent/trained_models/customer_risk_model.pkl",
    "NAA": ROOT / "ai-services/agents/network_analysis_agent/trained_models/network_analysis_catboost_model.pkl",
}
NAA_META = ROOT / "ai-services/agents/network_analysis_agent/trained_models/network_analysis_catboost_metadata.json"

COLORS = {"TPA": "#2196F3", "CRA": "#FF9800", "NAA": "#4CAF50"}
LABELS = {
    "TPA": "Transaction Pattern Agent",
    "CRA": "Customer Risk Agent",
    "NAA": "Network Analysis Agent",
}

# ── style ───────────────────────────────────────────────────────────────
plt.rcParams.update({
    "font.family": "serif",
    "font.size": 10,
    "figure.dpi": 300,
    "savefig.dpi": 300,
    "savefig.bbox": "tight",
    "savefig.pad_inches": 0.3,
})

# ── TPA feature names ──────────────────────────────────────────────────
_PAY_TYPES = ["ACH", "Cash Deposit", "Cash Withdrawal", "Cheque",
              "Credit card", "Cross-border", "Debit card"]
_CURRENCIES = ["Albanian lek", "Dirham", "Euro", "Indian rupee", "Mexican Peso",
               "Moroccan dirham", "Naira", "Pakistani rupee", "Swiss franc",
               "Turkish lira", "UK pounds", "US dollar", "Yen"]
_SENDER = ["Albania", "Austria", "France", "Germany", "India", "Italy",
           "Japan", "Mexico", "Morocco", "Nigeria", "Pakistan", "Spain",
           "Switzerland", "Turkey", "UAE", "UK"]
_RECV = ["Albania", "Austria", "France", "Germany", "India", "Italy",
         "Japan", "Mexico", "Netherlands", "Nigeria", "Pakistan", "Spain",
         "Switzerland", "Turkey", "UAE", "UK", "USA"]

# ── load models & compute SHAP ─────────────────────────────────────────
print("Loading models...")
np.random.seed(42)
N = 500

# --- TPA ---
tpa_model = joblib.load(MODELS["TPA"])
n_tpa = tpa_model.n_features_in_
tpa_names = ["Amount"]
for prefix, cats in [("PayType", _PAY_TYPES), ("Currency", _CURRENCIES),
                     ("Sender", _SENDER), ("Receiver", _RECV)]:
    for c in cats:
        tpa_names.append(f"{prefix}: {c}")
while len(tpa_names) < n_tpa:
    tpa_names.append(f"extra_{len(tpa_names)}")
tpa_names = tpa_names[:n_tpa]

tpa_X = np.zeros((N, n_tpa), dtype=np.float32)
tpa_X[:, 0] = np.random.lognormal(7, 2, N)
offset = 1
for prefix, cats in [("PayType", _PAY_TYPES), ("Currency", _CURRENCIES),
                     ("Sender", _SENDER), ("Receiver", _RECV)]:
    n_c = len(cats)
    for i in range(N):
        tpa_X[i, offset + np.random.randint(0, min(n_c, n_tpa - offset))] = 1.0
    offset += n_c

tpa_shap = shap.TreeExplainer(tpa_model).shap_values(pd.DataFrame(tpa_X, columns=tpa_names))
if isinstance(tpa_shap, list):
    tpa_shap = tpa_shap[1]
elif tpa_shap.ndim == 3:
    tpa_shap = tpa_shap[:, :, 1]
print(f"  TPA: {tpa_shap.shape}")

# --- CRA ---
with open(MODELS["CRA"], "rb") as f:
    cra_b = pickle.load(f)
cra_model, cra_names, cra_scaler = cra_b["model"], cra_b["feature_names"], cra_b["scaler"]
cra_ranges = {
    "transaction_count": (1, 500), "total_amount": (100, 500000),
    "avg_amount": (50, 50000), "median_amount": (50, 30000),
    "max_amount": (100, 200000), "min_amount": (1, 5000),
    "std_amount": (0, 50000), "active_days": (1, 365),
    "transactions_per_day": (0.01, 20), "cross_border_ratio": (0, 1),
    "cash_transaction_ratio": (0, 1), "amount_consistency": (0, 1),
    "large_transaction_ratio": (0, 1), "unique_receivers": (1, 200),
    "unique_receiver_countries": (1, 30), "receiver_diversity": (0, 1),
    "night_transaction_ratio": (0, 1), "weekend_transaction_ratio": (0, 1),
    "unique_currencies": (1, 10),
}
cra_raw = np.column_stack([
    np.random.uniform(*cra_ranges.get(f, (0, 1)), N).astype(np.float32)
    for f in cra_names
])
cra_X = cra_scaler.transform(cra_raw)
cra_shap = shap.TreeExplainer(cra_model).shap_values(pd.DataFrame(cra_X, columns=cra_names))
if isinstance(cra_shap, list):
    cra_shap = cra_shap[1]
elif cra_shap.ndim == 3:
    cra_shap = cra_shap[:, :, 1]
print(f"  CRA: {cra_shap.shape}")

# --- NAA ---
with open(MODELS["NAA"], "rb") as f:
    naa_model = pickle.load(f)
with open(NAA_META) as f:
    naa_names = json.load(f)["features"]["list"]
naa_ranges = {
    "in_degree": (0, 500), "out_degree": (0, 500),
    "degree_centrality": (0, 0.01), "in_degree_centrality": (0, 0.01),
    "out_degree_centrality": (0, 0.01), "betweenness_centrality": (0, 0.001),
    "closeness_centrality": (0, 1), "pagerank": (0, 0.001),
    "eigenvector_centrality": (0, 0.1), "clustering_coefficient": (0, 1),
    "community": (0, 50),
}
naa_X = np.column_stack([
    np.random.uniform(*naa_ranges.get(f, (0, 1)), N).astype(np.float32)
    for f in naa_names
])
naa_shap = shap.TreeExplainer(naa_model).shap_values(pd.DataFrame(naa_X, columns=naa_names))
if isinstance(naa_shap, list):
    naa_shap = naa_shap[1]
elif naa_shap.ndim == 3:
    naa_shap = naa_shap[:, :, 1]
print(f"  NAA: {naa_shap.shape}")

# ── collect top 8 features per agent ────────────────────────────────────
TOP_K = 8
all_features = []  # list of (agent, feature_name, mean_abs_shap)

for agent, shap_vals, names in [("TPA", tpa_shap, tpa_names),
                                 ("CRA", cra_shap, cra_names),
                                 ("NAA", naa_shap, naa_names)]:
    mean_abs = np.abs(shap_vals).mean(axis=0)
    top_idx = np.argsort(mean_abs)[::-1][:TOP_K]
    for idx in top_idx:
        all_features.append((agent, names[idx], mean_abs[idx]))

# Sort by SHAP value (highest first)
all_features.sort(key=lambda x: x[2], reverse=True)

# ── figure ──────────────────────────────────────────────────────────────
fig, ax = plt.subplots(figsize=(12, 8))

labels_display = [f"{agent}: {feat}" for agent, feat, _ in all_features]
values = [v for _, _, v in all_features]
bar_colors = [COLORS[a] for a, _, _ in all_features]

# Reverse for horizontal bar (highest at top)
labels_display = labels_display[::-1]
values = values[::-1]
bar_colors = bar_colors[::-1]

y_pos = np.arange(len(labels_display))
bars = ax.barh(y_pos, values, color=bar_colors, alpha=0.85,
               edgecolor="white", height=0.7)

# Value annotations
for bar, val in zip(bars, values):
    ax.text(val + max(values) * 0.02,
            bar.get_y() + bar.get_height() / 2,
            f"{val:.4f}", va="center", fontsize=8.5, fontweight="bold")

ax.set_yticks(y_pos)
ax.set_yticklabels(labels_display, fontsize=9)
ax.set_xlabel("Mean |SHAP Value|  (TreeSHAP, 500 samples)", fontsize=11)
ax.set_title(
    "Aggregated Feature Importance Across All Three Agents\n"
    "Top 8 features per agent, ranked by mean absolute SHAP value",
    fontsize=13, fontweight="bold",
)
ax.set_xlim(0, max(values) * 1.25)
ax.xaxis.grid(True, linestyle="--", alpha=0.3, zorder=0)
ax.set_axisbelow(True)

for sp in ("top", "right"):
    ax.spines[sp].set_visible(False)

# Legend
legend_patches = [
    mpatches.Patch(color=COLORS[a], label=f"{a} — {LABELS[a]}")
    for a in ["TPA", "CRA", "NAA"]
]
ax.legend(handles=legend_patches, loc="lower right", fontsize=10,
          framealpha=0.9, edgecolor="grey", title="Agent", title_fontsize=10)

plt.tight_layout()
out_path = OUT / "figZ_aggregated_feature_importance.png"
plt.savefig(out_path)
plt.close()
print(f"\n✅  Saved → {out_path}")
