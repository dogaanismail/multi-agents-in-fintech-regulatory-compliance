"""
Figure T  (Fig 5.11): Weight Distributions — All Linear Layers
                       (Actor × 3  +  Centralised Critic)
Loads .pth checkpoints and plots a 4×4 histogram grid.
For: Chapter 5, Section 5.4.2
"""

import sys, pathlib
import numpy as np
import matplotlib.pyplot as plt
import torch

# ── paths ───────────────────────────────────────────────────────────────
ROOT = pathlib.Path(__file__).resolve().parents[3]
MODELS = ROOT / "ai-services" / "marl_orchestrator" / "training" / "trained_models"
OUT    = ROOT / "simulation_tests" / "reports" / "charts"

CHECKPOINTS = {
    "transaction": MODELS / "actor_transaction.pth",
    "customer":    MODELS / "actor_customer.pth",
    "network":     MODELS / "actor_network.pth",
    "critic":      MODELS / "critic.pth",
}
LABELS = {
    "transaction": "Actor — Transaction Pattern Agent",
    "customer":    "Actor — Customer Risk Agent",
    "network":     "Actor — Network Analysis Agent",
    "critic":      "Critic — Centralised (CTDE)",
}
COLORS = {
    "transaction": "#2196F3",
    "customer":    "#FF9800",
    "network":     "#4CAF50",
    "critic":      "#9C27B0",
}
LAYER_NAMES = ["fc1", "fc2", "fc3", "fc4"]

# ── style ───────────────────────────────────────────────────────────────
plt.rcParams.update({
    "font.family": "serif",
    "font.size": 9,
    "figure.dpi": 300,
    "savefig.dpi": 300,
    "savefig.bbox": "tight",
    "savefig.pad_inches": 0.3,
})

# ── load ────────────────────────────────────────────────────────────────
state_dicts = {
    k: torch.load(v, map_location="cpu", weights_only=True)
    for k, v in CHECKPOINTS.items()
}

# ── plot 4×4 grid ───────────────────────────────────────────────────────
fig, axes = plt.subplots(4, 4, figsize=(16, 13))
fig.suptitle(
    "Weight Distributions — All Linear Layers\n"
    "(Actor × 3  +  Centralised Critic)",
    fontsize=13, fontweight="bold", y=1.01,
)

for row, name in enumerate(["transaction", "customer", "network", "critic"]):
    sd    = state_dicts[name]
    color = COLORS[name]
    label = LABELS[name]

    # gather weight matrices (skip bias, skip BN)
    weight_keys = [k for k in sd if k.endswith(".weight") and "bn" not in k]

    for col in range(4):
        ax = axes[row][col]
        ax.set_facecolor("#FAFAFA")
        for sp in ("top", "right"):
            ax.spines[sp].set_visible(False)

        if col < len(weight_keys):
            key = weight_keys[col]
            w   = sd[key].numpy().flatten()
            ax.hist(w, bins=60, color=color, alpha=0.82,
                    edgecolor="white", linewidth=0.3)
            ax.axvline(0, color="red", ls="--", lw=0.8, alpha=0.6)

            layer_tag = LAYER_NAMES[col] if col < len(LAYER_NAMES) else key.replace(".weight", "")
            shape_str = "×".join(str(d) for d in sd[key].shape)

            ax.set_title(f"{label}\n{layer_tag}  [{shape_str}]",
                         fontsize=8, fontweight="bold")
            ax.set_xlabel("Weight value", fontsize=7)
            if col == 0:
                ax.set_ylabel("Frequency", fontsize=8)

            # annotate μ / σ
            stats_txt = f"μ = {w.mean():+.5f}\nσ = {w.std():.5f}\nn = {len(w):,}"
            ax.text(0.97, 0.95, stats_txt, transform=ax.transAxes,
                    fontsize=7, va="top", ha="right",
                    bbox=dict(boxstyle="round,pad=0.3", fc="white",
                              ec="grey", alpha=0.85))
        else:
            ax.axis("off")

plt.tight_layout()
out_path = OUT / "figT_weight_distributions.png"
plt.savefig(out_path)
plt.close()
print(f"✅  Saved → {out_path}")
