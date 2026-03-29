"""
Figure S: Payment Lifecycle Sankey — Full Decision Flow
Shows: 10K payments → Fraud/Legit → REVIEW/ALLOW → TP/FP/TN/FN
Real data from results.csv + summary.json
For: Chapter 5, Section 5.3.6 (Payment Lifecycle Flow)
"""

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch
import numpy as np

plt.rcParams.update({
    'font.family': 'serif',
    'font.size': 10,
    'figure.dpi': 300,
    'savefig.dpi': 300,
    'savefig.bbox': 'tight',
    'savefig.pad_inches': 0.4,
})

# ── Real data ─────────────────────────────────────────────────────────
# Total: 10,000 payments
# Fraud: 1,000 | Legit: 9,000
# Actions: REVIEW=8,855 | ALLOW=1,145 | BLOCK=0
# Outcomes: TP=973 | FP=7,882 | TN=1,118 | FN=27

# Flow breakdown (from results.csv cross-tabs):
# Fraud (1000) → REVIEW: 973 (=TP) | ALLOW: 27 (=FN)
# Legit (9000) → REVIEW: 7882 (=FP) | ALLOW: 1118 (=TN)

fig, ax = plt.subplots(figsize=(18, 10))
ax.set_xlim(-0.5, 17)
ax.set_ylim(-1, 11)
ax.axis('off')

# ── Colors ─────────────────────────────────────────────────────────────
C = {
    'total':   '#546E7A',  # Dark grey
    'fraud':   '#EF5350',  # Red
    'legit':   '#42A5F5',  # Blue
    'review':  '#FFA726',  # Orange
    'allow':   '#66BB6A',  # Green
    'tp':      '#2E7D32',  # Dark green
    'fp':      '#FF8F00',  # Amber
    'tn':      '#1565C0',  # Dark blue
    'fn':      '#C62828',  # Dark red
    'flow_f':  '#EF9A9A',  # Light red (fraud flows)
    'flow_l':  '#90CAF9',  # Light blue (legit flows)
}

def draw_node(ax, x, y, w, h, label, sublabel, color, fontsize=10):
    box = FancyBboxPatch((x, y), w, h,
                         boxstyle="round,pad=0.12",
                         facecolor=color, edgecolor='white',
                         linewidth=2.5, alpha=0.92, zorder=5)
    ax.add_patch(box)
    ax.text(x + w/2, y + h/2 + 0.15, label,
            ha='center', va='center', fontsize=fontsize,
            fontweight='bold', color='white', zorder=6, clip_on=False)
    ax.text(x + w/2, y + h/2 - 0.25, sublabel,
            ha='center', va='center', fontsize=fontsize - 2,
            color='white', alpha=0.9, zorder=6, clip_on=False)

def draw_flow(ax, x1, y1, x2, y2, width, color, alpha=0.3):
    """Draw a curved flow band between two nodes."""
    from matplotlib.path import Path
    import matplotlib.patches as mpatches_path

    mid_x = (x1 + x2) / 2
    half_w = width / 2

    # Top path
    verts_top = [
        (x1, y1 + half_w),
        (mid_x, y1 + half_w),
        (mid_x, y2 + half_w),
        (x2, y2 + half_w),
    ]

    # Bottom path (reverse)
    verts_bot = [
        (x2, y2 - half_w),
        (mid_x, y2 - half_w),
        (mid_x, y1 - half_w),
        (x1, y1 - half_w),
    ]

    verts = verts_top + verts_bot + [verts_top[0]]
    codes = [Path.MOVETO] + [Path.CURVE4] * 3 + [Path.LINETO] + [Path.CURVE4] * 3 + [Path.CLOSEPOLY]

    path = Path(verts, codes)
    patch = mpatches_path.PathPatch(path, facecolor=color, alpha=alpha,
                                     edgecolor=color, linewidth=0.5, zorder=3)
    ax.add_patch(patch)

# Scale: max height for flows proportional to count
# Total = 10000, scale factor
scale = 6.0 / 10000  # 6 units for 10K

# ═══════════════════════════════════════════════════════════════════════
# COLUMN 1: Total Payments
# ═══════════════════════════════════════════════════════════════════════
col1_x = 0.0
node_w = 2.8

