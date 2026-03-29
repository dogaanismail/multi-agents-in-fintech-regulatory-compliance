"""
Figure V  (Fig 5.13): Centralised Critic CTDE Decomposition
Heatmap of fc1 weight matrix split into State vs Joint-Action regions.
For: Chapter 5, Section 5.4.4
"""

import sys, pathlib
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import torch

# ── paths ───────────────────────────────────────────────────────────────
ROOT   = pathlib.Path(__file__).resolve().parents[3]
MODELS = ROOT / "ai-services" / "marl_orchestrator" / "training" / "trained_models"
OUT    = ROOT / "simulation_tests" / "reports" / "charts"

# ── style ───────────────────────────────────────────────────────────────
plt.rcParams.update({
    "font.family": "serif",
    "font.size": 10,
    "figure.dpi": 300,
    "savefig.dpi": 300,
    "savefig.bbox": "tight",
    "savefig.pad_inches": 0.3,
})

# ── load critic ─────────────────────────────────────────────────────────
critic_sd = torch.load(MODELS / "critic.pth", map_location="cpu", weights_only=True)
fc1_w = critic_sd["fc1.weight"].numpy()          # shape [256, 12]
vmax  = float(np.abs(fc1_w).max())

state_cols  = fc1_w[:, :6]                        # columns 0-5
action_cols = fc1_w[:, 6:]                         # columns 6-11

# ── figure ──────────────────────────────────────────────────────────────
fig, (ax_s, ax_a) = plt.subplots(1, 2, figsize=(15, 6.5))
fig.suptitle(
    "Centralised Critic fc1 — State vs. Joint-Action Input Decomposition\n"
    "(red = positive weight  |  blue = negative weight)",
    fontsize=12, fontweight="bold",
)

# --- state panel ---
im0 = ax_s.imshow(state_cols, aspect="auto", cmap="RdBu_r",
                   vmin=-vmax, vmax=vmax, interpolation="nearest")
ax_s.set_title("State portion  (columns 0–5)", fontsize=11, fontweight="bold")
ax_s.set_xlabel("State feature index", fontsize=10)
ax_s.set_ylabel("Hidden neuron index  (256 neurons)", fontsize=10)
ax_s.set_xticks(range(6))
ax_s.set_xticklabels(
    ["s0\ntxn_prob", "s1\ntxn_risk", "s2\ncust_prob",
     "s3\ncust_risk", "s4\nnet_prob", "s5\nnet_risk"],
    fontsize=8,
)
cb0 = plt.colorbar(im0, ax=ax_s, fraction=0.046, pad=0.04)
cb0.set_label("Weight", fontsize=9)

# --- action panel ---
im1 = ax_a.imshow(action_cols, aspect="auto", cmap="RdBu_r",
                   vmin=-vmax, vmax=vmax, interpolation="nearest")
ax_a.set_title("Joint-action portion  (columns 6–11)", fontsize=11, fontweight="bold")
ax_a.set_xlabel("Action index", fontsize=10)
ax_a.set_ylabel("Hidden neuron index  (256 neurons)", fontsize=10)
ax_a.set_xticks(range(6))
ax_a.set_xticklabels(
    ["a0_0\nTPA BLOCK", "a0_1\nTPA ALLOW",
     "a1_0\nCRA BLOCK", "a1_1\nCRA ALLOW",
     "a2_0\nNAA BLOCK", "a2_1\nNAA ALLOW"],
    fontsize=8,
)
cb1 = plt.colorbar(im1, ax=ax_a, fraction=0.046, pad=0.04)
cb1.set_label("Weight", fontsize=9)

# ── L2-norm annotation ──────────────────────────────────────────────────
s_norm = float(np.linalg.norm(state_cols))
a_norm = float(np.linalg.norm(action_cols))
ratio  = s_norm / a_norm

norm_text = (
    f"L2 Norms\n"
    f"  State  (6 cols):  {s_norm:.4f}  (avg {s_norm/6:.4f})\n"
    f"  Action (6 cols):  {a_norm:.4f}  (avg {a_norm/6:.4f})\n"
    f"  Ratio s/a:  {ratio:.4f}  ({'state-dominant' if ratio > 1 else 'action-dominant'})"
)
fig.text(
    0.5, -0.04, norm_text,
    ha="center", fontsize=10, fontfamily="monospace",
    bbox=dict(boxstyle="round,pad=0.6", fc="#F5F5F5", ec="#BDBDBD"),
)

plt.tight_layout()
out_path = OUT / "figV_critic_ctde_decomposition.png"
plt.savefig(out_path)
plt.close()
print(f"✅  Saved → {out_path}")
