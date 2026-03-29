"""
Figure M: AML Transaction Graph — Laundering Typologies
Shows a small example directed graph illustrating three AML patterns:
  - Smurfing (many small transfers)
  - Layering (multi-hop cross-border chain)
  - Round-trip (A→B→A cycle)
With graph metrics: degree, PageRank, community detection highlighted.
For: Chapter 2, Section 2.4.4 (Graph Theory for Financial Network Analysis)
"""

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import numpy as np

plt.rcParams.update({
    'font.family': 'serif',
    'font.size': 10,
    'figure.dpi': 300,
    'savefig.dpi': 300,
    'savefig.bbox': 'tight',
    'savefig.pad_inches': 0.3,
})

fig, ax = plt.subplots(figsize=(14, 9))
ax.set_xlim(-0.5, 14.5)
ax.set_ylim(-0.5, 9.5)
ax.set_aspect('equal')
ax.axis('off')

# ── Colors ─────────────────────────────────────────────────────────────
C = {
    'legit':      '#78909C',  # Grey — legitimate
    'mule':       '#E53935',  # Red — mule/suspicious
    'smurf':      '#2196F3',  # Blue
    'layer':      '#4CAF50',  # Green
    'roundtrip':  '#FF9800',  # Orange
    'community1': '#E3F2FD',  # Light blue bg
    'community2': '#FFF3E0',  # Light orange bg
    'community3': '#E8F5E9',  # Light green bg
    'text':       '#263238',
}

def draw_node(ax, x, y, label, sublabel=None, color='#78909C', size=0.42, fontsize=9):
    circle = plt.Circle((x, y), size, facecolor=color, edgecolor='white',
                         linewidth=2, alpha=0.9, zorder=10)
    ax.add_patch(circle)
    ax.text(x, y + (0.08 if sublabel else 0), label,
            ha='center', va='center', fontsize=fontsize,
            fontweight='bold', color='white', zorder=11)
    if sublabel:
        ax.text(x, y - 0.15, sublabel, ha='center', va='center',
                fontsize=6, color='white', alpha=0.85, zorder=11)

def draw_edge(ax, x1, y1, x2, y2, label=None, color='#455A64', lw=1.5, ls='-'):
    # Shorten arrows to not overlap with circles
    dx, dy = x2 - x1, y2 - y1
    dist = np.sqrt(dx**2 + dy**2)
    offset = 0.48
    sx = x1 + offset * dx / dist
    sy = y1 + offset * dy / dist
    ex = x2 - offset * dx / dist
    ey = y2 - offset * dy / dist
    ax.annotate('', xy=(ex, ey), xytext=(sx, sy),
                arrowprops=dict(arrowstyle='->', color=color,
                                lw=lw, linestyle=ls),
                zorder=5)
    if label:
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        ax.text(mx, my + 0.25, label, ha='center', va='center',
                fontsize=7, color=color, style='italic',
                bbox=dict(boxstyle='round,pad=0.1', facecolor='white',
                          edgecolor='none', alpha=0.8))

# ── Community backgrounds ─────────────────────────────────────────────

# Community 1: Smurfing cluster (left)
c1 = FancyBboxPatch((0.0, 4.2), 4.8, 4.8,
                     boxstyle="round,pad=0.3",
                     facecolor=C['community1'], edgecolor='#90CAF9',
                     linewidth=1.2, alpha=0.4, zorder=1)
ax.add_patch(c1)
ax.text(2.4, 8.7, 'Community A — Smurfing Pattern',
        ha='center', fontsize=9, fontweight='bold', color='#1565C0')

# Community 2: Layering cluster (right)
c2 = FancyBboxPatch((9.5, 4.2), 4.8, 4.8,
                     boxstyle="round,pad=0.3",
                     facecolor=C['community3'], edgecolor='#A5D6A7',
                     linewidth=1.2, alpha=0.4, zorder=1)
ax.add_patch(c2)
ax.text(11.9, 8.7, 'Community B — Layering Pattern',
        ha='center', fontsize=9, fontweight='bold', color='#2E7D32')

# Community 3: Round-trip (bottom center)
c3 = FancyBboxPatch((4.5, 0.0), 5.5, 3.5,
                     boxstyle="round,pad=0.3",
                     facecolor=C['community2'], edgecolor='#FFB74D',
                     linewidth=1.2, alpha=0.4, zorder=1)
ax.add_patch(c3)
ax.text(7.25, 3.2, 'Community C — Round-Trip Pattern',
        ha='center', fontsize=9, fontweight='bold', color='#E65100')

# ══════════════════════════════════════════════════════════════════════
# SMURFING PATTERN (left cluster)
# Many small transfers from multiple sources → one collector
# ══════════════════════════════════════════════════════════════════════

# Source accounts (smurfs)
draw_node(ax, 0.8, 7.5, 'S1', None, C['legit'], 0.35)
draw_node(ax, 0.8, 6.3, 'S2', None, C['legit'], 0.35)
draw_node(ax, 0.8, 5.1, 'S3', None, C['legit'], 0.35)

# Collector (mule)
draw_node(ax, 3.2, 6.3, 'M1', 'Mule', C['mule'], 0.5)

# Smurfing edges (many small amounts)
draw_edge(ax, 0.8, 7.5, 3.2, 6.3, '$2.8k', C['smurf'])
draw_edge(ax, 0.8, 6.3, 3.2, 6.3, '$4.5k', C['smurf'])
draw_edge(ax, 0.8, 5.1, 3.2, 6.3, '$3.2k', C['smurf'])