total_h = 10000 * scale  # 6.0
total_y = 2.5
draw_node(ax, col1_x, total_y, node_w, total_h,
          '10,000 Payments', 'Simulation Input', C['total'], fontsize=11)

# ═══════════════════════════════════════════════════════════════════════
# COLUMN 2: Fraud vs Legitimate
# ═══════════════════════════════════════════════════════════════════════
col2_x = 4.5

fraud_h = 1000 * scale  # 0.6
legit_h = 9000 * scale  # 5.4

fraud_y = total_y + total_h - fraud_h  # top
legit_y = total_y  # bottom

draw_node(ax, col2_x, fraud_y, node_w, max(fraud_h, 1.0),
          'Fraud', 'n = 1,000 (10%)', C['fraud'])
draw_node(ax, col2_x, legit_y, node_w, max(legit_h, 3.0),
          'Legitimate', 'n = 9,000 (90%)', C['legit'])

# Flows: Total → Fraud/Legit
fraud_center = fraud_y + max(fraud_h, 1.0) / 2
legit_center = legit_y + max(legit_h, 3.0) / 2
total_center = total_y + total_h / 2

draw_flow(ax, col1_x + node_w, total_center + 1.5, col2_x, fraud_center,
          max(fraud_h, 0.5), C['flow_f'], 0.35)
draw_flow(ax, col1_x + node_w, total_center - 0.8, col2_x, legit_center,
          max(legit_h, 2.5), C['flow_l'], 0.25)

# ═══════════════════════════════════════════════════════════════════════
# COLUMN 3: MARL Decision
# ═══════════════════════════════════════════════════════════════════════
col3_x = 9.0

review_n = 8855
allow_n = 1145

review_y = 5.5
allow_y = 2.5

draw_node(ax, col3_x, review_y, node_w, 2.5,
          'REVIEW', f'n = {review_n:,} (88.6%)', C['review'], fontsize=11)
draw_node(ax, col3_x, allow_y, node_w, 1.5,
          'ALLOW', f'n = {allow_n:,} (11.4%)', C['allow'], fontsize=11)

# Flows: Fraud → REVIEW (973=TP), Fraud → ALLOW (27=FN)
draw_flow(ax, col2_x + node_w, fraud_center + 0.2, col3_x, review_y + 2.0,
          0.5, C['flow_f'], 0.4)

draw_flow(ax, col2_x + node_w, fraud_center - 0.3, col3_x, allow_y + 1.2,
          0.15, C['flow_f'], 0.5)

# Flows: Legit → REVIEW (7882=FP), Legit → ALLOW (1118=TN)
draw_flow(ax, col2_x + node_w, legit_center + 0.8, col3_x, review_y + 0.8,
          2.5, C['flow_l'], 0.2)

draw_flow(ax, col2_x + node_w, legit_center - 0.5, col3_x, allow_y + 0.5,
          0.6, C['flow_l'], 0.3)

# Flow labels
ax.text(7.2, review_y + 2.3, '973', fontsize=9, fontweight='bold',
        color=C['fraud'], ha='center', clip_on=False,
        bbox=dict(boxstyle='round,pad=0.1', facecolor='white', alpha=0.85))
ax.text(7.2, allow_y + 0.6, '27', fontsize=9, fontweight='bold',
        color=C['fraud'], ha='center', clip_on=False,
        bbox=dict(boxstyle='round,pad=0.1', facecolor='white', alpha=0.85))
ax.text(7.2, review_y + 0.5, '7,882', fontsize=9, fontweight='bold',
        color=C['legit'], ha='center', clip_on=False,
        bbox=dict(boxstyle='round,pad=0.1', facecolor='white', alpha=0.85))
ax.text(7.2, allow_y - 0.1, '1,118', fontsize=9, fontweight='bold',
        color=C['legit'], ha='center', clip_on=False,
        bbox=dict(boxstyle='round,pad=0.1', facecolor='white', alpha=0.85))

# ═══════════════════════════════════════════════════════════════════════
# COLUMN 4: Detection Outcomes
# ═══════════════════════════════════════════════════════════════════════
col4_x = 13.5

