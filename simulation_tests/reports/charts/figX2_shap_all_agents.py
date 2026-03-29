"""
Chapter 5 — TreeSHAP Explainability for All Three Agents
=========================================================
Loads TPA (XGBoost), CRA (XGBoost), NAA (CatBoost) trained models,
generates synthetic data matching each agent's feature schema,
and produces SHAP summary/bar/beeswarm plots for all three.

Outputs → simulation_tests/reports/charts/
  figX2_shap_all_agents_bar.png      (mean |SHAP| bar chart, 3 panels)
  figX3_shap_all_agents_beeswarm.png (beeswarm, 3 panels)

Run:  python simulation_tests/reports/charts/figX2_shap_all_agents.py
"""

import warnings
warnings.filterwarnings("ignore")

import pathlib
import pickle
import json
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import joblib
import shap
import xgboost as xgb

# ── paths ───────────────────────────────────────────────────────────────
ROOT   = pathlib.Path(__file__).resolve().parents[3]
OUT    = ROOT / "simulation_tests" / "reports" / "charts"

TPA_MODEL = ROOT / "ai-services/agents/transaction_pattern_agent/trained_models/xgboost_transaction_pattern_agent.pkl"
CRA_MODEL = ROOT / "ai-services/agents/customer_risk_agent/trained_models/customer_risk_model.pkl"
NAA_MODEL = ROOT / "ai-services/agents/network_analysis_agent/trained_models/network_analysis_catboost_model.pkl"
NAA_META  = ROOT / "ai-services/agents/network_analysis_agent/trained_models/network_analysis_catboost_metadata.json"

COLORS = {"TPA": "#2196F3", "CRA": "#FF9800", "NAA": "#4CAF50"}

# ── style ───────────────────────────────────────────────────────────────
plt.rcParams.update({
    "font.family": "serif",
    "font.size": 10,
    "figure.dpi": 300,
    "savefig.dpi": 300,
    "savefig.bbox": "tight",
    "savefig.pad_inches": 0.3,
})

# =====================================================================
# 1. Load models
# =====================================================================
print("Loading models...")

# TPA — XGBoost (57 features after one-hot encoding)
tpa_model = joblib.load(TPA_MODEL)
TPA_N_FEATURES = tpa_model.n_features_in_  # 57

# Reconstruct meaningful feature names for TPA
# Raw: Amount (scaled), then OneHot of Payment_type(7), Payment_currency(13),
#      Sender_bank_location(16), Receiver_bank_location(18) → 1+7+13+16+18 = 55
# Actual n_features may differ slightly; pad or trim to match.
_PAY_TYPES = ["ACH", "Cash Deposit", "Cash Withdrawal", "Cheque",
              "Credit card", "Cross-border", "Debit card"]
_CURRENCIES = ["Albanian lek", "Dirham", "Euro", "Indian rupee", "Mexican Peso",
               "Moroccan dirham", "Naira", "Pakistani rupee", "Swiss franc",
               "Turkish lira", "UK pounds", "US dollar", "Yen"]
_SENDER_LOCS = ["Albania", "Austria", "France", "Germany", "India", "Italy",
                "Japan", "Mexico", "Morocco", "Nigeria", "Pakistan", "Spain",
                "Switzerland", "Turkey", "UAE", "UK"]
_RECV_LOCS = ["Albania", "Austria", "France", "Germany", "India", "Italy",
              "Japan", "Mexico", "Netherlands", "Nigeria", "Pakistan", "Spain",
              "Switzerland", "Turkey", "UAE", "UK", "USA"]
# Extra: some sklearn versions add a residual column
_EXTRA = ["Receiver_USA_2"]

tpa_feature_names = ["Amount"]
groups = [
    ("PayType", _PAY_TYPES),
    ("Ccy", _CURRENCIES),
    ("Sender", _SENDER_LOCS),
    ("Receiver", _RECV_LOCS),
]
for prefix, cats in groups:
    for c in cats:
        tpa_feature_names.append(f"{prefix}: {c}")
# Pad or trim to match actual model width
while len(tpa_feature_names) < TPA_N_FEATURES:
    tpa_feature_names.append(f"extra_{len(tpa_feature_names)}")
tpa_feature_names = tpa_feature_names[:TPA_N_FEATURES]

print(f"  TPA: {type(tpa_model).__name__}, {TPA_N_FEATURES} features")

# CRA — XGBoost (19 features)
with open(CRA_MODEL, "rb") as f:
    cra_bundle = pickle.load(f)
cra_model = cra_bundle["model"]
cra_feature_names = cra_bundle["feature_names"]
cra_scaler = cra_bundle["scaler"]
print(f"  CRA: {type(cra_model).__name__}, {len(cra_feature_names)} features")

# NAA — CatBoost (11 features)
with open(NAA_MODEL, "rb") as f:
    naa_model = pickle.load(f)
with open(NAA_META) as f:
    naa_meta = json.load(f)
