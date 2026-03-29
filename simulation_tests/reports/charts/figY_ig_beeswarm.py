"""
Figure Y  (Fig 5.17): Integrated Gradients — Beeswarm Attribution
                      per Actor (scatter plot, 3 panels)
Colour = feature value (green=low risk → red=high risk).
For: Chapter 5, Section 5.5.2
"""

import sys, pathlib
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.cm as cm
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

# ── synthetic samples ───────────────────────────────────────────────────
np.random.seed(42)
n_samples = 500
leg_states   = np.random.beta(2, 8, (425, 6)).astype(np.float32)
fraud_states = np.random.beta(6, 2, (75, 6)).astype(np.float32)
X = np.vstack([leg_states, fraud_states])
X_t = torch.FloatTensor(X)

# ── compute IG ──────────────────────────────────────────────────────────
ig_scores = {}
for name, model in actors.items():
    ig_scores[name] = integrated_gradients(model, X_t)
    print(f"  IG computed for {name}")

# ── figure: beeswarm ────────────────────────────────────────────────────
fig, axes = plt.subplots(1, 3, figsize=(16, 6.5))
fig.suptitle(
    "Integrated Gradients — Attribution Distribution (Beeswarm)\n"
    "Positive = pushes toward BLOCK   |   Negative = pushes toward ALLOW",
    fontsize=12, fontweight="bold",
)

for ax, (name, (label, color)) in zip(axes, ACTOR_META.items()):
    ig = ig_scores[name]                           # [500, 6]

    for feat_idx in range(len(STATE_LABELS)):
        y_jitter = np.full(n_samples, feat_idx) + np.random.uniform(-0.35, 0.35, n_samples)
        sc = ax.scatter(
            ig[:, feat_idx], y_jitter,
            c=X[:, feat_idx], cmap="RdYlGn_r",
            alpha=0.35, s=10, vmin=0, vmax=1,
            edgecolors="none",
        )

    ax.axvline(0, color="black", lw=0.8, ls="--", alpha=0.6)
    ax.set_yticks(range(len(STATE_LABELS)))
    ax.set_yticklabels(STATE_LABELS, fontsize=9)
    ax.set_xlabel("IG Attribution for P(BLOCK)", fontsize=10)
    ax.set_title(label, fontsize=11, fontweight="bold")
    for sp in ("top", "right"):
        ax.spines[sp].set_visible(False)

# shared colorbar
sm = cm.ScalarMappable(cmap="RdYlGn_r", norm=plt.Normalize(0, 1))
sm.set_array([])
cbar = fig.colorbar(sm, ax=axes[-1], shrink=0.6, pad=0.08)
cbar.set_label("Feature value  (0 = low risk  →  1 = high risk)", fontsize=9)

plt.tight_layout()
out_path = OUT / "figY_ig_beeswarm.png"
plt.savefig(out_path)
plt.close()
print(f"\n✅  Saved → {out_path}")
