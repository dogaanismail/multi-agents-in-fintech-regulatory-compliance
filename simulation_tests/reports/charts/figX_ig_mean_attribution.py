"""
Figure X  (Fig 5.16): Integrated Gradients — Mean |IG| Feature Attribution
                      per Actor (bar chart, 3 panels)
Loads .pth checkpoints, runs IG with zero baseline, 50 Riemann steps.
For: Chapter 5, Section 5.5.2
"""

import sys, pathlib
import numpy as np
import matplotlib.pyplot as plt
import torch
import torch.nn as nn
import torch.nn.functional as F

# ── paths ───────────────────────────────────────────────────────────────
ROOT   = pathlib.Path(__file__).resolve().parents[3]
MODELS = ROOT / "ai-services" / "marl_orchestrator" / "training" / "trained_models"
OUT    = ROOT / "simulation_tests" / "reports" / "charts"

ACTOR_META = {
    "transaction": ("Transaction Pattern Agent (TPA)", "#2196F3"),
    "customer":    ("Customer Risk Agent (CRA)",       "#FF9800"),
    "network":     ("Network Analysis Agent (NAA)",    "#4CAF50"),
}
STATE_LABELS = ["txn_prob", "txn_risk", "cust_prob", "cust_risk", "net_prob", "net_risk"]

# ── style ───────────────────────────────────────────────────────────────
plt.rcParams.update({
    "font.family": "serif",
    "font.size": 10,
    "figure.dpi": 300,
    "savefig.dpi": 300,
    "savefig.bbox": "tight",
    "savefig.pad_inches": 0.3,
})


# ── actor architecture ──────────────────────────────────────────────────
class Actor(nn.Module):
    def __init__(self, state_dim=6, action_dim=2, hidden_dim=256):
        super().__init__()
        self.fc1 = nn.Linear(state_dim, hidden_dim)
        self.bn1 = nn.BatchNorm1d(hidden_dim)
        self.fc2 = nn.Linear(hidden_dim, hidden_dim)
        self.bn2 = nn.BatchNorm1d(hidden_dim)
        self.fc3 = nn.Linear(hidden_dim, hidden_dim // 2)
        self.bn3 = nn.BatchNorm1d(hidden_dim // 2)
        self.fc4 = nn.Linear(hidden_dim // 2, action_dim)

    def forward(self, x):
        if x.size(0) > 1:
            x = F.relu(self.bn1(self.fc1(x)))
            x = F.relu(self.bn2(self.fc2(x)))
            x = F.relu(self.bn3(self.fc3(x)))
        else:
            x = F.relu(self.fc1(x))
            x = F.relu(self.fc2(x))
            x = F.relu(self.fc3(x))
        return F.softmax(self.fc4(x), dim=-1)


# ── Integrated Gradients ────────────────────────────────────────────────
def integrated_gradients(model, inputs, baseline=None, steps=50):
    """IG attribution for P(BLOCK) output (column 0)."""
    if baseline is None:
        baseline = torch.zeros_like(inputs)
    alphas = torch.linspace(0, 1, steps)
    scaled = baseline.unsqueeze(0) + alphas[:, None, None] * (inputs - baseline).unsqueeze(0)
    scaled = scaled.view(-1, inputs.shape[1]).requires_grad_(True)
    out = model(scaled)[:, 0]
    grad = torch.autograd.grad(out.sum(), scaled)[0]
    grad = grad.view(steps, inputs.shape[0], inputs.shape[1])
    avg_grad = grad.mean(dim=0)
    ig = (inputs - baseline) * avg_grad
    return ig.detach().numpy()


# ── load actors ─────────────────────────────────────────────────────────
actors = {}
for name in ACTOR_META:
    sd = torch.load(MODELS / f"actor_{name}.pth", map_location="cpu", weights_only=True)
    m = Actor(); m.load_state_dict(sd); m.eval()
    actors[name] = m
    print(f"  Loaded {name}")

# ── synthetic samples ───────────────────────────────────────────────────
np.random.seed(42)
leg_states   = np.random.beta(2, 8, (425, 6)).astype(np.float32)   # low-risk
fraud_states = np.random.beta(6, 2, (75, 6)).astype(np.float32)    # high-risk
X = np.vstack([leg_states, fraud_states])
X_t = torch.FloatTensor(X)
print(f"  Samples: {X.shape[0]} (425 legit + 75 fraud)")

# ── compute IG ──────────────────────────────────────────────────────────
ig_scores = {}
for name, model in actors.items():
    ig_scores[name] = integrated_gradients(model, X_t)
    print(f"  IG computed for {name}: shape {ig_scores[name].shape}")

# ── figure: Mean |IG| bar chart ─────────────────────────────────────────
fig, axes = plt.subplots(1, 3, figsize=(16, 5.5), sharey=False)
fig.suptitle(
    "Integrated Gradients Feature Attribution — Mean |IG| per Actor\n"
    "(higher = greater influence on P(BLOCK);  500 samples, 50 Riemann steps, zero baseline)",
    fontsize=12, fontweight="bold",
)

for ax, (name, (label, color)) in zip(axes, ACTOR_META.items()):
    ig = ig_scores[name]                           # [500, 6]
    mean_abs = np.abs(ig).mean(axis=0)             # [6]
    rank_idx = np.argsort(mean_abs)[::-1]

    bars = ax.barh(
        [STATE_LABELS[i] for i in rank_idx],
        mean_abs[rank_idx],
        color=color, alpha=0.82, edgecolor="white", height=0.6,
    )
    ax.set_title(label, fontsize=11, fontweight="bold")
    ax.set_xlabel("Mean |IG attribution|", fontsize=10)
    if ax == axes[0]:
        ax.set_ylabel("State feature", fontsize=10)

    for bar, val in zip(bars, mean_abs[rank_idx]):
        ax.text(val + mean_abs.max() * 0.03,
                bar.get_y() + bar.get_height() / 2,
                f"{val:.4f}", va="center", fontsize=9, fontweight="bold")

    ax.set_xlim(0, mean_abs.max() * 1.35)
    for sp in ("top", "right"):
        ax.spines[sp].set_visible(False)

plt.tight_layout()
out_path = OUT / "figX_ig_mean_attribution.png"
plt.savefig(out_path)
plt.close()
print(f"\n✅  Saved → {out_path}")