naa_feature_names = naa_meta["features"]["list"]
print(f"  NAA: {type(naa_model).__name__}, {len(naa_feature_names)} features")

# =====================================================================
# 2. Generate synthetic data for each agent
# =====================================================================
print("\nGenerating synthetic samples...")
np.random.seed(42)
N = 500  # samples per agent

# TPA: 57 features — Amount (continuous) + one-hot categories
tpa_X = np.zeros((N, TPA_N_FEATURES), dtype=np.float32)
tpa_X[:, 0] = np.random.lognormal(mean=7.0, sigma=2.0, size=N)  # Amount (scaled)
# For each one-hot group, randomly activate one category per sample
offset = 1
for prefix, cats in groups:
    n_cats = len(cats)
    actual = min(n_cats, TPA_N_FEATURES - offset)
    if actual <= 0:
        break
    for i in range(N):
        col = np.random.randint(0, actual)
        tpa_X[i, offset + col] = 1.0
    offset += actual

tpa_df = pd.DataFrame(tpa_X, columns=tpa_feature_names[:TPA_N_FEATURES])
print(f"  TPA synthetic: {tpa_df.shape}")

# CRA: 19 continuous features (matching the scaler's expected input)
cra_raw = np.zeros((N, len(cra_feature_names)), dtype=np.float32)
# Generate realistic ranges for each feature
cra_ranges = {
    "transaction_count": (1, 500),
    "total_amount": (100, 500000),
    "avg_amount": (50, 50000),
    "median_amount": (50, 30000),
    "max_amount": (100, 200000),
    "min_amount": (1, 5000),
    "std_amount": (0, 50000),
    "active_days": (1, 365),
    "transactions_per_day": (0.01, 20),
    "cross_border_ratio": (0, 1),
    "cash_transaction_ratio": (0, 1),
    "amount_consistency": (0, 1),
    "large_transaction_ratio": (0, 1),
    "unique_receivers": (1, 200),
    "unique_receiver_countries": (1, 30),
    "receiver_diversity": (0, 1),
    "night_transaction_ratio": (0, 1),
    "weekend_transaction_ratio": (0, 1),
    "unique_currencies": (1, 10),
}
for j, fname in enumerate(cra_feature_names):
    lo, hi = cra_ranges.get(fname, (0, 1))
    cra_raw[:, j] = np.random.uniform(lo, hi, N).astype(np.float32)

# Scale with the saved scaler
cra_X = cra_scaler.transform(cra_raw)
cra_df = pd.DataFrame(cra_X, columns=cra_feature_names)
print(f"  CRA synthetic: {cra_df.shape}")

# NAA: 11 topology features
naa_raw = np.zeros((N, len(naa_feature_names)), dtype=np.float32)
naa_ranges = {
    "in_degree": (0, 500),
    "out_degree": (0, 500),
    "degree_centrality": (0, 0.01),
    "in_degree_centrality": (0, 0.01),
    "out_degree_centrality": (0, 0.01),
    "betweenness_centrality": (0, 0.001),
    "closeness_centrality": (0, 1),
    "pagerank": (0, 0.001),
    "eigenvector_centrality": (0, 0.1),
    "clustering_coefficient": (0, 1),
    "community": (0, 50),
}
for j, fname in enumerate(naa_feature_names):
    lo, hi = naa_ranges.get(fname, (0, 1))
    naa_raw[:, j] = np.random.uniform(lo, hi, N).astype(np.float32)

naa_df = pd.DataFrame(naa_raw, columns=naa_feature_names)
print(f"  NAA synthetic: {naa_df.shape}")

# =====================================================================
# 3. Compute SHAP values
# =====================================================================
print("\nComputing SHAP values...")

# TPA — TreeExplainer
tpa_explainer = shap.TreeExplainer(tpa_model)
tpa_shap = tpa_explainer.shap_values(tpa_df)
# For binary classification, shap_values may return list [class0, class1]
if isinstance(tpa_shap, list):
    tpa_shap = tpa_shap[1]  # class 1 = fraud
elif tpa_shap.ndim == 3:
    tpa_shap = tpa_shap[:, :, 1]
print(f"  TPA SHAP shape: {tpa_shap.shape}")

# CRA — TreeExplainer
cra_explainer = shap.TreeExplainer(cra_model)
cra_shap = cra_explainer.shap_values(cra_df)
if isinstance(cra_shap, list):
    cra_shap = cra_shap[1]
elif cra_shap.ndim == 3:
    cra_shap = cra_shap[:, :, 1]
print(f"  CRA SHAP shape: {cra_shap.shape}")

# NAA — TreeExplainer (CatBoost is supported)
naa_explainer = shap.TreeExplainer(naa_model)
naa_shap = naa_explainer.shap_values(naa_df)
if isinstance(naa_shap, list):
    naa_shap = naa_shap[1]
elif naa_shap.ndim == 3:
    naa_shap = naa_shap[:, :, 1]
print(f"  NAA SHAP shape: {naa_shap.shape}")

