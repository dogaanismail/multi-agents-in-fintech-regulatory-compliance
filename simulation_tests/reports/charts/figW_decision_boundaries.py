"""
Figure W  (Fig 5.15): Actor Decision Boundaries — P(BLOCK) Heatmaps
Sweeps txn_prob × cust_prob while holding other dims at 0.5.
For: Chapter 5, Section 5.4.6
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

ACTORS_META = {
    "transaction": ("Transaction Pattern Agent (TPA)", "#2196F3"),
    "customer":    ("Customer Risk Agent (CRA)",       "#FF9800"),
    "network":     ("Network Analysis Agent (NAA)",    "#4CAF50"),
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


# ── rebuild actor architecture ──────────────────────────────────────────
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


# ── load actors ─────────────────────────────────────────────────────────
actors = {}
for name in ACTORS_META:
    sd = torch.load(MODELS / f"actor_{name}.pth", map_location="cpu", weights_only=True)
    m  = Actor()
    m.load_state_dict(sd)
    m.eval()
    actors[name] = m

# ── sweep grid ──────────────────────────────────────────────────────────
N = 80
prob_range = np.linspace(0, 1, N)
txn_g, cust_g = np.meshgrid(prob_range, prob_range)

grid_states = np.full((N * N, 6), 0.5, dtype=np.float32)
grid_states[:, 0] = txn_g.ravel()    # txn_prob
grid_states[:, 2] = cust_g.ravel()   # cust_prob
state_t = torch.FloatTensor(grid_states)

# ── plot ────────────────────────────────────────────────────────────────
fig, axs = plt.subplots(1, 3, figsize=(16, 5.5))
fig.suptitle(
    "Actor Decision Boundaries:  P(BLOCK)  across  txn_prob  x  cust_prob\n"
    "(net_prob = 0.5,  all risk scores = 0.5)",
    fontsize=12, fontweight="bold",
)

for ax, (name, (label, color)) in zip(axs, ACTORS_META.items()):
    with torch.no_grad():
        probs = actors[name](state_t).numpy()
    p_block = probs[:, 0].reshape(N, N)

    cf = ax.contourf(txn_g, cust_g, p_block,
                     levels=np.linspace(0, 1, 21), cmap="RdYlGn_r")
    ax.contour(txn_g, cust_g, p_block,
               levels=[0.5], colors="white", linewidths=2, linestyles="--")

    ax.set_title(label, fontsize=11, fontweight="bold")
    ax.set_xlabel("txn_prob", fontsize=10)
    ax.set_ylabel("cust_prob", fontsize=10)
    cb = plt.colorbar(cf, ax=ax)
    cb.set_label("P(BLOCK)", fontsize=9)

    # annotate P(BLOCK) at corners
    corners = [(0.05, 0.05), (0.05, 0.95), (0.95, 0.05), (0.95, 0.95)]
    for cx, cy in corners:
        ix = int(cx * (N - 1))
        iy = int(cy * (N - 1))
        pval = p_block[iy, ix]
        txt_color = "white" if pval > 0.6 or pval < 0.3 else "black"
        ax.text(cx, cy, f"{pval:.2f}", transform=ax.transAxes,
                fontsize=8, ha="center", va="center", color=txt_color,
                fontweight="bold",
                bbox=dict(boxstyle="round,pad=0.2", fc="black", alpha=0.35))

plt.tight_layout()
out_path = OUT / "figW_decision_boundaries.png"
plt.savefig(out_path)
plt.close()
print(f"✅  Saved → {out_path}")
