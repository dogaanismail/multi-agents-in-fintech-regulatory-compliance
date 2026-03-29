"""
Figure L: MADDPG CTDE Architecture Diagram
Shows the Centralised Training with Decentralised Execution paradigm:
- 3 Actor networks (TPA, CRA, NAA) each seeing local observation
- 1 Centralised Critic seeing joint state + all actions
- Training vs Execution paths
For: Chapter 2, Section 2.4.3 (MADDPG)
"""

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import numpy as np

# ── Style ──────────────────────────────────────────────────────────────
plt.rcParams.update({
    'font.family': 'serif',
    'font.size': 10,
    'axes.titlesize': 14,
    'figure.dpi': 300,
    'savefig.dpi': 300,
    'savefig.bbox': 'tight',
    'savefig.pad_inches': 0.3,
})

fig, ax = plt.subplots(figsize=(14, 9))
ax.set_xlim(-1, 15)
ax.set_ylim(-1, 10)
ax.set_aspect('equal')
ax.axis('off')

# ── Color palette ──────────────────────────────────────────────────────
C = {
    'tpa':       '#2196F3',  # Blue
    'cra':       '#4CAF50',  # Green
    'naa':       '#FF9800',  # Orange
    'critic':    '#37474F',  # Dark slate
    'env':       '#78909C',  # Blue grey
    'reward':    '#E91E63',  # Pink
    'replay':    '#7E57C2',  # Purple
    'target':    '#90A4AE',  # Light slate
    'bg_train':  '#E3F2FD',  # Light blue bg
    'bg_exec':   '#E8F5E9',  # Light green bg
    'arrow':     '#455A64',
    'text':      '#263238',
}

def draw_box(ax, x, y, w, h, label, sublabel, color, text_color='white', fontsize=9):
    box = FancyBboxPatch((x, y), w, h,
                         boxstyle="round,pad=0.12",
                         facecolor=color, edgecolor='white',
                         linewidth=1.5, alpha=0.92, zorder=5)
    ax.add_patch(box)
    if sublabel:
        ax.text(x + w/2, y + h/2 + 0.12, label,
                ha='center', va='center', fontsize=fontsize,
                fontweight='bold', color=text_color, zorder=6)
        ax.text(x + w/2, y + h/2 - 0.2, sublabel,
                ha='center', va='center', fontsize=fontsize - 2,
                color=text_color, alpha=0.85, zorder=6, style='italic')
    else:
        ax.text(x + w/2, y + h/2, label,
                ha='center', va='center', fontsize=fontsize,
                fontweight='bold', color=text_color, zorder=6)

def draw_arrow(ax, x1, y1, x2, y2, color='#455A64', style='->', lw=1.5, ls='-'):
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=style, color=color,
                                lw=lw, linestyle=ls),
                zorder=4)

# ── Background regions ────────────────────────────────────────────────

# Training region (left)
train_bg = FancyBboxPatch((0.2, 0.3), 6.6, 9.0,
                          boxstyle="round,pad=0.2",
                          facecolor=C['bg_train'], edgecolor='#90CAF9',
                          linewidth=1.5, alpha=0.5, zorder=1)
ax.add_patch(train_bg)
ax.text(3.5, 9.0, 'TRAINING PHASE', ha='center', va='center',
        fontsize=11, fontweight='bold', color='#1565C0', zorder=2)
ax.text(3.5, 8.6, '(Centralised — full observability)',
        ha='center', va='center', fontsize=8, color='#1565C0', alpha=0.7, zorder=2)

# Execution region (right)
exec_bg = FancyBboxPatch((7.4, 0.3), 6.6, 9.0,
                         boxstyle="round,pad=0.2",
                         facecolor=C['bg_exec'], edgecolor='#A5D6A7',
                         linewidth=1.5, alpha=0.5, zorder=1)
ax.add_patch(exec_bg)
ax.text(10.7, 9.0, 'EXECUTION PHASE', ha='center', va='center',
        fontsize=11, fontweight='bold', color='#2E7D32', zorder=2)
ax.text(10.7, 8.6, '(Decentralised — local observation only)',
        ha='center', va='center', fontsize=8, color='#2E7D32', alpha=0.7, zorder=2)

# ── TRAINING SIDE ─────────────────────────────────────────────────────

# Environment / State
draw_box(ax, 2.3, 7.3, 2.4, 0.75, 'Environment', 's = [6-dim state]', C['env'])

# Three actors (training)
actor_y = 5.5
actor_w, actor_h = 1.6, 0.75
draw_box(ax, 0.6,  actor_y, actor_w, actor_h, 'Actor₁ (TPA)', 'πφ₁(s)', C['tpa'])
draw_box(ax, 2.7,  actor_y, actor_w, actor_h, 'Actor₂ (CRA)', 'πφ₂(s)', C['cra'])
draw_box(ax, 4.8,  actor_y, actor_w, actor_h, 'Actor₃ (NAA)', 'πφ₃(s)', C['naa'])

# Arrows: environment → actors
for ax_pos in [1.4, 3.5, 5.6]:
    draw_arrow(ax, 3.5, 7.3, ax_pos, actor_y + actor_h, color=C['env'])

# Actions labels
ax.text(1.4, 5.15, 'a₁', ha='center', fontsize=8, color=C['tpa'], fontweight='bold')
ax.text(3.5, 5.15, 'a₂', ha='center', fontsize=8, color=C['cra'], fontweight='bold')
ax.text(5.6, 5.15, 'a₃', ha='center', fontsize=8, color=C['naa'], fontweight='bold')

# Centralised Critic
draw_box(ax, 1.8, 3.3, 3.4, 0.85, 'Centralised Critic', 'Qθ(s, a₁, a₂, a₃)', C['critic'], fontsize=10)