# =====================================================================
# 4. Figure X2: Mean |SHAP| bar chart — all 3 agents
# =====================================================================
print("\nGenerating figures...")

fig, axes = plt.subplots(1, 3, figsize=(18, 6.5))
fig.suptitle(
    "TreeSHAP Feature Importance — Mean |SHAP Value| per Agent\n"
    "(XGBoost for TPA & CRA,  CatBoost for NAA;  500 synthetic samples)",
    fontsize=13, fontweight="bold",
)

agent_data = [
    ("TPA", tpa_shap, tpa_feature_names, COLORS["TPA"], "Transaction Pattern Agent\n(XGBoost, 57 features)"),
    ("CRA", cra_shap, cra_feature_names, COLORS["CRA"], "Customer Risk Agent\n(XGBoost, 19 features)"),
    ("NAA", naa_shap, naa_feature_names, COLORS["NAA"], "Network Analysis Agent\n(CatBoost, 11 features)"),
]

for ax, (short, shap_vals, feat_names, color, title) in zip(axes, agent_data):
    mean_abs = np.abs(shap_vals).mean(axis=0)
    
    # Show top 12 features (or all if fewer)
    top_k = min(12, len(feat_names))
    top_idx = np.argsort(mean_abs)[::-1][:top_k]
    
    # Reverse for horizontal bar (top feature at top)
    display_names = [feat_names[i] for i in top_idx][::-1]
    display_vals  = mean_abs[top_idx][::-1]
    
    bars = ax.barh(display_names, display_vals, color=color, alpha=0.82,
                   edgecolor="white", height=0.65)
    
    for bar, val in zip(bars, display_vals):
        ax.text(val + display_vals.max() * 0.03,
                bar.get_y() + bar.get_height() / 2,
                f"{val:.4f}", va="center", fontsize=8, fontweight="bold")
    
    ax.set_title(title, fontsize=10, fontweight="bold")
    ax.set_xlabel("Mean |SHAP value|", fontsize=9)
    ax.set_xlim(0, display_vals.max() * 1.35)
    
    for sp in ("top", "right"):
        ax.spines[sp].set_visible(False)

plt.tight_layout()
out1 = OUT / "figX2_shap_all_agents_bar.png"
plt.savefig(out1)
plt.close()
print(f"  ✅ {out1.name}")

# =====================================================================
# 5. Figure X3: Beeswarm — all 3 agents (top 10 features each)
# =====================================================================
fig, axes = plt.subplots(1, 3, figsize=(18, 7))
fig.suptitle(
    "TreeSHAP Beeswarm — Feature Attribution Distribution per Agent\n"
    "Positive SHAP = pushes toward fraud prediction   |   Colour = feature value",
    fontsize=13, fontweight="bold",
)

for ax, (short, shap_vals, feat_names, color, title) in zip(axes, agent_data):
    mean_abs = np.abs(shap_vals).mean(axis=0)
    top_k = min(10, len(feat_names))
    top_idx = np.argsort(mean_abs)[::-1][:top_k]
    
    # Plot each feature as a horizontal strip
    for rank, fi in enumerate(reversed(top_idx)):
        y_jitter = np.full(N, rank) + np.random.uniform(-0.35, 0.35, N)
        
        # Get raw feature values for colouring
        if short == "TPA":
            feat_raw = tpa_df.iloc[:, fi].values
        elif short == "CRA":
            feat_raw = cra_raw[:, fi]  # use pre-scaled values for colour
        else:
            feat_raw = naa_raw[:, fi]
        
        # Normalise to [0, 1] for colourmap
        fmin, fmax = feat_raw.min(), feat_raw.max()
        if fmax > fmin:
            feat_norm = (feat_raw - fmin) / (fmax - fmin)
        else:
            feat_norm = np.zeros_like(feat_raw)
        
        ax.scatter(shap_vals[:, fi], y_jitter,
                   c=feat_norm, cmap="RdYlGn_r",
                   alpha=0.35, s=8, vmin=0, vmax=1,
                   edgecolors="none")
    
    ax.axvline(0, color="black", lw=0.8, ls="--", alpha=0.6)
    ax.set_yticks(range(top_k))
    ax.set_yticklabels([feat_names[i] for i in reversed(top_idx)], fontsize=8)
    ax.set_xlabel("SHAP value (fraud class)", fontsize=9)
    ax.set_title(title.split("\n")[0], fontsize=10, fontweight="bold")
    
    for sp in ("top", "right"):
        ax.spines[sp].set_visible(False)

# Shared colourbar
sm = plt.cm.ScalarMappable(cmap="RdYlGn_r", norm=plt.Normalize(0, 1))
sm.set_array([])
cbar = fig.colorbar(sm, ax=axes[-1], shrink=0.5, pad=0.08)
cbar.set_label("Feature value  (low → high)", fontsize=9)

plt.tight_layout()
out2 = OUT / "figX3_shap_all_agents_beeswarm.png"
plt.savefig(out2)
plt.close()
print(f"  ✅ {out2.name}")

print("\nDone! Both figures saved.")
