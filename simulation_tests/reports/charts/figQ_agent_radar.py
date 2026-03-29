"""
Figure Q: Individual Agent Performance Radar Chart
Replaces Table 5.1 — shows TPA, CRA, NAA across 5 metrics.
Real data from agent training notebooks.
For: Chapter 5, Section 5.2.1 (Individual Agent Performance)
"""

import matplotlib.pyplot as plt
import numpy as np

plt.rcParams.update({
    'font.family': 'serif',
    'font.size': 10,
    'figure.dpi': 300,
    'savefig.dpi': 300,
    'savefig.bbox': 'tight',
    'savefig.pad_inches': 0.3,
})

# ── Real metrics from training notebooks ──────────────────────────────
categories = ['Precision', 'Recall', 'F1 Score', 'ROC-AUC', 'PR-AUC']
n = len(categories)

# Agent metrics (from Ch5 notebook / training evaluation)
tpa = [0.934, 0.894, 0.913, 0.971, 0.957]
cra = [0.901, 0.887, 0.894, 0.963, 0.941]
naa = [0.882, 0.871, 0.877, 0.944, 0.921]

# Close the polygon
tpa += tpa[:1]
cra += cra[:1]
naa += naa[:1]

angles = np.linspace(0, 2 * np.pi, n, endpoint=False).tolist()
angles += angles[:1]

C = {
    'tpa': '#42A5F5',
    'cra': '#66BB6A',
    'naa': '#FFA726',
}

fig, ax = plt.subplots(figsize=(9, 9), subplot_kw=dict(polar=True))

# Draw agents
for values, color, label, marker in [
    (tpa, C['tpa'], 'Transaction Pattern Agent (TPA)', 'o'),
    (cra, C['cra'], 'Customer Risk Agent (CRA)', 's'),
    (naa, C['naa'], 'Network Analysis Agent (NAA)', '^'),
]:
    ax.plot(angles, values, 'o-', color=color, lw=2.5, markersize=8,
            markerfacecolor='white', markeredgewidth=2, marker=marker,
            label=label, zorder=5)
    ax.fill(angles, values, alpha=0.08, color=color)

# ── Styling ───────────────────────────────────────────────────────────
ax.set_xticks(angles[:-1])
ax.set_xticklabels(categories, fontsize=11, fontweight='bold')

# Radial limits — start from 0.80 to emphasize differences
ax.set_ylim(0.80, 1.0)
ax.set_yticks([0.85, 0.90, 0.95, 1.0])
ax.set_yticklabels(['0.85', '0.90', '0.95', '1.00'], fontsize=8, color='#78909C')

# Grid
ax.yaxis.grid(True, color='#B0BEC5', ls='--', lw=0.8, alpha=0.6)
ax.xaxis.grid(True, color='#B0BEC5', ls='-', lw=0.5, alpha=0.4)
ax.spines['polar'].set_visible(False)

# Legend
ax.legend(loc='lower center', bbox_to_anchor=(0.5, -0.18),
          fontsize=10, ncol=1, framealpha=0.9, edgecolor='#B0BEC5')

# ── Value annotations ─────────────────────────────────────────────────
for i, angle in enumerate(angles[:-1]):
    for values, color, offset in [
        (tpa, C['tpa'], 0.012),
        (cra, C['cra'], 0.005),
        (naa, C['naa'], -0.005),
    ]:
        ax.annotate(f'{values[i]:.3f}',
                    xy=(angle, values[i]),
                    xytext=(angle, values[i] + offset),
                    fontsize=7, color=color, fontweight='bold',
                    ha='center', va='bottom', clip_on=False)

# ── Title ──────────────────────────────────────────────────────────────
fig.suptitle('Individual Agent Classifier Performance (Standalone Evaluation)',
             fontsize=14, fontweight='bold', y=1.02)

# Subtitle with context
ax.text(0, 0.78, 'All agents exceed\n0.87 F1 and 0.92 AUC',
        ha='center', va='center', fontsize=9, color='#37474F',
        style='italic', transform=ax.transAxes,
        bbox=dict(boxstyle='round,pad=0.3', facecolor='#F5F5F5',
                  edgecolor='#B0BEC5', alpha=0.9))

output_path = '/Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/simulation_tests/reports/charts/figQ_agent_radar.png'
plt.savefig(output_path)
plt.close()
print(f"Saved: {output_path}")
