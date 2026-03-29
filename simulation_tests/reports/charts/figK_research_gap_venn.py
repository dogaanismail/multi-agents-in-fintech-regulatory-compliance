"""
Figure K: Research Gap Venn Diagram
Shows the three independent research threads and how this dissertation
uniquely occupies the intersection of all three.
For: Chapter 2, Section 2.3.5 (Research Gap)
"""

import matplotlib.pyplot as plt
import matplotlib.patches as patches
import numpy as np
from matplotlib.patches import FancyBboxPatch

# ── Style ──────────────────────────────────────────────────────────────
plt.rcParams.update({
    'font.family': 'serif',
    'font.size': 11,
    'axes.titlesize': 14,
    'figure.dpi': 300,
    'savefig.dpi': 300,
    'savefig.bbox': 'tight',
    'savefig.pad_inches': 0.3,
})

fig, ax = plt.subplots(figsize=(10, 8))
ax.set_xlim(-5, 5)
ax.set_ylim(-4.5, 5)
ax.set_aspect('equal')
ax.axis('off')

# ── Three circles ──────────────────────────────────────────────────────
# Colors: academic palette with transparency
colors = {
    'supervised': '#2196F3',   # Blue
    'graph':      '#4CAF50',   # Green
    'marl':       '#FF9800',   # Orange
}

# Circle positions (equilateral triangle arrangement)
r = 2.4  # radius
cx = [0, -1.8, 1.8]
cy = [1.8, -1.0, -1.0]

# Draw circles
for i, (x, y, key) in enumerate(zip(cx, cy, colors.keys())):
    circle = plt.Circle((x, y), r, 
                         facecolor=colors[key], 
                         edgecolor='white',
                         linewidth=2.5,
                         alpha=0.22)
    ax.add_patch(circle)
    # Bold border
    circle_border = plt.Circle((x, y), r,
                               facecolor='none',
                               edgecolor=colors[key],
                               linewidth=2.0,
                               alpha=0.7)
    ax.add_patch(circle_border)

# ── Labels for each circle (outside the overlaps) ─────────────────────

# Top circle: Supervised Learning
ax.text(0, 3.8, 'Supervised Learning\nfor Fraud Detection', 
        ha='center', va='center', fontsize=11, fontweight='bold',
        color=colors['supervised'])
ax.text(0, 3.05, 'XGBoost · CatBoost · SMOTE\nSHAP · Precision-Recall',
        ha='center', va='center', fontsize=8.5, color='#555555',
        style='italic')

# Bottom-left: Graph-Based Analysis
ax.text(-3.5, -2.8, 'Graph-Based\nNetwork Analysis', 
        ha='center', va='center', fontsize=11, fontweight='bold',
        color=colors['graph'])
ax.text(-3.5, -3.6, 'PageRank · Centrality\nLouvain · Neo4j GDS',
        ha='center', va='center', fontsize=8.5, color='#555555',
        style='italic')

# Bottom-right: MARL
ax.text(3.5, -2.8, 'Multi-Agent\nReinforcement Learning', 
        ha='center', va='center', fontsize=11, fontweight='bold',
        color=colors['marl'])
ax.text(3.5, -3.6, 'MADDPG · CTDE\nActor-Critic · Replay Buffer',
        ha='center', va='center', fontsize=8.5, color='#555555',
        style='italic')

# ── Pairwise overlap labels ───────────────────────────────────────────

# Supervised + Graph (top-left overlap)
ax.text(-1.15, 1.4, 'Feature\nEngineering',
        ha='center', va='center', fontsize=8, color='#333333',
        fontweight='medium')

# Supervised + MARL (top-right overlap)
ax.text(1.15, 1.4, 'Adaptive\nDetection',
        ha='center', va='center', fontsize=8, color='#333333',
        fontweight='medium')

# Graph + MARL (bottom overlap)
ax.text(0, -1.6, 'Network-Aware\nAgents',
        ha='center', va='center', fontsize=8, color='#333333',
        fontweight='medium')

# ── Center: THIS DISSERTATION ─────────────────────────────────────────

# Highlight box in the center
center_box = FancyBboxPatch((-1.35, -0.05), 2.7, 1.05,
                            boxstyle="round,pad=0.15",
                            facecolor='#37474F', edgecolor='white',
                            linewidth=2, alpha=0.88, zorder=10)
ax.add_patch(center_box)

ax.text(0, 0.55, 'Proposed System',
        ha='center', va='center', fontsize=10, fontweight='bold',
        color='white', zorder=11)

ax.text(0, 0.15, 'MADDPG + XGBoost + Neo4j GDS',
        ha='center', va='center', fontsize=7.5,
        color='#CFD8DC', zorder=11, style='italic')

# ── References in each zone ───────────────────────────────────────────

# Add key reference citations subtly
ref_style = dict(fontsize=6.5, color='#888888', ha='center', va='center')

ax.text(0, 2.4, 'Chen & Guestrin (2016)\nProkhorenkova et al. (2018)',
        **ref_style)

ax.text(-2.4, -0.3, 'Colladon &\nRemondi (2017)\nAkoglu et al. (2015)',
        **ref_style)

ax.text(2.4, -0.3, 'Lowe et al. (2017)\nAmato (2024)\nWan et al. (2022)',
        **ref_style)

# ── Title ──────────────────────────────────────────────────────────────
ax.set_title('Research Gap: Convergence of Three Independent Research Threads',
             fontsize=13, fontweight='bold', pad=15, color='#222222')

# ── Save ───────────────────────────────────────────────────────────────
output_path = '/Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/simulation_tests/reports/charts/figK_research_gap_venn.png'
plt.savefig(output_path)
plt.close()
print(f"Saved: {output_path}")