# From REVIEW
draw_node(ax, col4_x, 7.2, node_w, 1.0,
          'True Positive', 'n = 973', C['tp'])
draw_node(ax, col4_x, 5.5, node_w, 1.0,
          'False Positive', 'n = 7,882', C['fp'])

# From ALLOW
draw_node(ax, col4_x, 3.2, node_w, 1.0,
          'True Negative', 'n = 1,118', C['tn'])
draw_node(ax, col4_x, 1.5, node_w, 1.0,
          'False Negative', 'n = 27', C['fn'])

# Flows: REVIEW → TP/FP
draw_flow(ax, col3_x + node_w, review_y + 2.0, col4_x, 7.7,
          0.5, '#A5D6A7', 0.4)
draw_flow(ax, col3_x + node_w, review_y + 0.8, col4_x, 6.0,
          2.0, '#FFCC80', 0.25)

# Flows: ALLOW → TN/FN
draw_flow(ax, col3_x + node_w, allow_y + 1.0, col4_x, 3.7,
          0.6, '#90CAF9', 0.35)
draw_flow(ax, col3_x + node_w, allow_y + 0.3, col4_x, 2.0,
          0.12, '#EF9A9A', 0.5)

# ═══════════════════════════════════════════════════════════════════════
# COLUMN HEADERS
# ═══════════════════════════════════════════════════════════════════════
header_y = 9.8
headers = [
    (col1_x + node_w/2, 'Input'),
    (col2_x + node_w/2, 'Ground Truth'),
    (col3_x + node_w/2, 'MARL Decision'),
    (col4_x + node_w/2, 'Detection Outcome'),
]
for hx, label in headers:
    ax.text(hx, header_y, label, ha='center', fontsize=11,
            fontweight='bold', color='#37474F',
            bbox=dict(boxstyle='round,pad=0.2', facecolor='#ECEFF1',
                      edgecolor='#B0BEC5', alpha=0.9))

# ═══════════════════════════════════════════════════════════════════════
# KEY METRICS ANNOTATION
# ═══════════════════════════════════════════════════════════════════════
metrics_bg = FancyBboxPatch((13.5, -0.5), node_w, 1.5,
                            boxstyle="round,pad=0.12",
                            facecolor='#F3E5F5', edgecolor='#AB47BC',
                            linewidth=1.2, alpha=0.85, zorder=3)
ax.add_patch(metrics_bg)
ax.text(col4_x + node_w/2, 0.7, 'Key Metrics', ha='center',
        fontsize=9, fontweight='bold', color='#6A1B9A', zorder=4, clip_on=False)
ax.text(col4_x + node_w/2, 0.3, 'Recall: 97.3%  |  F1: 19.7%', ha='center',
        fontsize=8.5, color='#4A148C', zorder=4, clip_on=False)
ax.text(col4_x + node_w/2, -0.1, 'FNR: 2.7%  |  Precision: 11.0%', ha='center',
        fontsize=8.5, color='#4A148C', zorder=4, clip_on=False)

# Design rationale
rationale_bg = FancyBboxPatch((0.0, -0.5), 5.5, 1.5,
                              boxstyle="round,pad=0.12",
                              facecolor='#E8F5E9', edgecolor='#66BB6A',
                              linewidth=1.0, alpha=0.85, zorder=3)
ax.add_patch(rationale_bg)
ax.text(2.75, 0.7, 'Design Intent', ha='center',
        fontsize=9, fontweight='bold', color='#2E7D32', zorder=4, clip_on=False)
ax.text(2.75, 0.25, 'High recall (97.3%) prioritised over precision.\n'
        'FPs routed to human review, not auto-blocked.\n'
        'Only 27 fraudulent payments missed out of 1,000.',
        ha='center', fontsize=8, color='#2E7D32', zorder=4, clip_on=False)

# ── Title ──────────────────────────────────────────────────────────────
fig.suptitle('Payment Lifecycle Sankey: Decision Flow across 10,000 Transactions',
             fontsize=14, fontweight='bold', y=1.01)

output_path = '/Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/simulation_tests/reports/charts/figS_payment_sankey.png'
plt.savefig(output_path)
plt.close()
print(f"Saved: {output_path}")