# Arrows: actors → critic (actions)
for ax_pos in [1.4, 3.5, 5.6]:
    draw_arrow(ax, ax_pos, actor_y, 3.5, 3.3 + 0.85, color=C['arrow'])

# Arrow: environment → critic (state)
draw_arrow(ax, 2.3, 7.3, 2.2, 3.3 + 0.85, color=C['env'], ls='--')
ax.text(1.5, 6.0, 's', ha='center', fontsize=8, color=C['env'],
        fontweight='bold', style='italic')

# Replay Buffer
draw_box(ax, 1.8, 1.5, 3.4, 0.7, 'Replay Buffer', '(s, a, r, s′, d)', C['replay'])

# Arrow: critic ↔ replay
draw_arrow(ax, 3.5, 3.3, 3.5, 1.5 + 0.7, color=C['replay'])
draw_arrow(ax, 3.5, 1.5, 3.5, 1.0, color=C['replay'])

# Reward signal
draw_box(ax, 5.2, 1.5, 1.4, 0.7, 'Reward', 'r (scalar)', C['reward'])

# Arrow: reward → replay
draw_arrow(ax, 5.2, 1.85, 5.2, 1.85, color=C['reward'])
ax.annotate('', xy=(5.2, 1.85), xytext=(5.9, 1.85),
            arrowprops=dict(arrowstyle='<-', color=C['reward'], lw=1.5), zorder=4)

# Target networks (subtle)
draw_box(ax, 1.0, 0.5, 2.2, 0.55, 'Target Networks', 'θ′ ← τθ + (1−τ)θ′',
         C['target'], text_color='#37474F', fontsize=8)

# Arrow: critic → target (soft update)
draw_arrow(ax, 3.5, 1.5, 2.1, 0.5 + 0.55, color=C['target'], ls='--')

# ── EXECUTION SIDE ────────────────────────────────────────────────────

# Payment transaction (input)
draw_box(ax, 9.2, 7.3, 3.0, 0.75, 'Payment Transaction', 'Feature extraction', C['env'])

# Three actors (execution) — identical copies
exec_actor_y = 5.5
draw_box(ax, 8.0,  exec_actor_y, 1.6, 0.75, 'Actor₁ (TPA)', 'πφ₁(oᵢ)', C['tpa'])
draw_box(ax, 9.9,  exec_actor_y, 1.6, 0.75, 'Actor₂ (CRA)', 'πφ₂(oᵢ)', C['cra'])
draw_box(ax, 11.8, exec_actor_y, 1.6, 0.75, 'Actor₃ (NAA)', 'πφ₃(oᵢ)', C['naa'])

# Arrows: transaction → actors (local observations)
for ax_pos in [8.8, 10.7, 12.6]:
    draw_arrow(ax, 10.7, 7.3, ax_pos, exec_actor_y + 0.75, color=C['env'])

# Local observation labels
ax.text(8.8,  5.15, 'o₁', ha='center', fontsize=8, color=C['tpa'], fontweight='bold')
ax.text(10.7, 5.15, 'o₂', ha='center', fontsize=8, color=C['cra'], fontweight='bold')
ax.text(12.6, 5.15, 'o₃', ha='center', fontsize=8, color=C['naa'], fontweight='bold')

# Decision Maker (weighted voting)
draw_box(ax, 9.2, 3.3, 3.0, 0.85, 'Decision Maker', 'Weighted voting → action',
         C['critic'], fontsize=10)

# Arrows: actors → decision maker
for ax_pos in [8.8, 10.7, 12.6]:
    draw_arrow(ax, ax_pos, exec_actor_y, 10.7, 3.3 + 0.85, color=C['arrow'])

# Final output
draw_box(ax, 9.0, 1.5, 3.4, 0.7, 'ALLOW  |  REVIEW  |  BLOCK', None,
         '#37474F', fontsize=10)

# Arrow: decision → output
draw_arrow(ax, 10.7, 3.3, 10.7, 1.5 + 0.7, color=C['arrow'])

# No critic needed label
ax.text(13.2, 3.72, 'No Critic\nneeded', ha='center', va='center',
        fontsize=7.5, color='#2E7D32', fontweight='bold',
        style='italic', zorder=6,
        bbox=dict(boxstyle='round,pad=0.2', facecolor='#E8F5E9',
                  edgecolor='#A5D6A7', alpha=0.8))

# ── Dividing line ─────────────────────────────────────────────────────
ax.plot([7.1, 7.1], [0.2, 9.4], color='#B0BEC5', lw=2, ls='--', zorder=2)
ax.text(7.1, 9.55, '↔ Weight\nTransfer', ha='center', va='center',
        fontsize=7, color='#78909C', fontweight='bold')

# ── Key annotations ───────────────────────────────────────────────────

# Input dimensionality annotation
ax.text(3.5, 4.5, 'Input: s + (a₁, a₂, a₃) = 12 dims',
        ha='center', fontsize=7, color=C['critic'], style='italic')

ax.text(10.7, 4.5, 'Input: oᵢ only = 6 dims each',
        ha='center', fontsize=7, color='#2E7D32', style='italic')

# ── Title ──────────────────────────────────────────────────────────────
fig.suptitle('MADDPG: Centralised Training with Decentralised Execution (CTDE)',
             fontsize=13, fontweight='bold', color=C['text'], y=0.98)

# ── Save ───────────────────────────────────────────────────────────────
output_path = '/Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/simulation_tests/reports/charts/figL_maddpg_ctde_architecture.png'
plt.savefig(output_path)
plt.close()
print(f"Saved: {output_path}")