# Annotation
ax.text(1.8, 4.7, 'Each < $10k\nthreshold', ha='center',
        fontsize=7, color='#1565C0', style='italic')

# ══════════════════════════════════════════════════════════════════════
# LAYERING PATTERN (right cluster)
# Multi-hop: GB → TR → AE with currency changes
# ══════════════════════════════════════════════════════════════════════

draw_node(ax, 10.3, 7.5, 'L1', 'GB', C['legit'], 0.42)
draw_node(ax, 12.3, 6.3, 'L2', 'TR', C['mule'], 0.42)
draw_node(ax, 13.5, 4.8, 'L3', 'AE', C['mule'], 0.42)

# Layering edges (cross-border with currency change)
draw_edge(ax, 10.3, 7.5, 12.3, 6.3, 'GBP $50k', C['layer'])
draw_edge(ax, 12.3, 6.3, 13.5, 4.8, 'TRY $48k', C['layer'])

# Annotation
ax.text(10.5, 5.0, 'Cross-border\ncurrency change', ha='center',
        fontsize=7, color='#2E7D32', style='italic')

# ══════════════════════════════════════════════════════════════════════
# ROUND-TRIP PATTERN (bottom center)
# A → B → A (boomerang)
# ══════════════════════════════════════════════════════════════════════

draw_node(ax, 5.5, 1.5, 'R1', None, C['roundtrip'], 0.42)
draw_node(ax, 8.5, 1.5, 'R2', None, C['roundtrip'], 0.42)

# Bidirectional arrows (offset vertically)
draw_edge(ax, 5.5, 1.8, 8.5, 1.8, '$25k', C['roundtrip'])
draw_edge(ax, 8.5, 1.2, 5.5, 1.2, '$24.5k', C['roundtrip'])

# Annotation
ax.text(7.0, 0.5, 'Boomerang: A to B to A\n(near-equal amounts)', ha='center',
        fontsize=7, color='#E65100', style='italic')

# ══════════════════════════════════════════════════════════════════════
# CONNECTING EDGES (between communities — the mule bridges)
# ══════════════════════════════════════════════════════════════════════

# M1 → Layering entry (mule bridges communities)
draw_edge(ax, 3.2, 6.3, 10.3, 7.5, '$10.5k', '#455A64', lw=1.2, ls='--')

# M1 → Round-trip entry
draw_edge(ax, 3.2, 6.3, 5.5, 1.5, '$8k', '#455A64', lw=1.2, ls='--')

# ══════════════════════════════════════════════════════════════════════
# GRAPH METRICS ANNOTATION BOX
# ══════════════════════════════════════════════════════════════════════

metrics_box = FancyBboxPatch((5.0, 5.5), 4.5, 3.2,
                             boxstyle="round,pad=0.15",
                             facecolor='white', edgecolor='#B0BEC5',
                             linewidth=1.2, alpha=0.92, zorder=8)
ax.add_patch(metrics_box)

ax.text(7.25, 8.3, 'Graph Metrics (Node M1)', ha='center',
        fontsize=9, fontweight='bold', color=C['text'], zorder=9)

metrics = [
    ('In-degree:',        '3  (receives from S1, S2, S3)'),
    ('Out-degree:',       '2  (sends to L1, R1)'),
    ('Betweenness:',      'High  (bridges 3 communities)'),
    ('PageRank:',         'Elevated  (many incoming links)'),
    ('Clustering coeff:', 'Low  (neighbours not connected)'),
    ('Community:',        'A  (but bridges to B and C)'),
]

for i, (key, val) in enumerate(metrics):
    y_pos = 7.9 - i * 0.38
    ax.text(5.4, y_pos, key, ha='left', fontsize=7.5,
            fontweight='bold', color=C['text'], zorder=9)
    ax.text(7.1, y_pos, val, ha='left', fontsize=7.5,
            color='#546E7A', zorder=9)

# Arrow from metrics box to M1
ax.annotate('', xy=(3.6, 6.3), xytext=(5.0, 6.3),
            arrowprops=dict(arrowstyle='->', color=C['mule'],
                            lw=1.5, linestyle='--'),
            zorder=7)

# ── Legend ─────────────────────────────────────────────────────────────
legend_items = [
    mpatches.Patch(facecolor=C['legit'], edgecolor='white', label='Legitimate account'),
    mpatches.Patch(facecolor=C['mule'], edgecolor='white', label='Suspicious / Mule account'),
    mpatches.Patch(facecolor=C['smurf'], edgecolor='white', label='Smurfing transaction'),
    mpatches.Patch(facecolor=C['layer'], edgecolor='white', label='Layering transaction'),
    mpatches.Patch(facecolor=C['roundtrip'], edgecolor='white', label='Round-trip transaction'),
]
ax.legend(handles=legend_items, loc='lower left', fontsize=7.5,
          framealpha=0.9, edgecolor='#B0BEC5', ncol=2,
          bbox_to_anchor=(0.0, -0.02))

# ── Title ──────────────────────────────────────────────────────────────
fig.suptitle('AML Transaction Graph: Laundering Typologies and Graph-Derived Features',
             fontsize=13, fontweight='bold', color=C['text'], y=0.98)

# ── Save ───────────────────────────────────────────────────────────────
output_path = '/Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/simulation_tests/reports/charts/figM_aml_transaction_graph.png'
plt.savefig(output_path)
plt.close()
print(f"Saved: {output_path}")
