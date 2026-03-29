"""
Figure U  (Fig 5.12): Actor Specialisation — fc1 Weight Divergence
Three side-by-side histograms + pairwise MAD annotation.
For: Chapter 5, Section 5.4.3
"""

import sys, pathlib
import numpy as np
import matplotlib.pyplot as plt
import torch

# ── paths ───────────────────────────────────────────────────────────────
ROOT   = pathlib.Path(__file__).resolve().parents[3]
MODELS = ROOT / "ai-services" / "marl_orchestrator" / "training" / "trained_models"
OUT    = ROOT / "simulation_tests" / "reports" / "charts"

ACTORS = {
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

# ── load ────────────────────────────────────────────────────────────────
sds = {
    k: torch.load(MODELS / f"actor_{k}.pth", map_location="cpu", weights_only=True)
    for k in ACTORS
}

# ── plot ────────────────────────────────────────────────────────────────
fig, axes = plt.subplots(1, 3, figsize=(15, 5), sharey=False)
fig.suptitle(
    "Actor Specialisation — fc1 (Input Layer) Weight Distributions\n"
    "Divergence from identical Xavier initialisation confirms domain-specific gradient signals",
    fontsize=12, fontweight="bold",
)

fc1_weights = {}
for ax, (name, (label, color)) in zip(axes, ACTORS.items()):
    w = sds[name]["fc1.weight"].numpy().flatten()
    fc1_weights[name] = sds[name]["fc1.weight"]

    ax.hist(w, bins=70, color=color, alpha=0.82, edgecolor="white", lw=0.3)
    ax.axvline(0, color="red", ls="--", lw=1, alpha=0.7)

    ax.set_title(f"{label}\nfc1  σ = {w.std():.5f}", fontsize=10, fontweight="bold")
    ax.set_xlabel("Weight value", fontsize=9)
    if ax == axes[0]:
        ax.set_ylabel("Frequency", fontsize=10)

    for sp in ("top", "right"):
        ax.spines[sp].set_visible(False)

    # shape annotation
    shape_str = "×".join(str(d) for d in sds[name]["fc1.weight"].shape)
    ax.text(0.97, 0.85, f"Shape: [{shape_str}]\nn = {len(w):,}",
            transform=ax.transAxes, fontsize=8, va="top", ha="right",
            bbox=dict(boxstyle="round,pad=0.3", fc="white", ec="grey", alpha=0.85))

# ── pairwise MAD annotation box below the plots ────────────────────────
names = list(ACTORS.keys())
pairs = []
for i in range(len(names)):
    for j in range(i + 1, len(names)):
        mad = (fc1_weights[names[i]] - fc1_weights[names[j]]).abs().mean().item()
        a_short = ACTORS[names[i]][0].split("(")[1].rstrip(")")
        b_short = ACTORS[names[j]][0].split("(")[1].rstrip(")")
        pairs.append(f"{a_short} vs {b_short}: MAD = {mad:.6f}")

mad_text = "Pairwise Mean Absolute Difference (fc1.weight):   " + "   |   ".join(pairs)
fig.text(0.5, -0.02, mad_text, ha="center", fontsize=9, fontstyle="italic",
         bbox=dict(boxstyle="round,pad=0.5", fc="#F5F5F5", ec="#BDBDBD"))

plt.tight_layout()
out_path = OUT / "figU_actor_specialisation.png"
plt.savefig(out_path)
plt.close()
print(f"✅  Saved → {out_path}")
