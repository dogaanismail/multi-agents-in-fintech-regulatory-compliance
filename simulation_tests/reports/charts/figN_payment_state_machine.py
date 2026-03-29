"""
Figure N: Payment Aggregate State Machine — Event-Sourced Lifecycle
Shows the Axon event-sourced aggregate states and transitions,
with real event counts from the 10K simulation (30,566 total events).
For: Chapter 3, Section 3.4.9 or 3.4.10 (Novel Engineering Decisions)
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

fig, ax = plt.subplots(figsize=(18, 11))
ax.set_xlim(-1.0, 17.5)
ax.set_ylim(-2.5, 11.5)
ax.set_aspect('equal')
ax.axis('off')

# ── Colors ─────────────────────────────────────────────────────────────
C = {
    'init':     '#78909C',  # Grey
    'pending':  '#42A5F5',  # Blue
    'approved': '#66BB6A',  # Green
    'blocked':  '#EF5350',  # Red
    'review':   '#FFA726',  # Orange
    'complete': '#26A69A',  # Teal
    'failed':   '#EC407A',  # Pink
    'charge':   '#7E57C2',  # Purple
    'arrow':    '#455A64',
    'text':     '#263238',
}

def draw_state(ax, x, y, w, h, label, sublabel, color, count=None):
    box = FancyBboxPatch((x, y), w, h,
                         boxstyle="round,pad=0.15",
                         facecolor=color, edgecolor='white',
                         linewidth=2, alpha=0.92, zorder=5)
    ax.add_patch(box)
    if sublabel:
        ax.text(x + w/2, y + h/2 + 0.15, label,
                ha='center', va='center', fontsize=8.5,
                fontweight='bold', color='white', zorder=6,
                clip_on=False)
        ax.text(x + w/2, y + h/2 - 0.2, sublabel,
                ha='center', va='center', fontsize=6.5,
                color='white', alpha=0.9, zorder=6,
                clip_on=False)
    else:
        ax.text(x + w/2, y + h/2, label,
                ha='center', va='center', fontsize=8.5,
                fontweight='bold', color='white', zorder=6,
                clip_on=False)
    if count is not None:
        ax.text(x + w - 0.05, y + h + 0.18, f'n={count:,}',
                ha='right', va='bottom', fontsize=6.5,
                color=color, fontweight='bold', zorder=7,
                clip_on=False,
                bbox=dict(boxstyle='round,pad=0.1', facecolor='white',
                          edgecolor=color, alpha=0.85, linewidth=0.8))

def draw_arrow(ax, x1, y1, x2, y2, label=None, color='#455A64', lw=1.5,
               ls='-', label_offset=(0, 0.25), fontsize=7):
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle='->', color=color,
                                lw=lw, linestyle=ls,
                                connectionstyle='arc3,rad=0'),
                zorder=4)
    if label:
        mx = (x1 + x2) / 2 + label_offset[0]
        my = (y1 + y2) / 2 + label_offset[1]
        ax.text(mx, my, label, ha='center', va='center',
                fontsize=fontsize, color=color, style='italic',
                clip_on=False,
                bbox=dict(boxstyle='round,pad=0.1', facecolor='white',
                          edgecolor='none', alpha=0.9))

# ── State boxes ───────────────────────────────────────────────────────

bw, bh = 3.4, 0.95  # wider boxes for readable text

# Row 1: INITIATED (centered)
cx = 7.0
draw_state(ax, cx, 9.5, bw, bh, 'INITIATED', 'PaymentInitiatedEvent', C['init'], 10082)

# Row 2: FRAUD_CHECK_PENDING (centered)
draw_state(ax, cx, 7.5, bw, bh, 'FRAUD_CHECK_PENDING', 'RiskAssessmentInitiatedEvent', C['pending'], 10082)

# Row 3: Three outcomes (fan-out) — spread wider
draw_state(ax, 0.0, 5.0, bw, bh, 'FRAUD_CHECK_APPROVED', 'FraudCheckApprovedEvent', C['approved'], 23)
draw_state(ax, 5.8, 5.0, bw, bh, 'MANUAL_REVIEW_REQUIRED', 'ManualReviewRequestedEvent', C['review'], 193)
draw_state(ax, 12.0, 5.0, bw, bh, 'BLOCKED', 'PaymentBlockedEvent', C['blocked'], 4)

# Row 4: Manual review outcomes
draw_state(ax, 4.0, 3.0, bw, bh, 'MANUAL_REVIEW_APPROVED', 'ManualReviewApprovedEvent', C['approved'], 7)
draw_state(ax, 9.0, 3.0, bw, bh, 'MANUAL_REVIEW_REJECTED', 'ManualReviewRejectedEvent', C['blocked'], 4)

# Row 5: Account charge
draw_state(ax, 0.0, 1.3, bw, bh, 'ACCOUNT_CHARGE_PENDING', 'AccountChargeInitiatedEvent', C['charge'], 30)

# Row 6: Terminal states (wider boxes)
tw = 2.4
draw_state(ax, 0.0, -0.3, tw, 0.8, 'COMPLETED', 'PaymentCompleted', C['complete'], 34)
draw_state(ax, 3.0, -0.3, tw, 0.8, 'FAILED', 'AccountChargeFailed', C['failed'], 4)

# ── Arrows (transitions) ─────────────────────────────────────────────

mid = cx + bw / 2  # center x of top boxes

# INITIATED → FRAUD_CHECK_PENDING
draw_arrow(ax, mid, 9.5, mid, 7.5 + bh,
           'RiskAssessmentInitiated', C['arrow'])

# FRAUD_CHECK_PENDING → three outcomes
draw_arrow(ax, cx, 7.5 + bh/2, 0.0 + bw, 5.0 + bh/2,
           'RiskAssessmentCompleted\n(action = PROCEED)', C['approved'],
           label_offset=(0, 0.35))

draw_arrow(ax, mid, 7.5, mid - 0.3, 5.0 + bh,
           'RiskAssessmentCompleted\n(action = ESCALATE)', C['review'])

draw_arrow(ax, cx + bw, 7.5 + bh/2, 12.0, 5.0 + bh/2,
           'RiskAssessmentCompleted\n(action = BLOCK)', C['blocked'],
           label_offset=(0, 0.35))

# MANUAL_REVIEW → approve/reject
draw_arrow(ax, 6.5, 5.0, 5.7, 3.0 + bh,
           'Officer approves', C['approved'], label_offset=(-0.4, 0.15))

draw_arrow(ax, 8.5, 5.0, 9.8, 3.0 + bh,
           'Officer rejects', C['blocked'], label_offset=(0.4, 0.15))

# FRAUD_CHECK_APPROVED → ACCOUNT_CHARGE_PENDING
draw_arrow(ax, 1.7, 5.0, 1.7, 1.3 + bh,
           'AccountChargeInitiated', C['charge'])

# MANUAL_REVIEW_APPROVED → ACCOUNT_CHARGE_PENDING
draw_arrow(ax, 4.0, 3.0 + bh/2, 0.0 + bw, 1.3 + bh/2,
           'AccountChargeInitiated', C['charge'], label_offset=(0, 0.3))

# ACCOUNT_CHARGE_PENDING → COMPLETED / FAILED
draw_arrow(ax, 1.0, 1.3, 1.0, 0.5,
           'Charged', C['complete'], label_offset=(-0.6, 0.0))

draw_arrow(ax, 2.8, 1.3, 3.8, 0.5,
           'Charge failed', C['failed'], label_offset=(0.6, 0.0))

# ── Saga timeout annotation ──────────────────────────────────────────
timeout_box = FancyBboxPatch((12.5, 7.5), 4.2, 1.5,
                             boxstyle="round,pad=0.15",
                             facecolor='#FFF8E1', edgecolor='#FFB74D',
                             linewidth=1.2, alpha=0.9, zorder=3)
ax.add_patch(timeout_box)
ax.text(14.6, 8.65, 'Saga Deadline Handlers', ha='center',
        fontsize=8.5, fontweight='bold', color='#E65100', zorder=4, clip_on=False)
ax.text(14.6, 8.2, 'PaymentRiskSaga: 1 min timeout', ha='center',
        fontsize=7.5, color='#E65100', zorder=4, clip_on=False)
ax.text(14.6, 7.85, 'AccountChargeSaga: 2 min timeout', ha='center',
        fontsize=7.5, color='#E65100', zorder=4, clip_on=False)

# Dashed arrows from saga box to states
draw_arrow(ax, 12.5, 8.25, cx + bw, 7.95, None, '#FFB74D', lw=1.2, ls='--')
draw_arrow(ax, 12.5, 7.85, 3.4, 1.8, None, '#FFB74D', lw=1.0, ls='--')

# ── Feedback loop annotation ─────────────────────────────────────────
feedback_box = FancyBboxPatch((13.0, 1.5), 4.3, 2.0,
                              boxstyle="round,pad=0.15",
                              facecolor='#E8F5E9', edgecolor='#66BB6A',
                              linewidth=1.2, alpha=0.9, zorder=3)
ax.add_patch(feedback_box)
ax.text(15.15, 3.15, 'Human-in-the-Loop Feedback', ha='center',
        fontsize=8.5, fontweight='bold', color='#2E7D32', zorder=4, clip_on=False)
ax.text(15.15, 2.7, 'Officer decisions published via', ha='center',
        fontsize=7.5, color='#2E7D32', zorder=4, clip_on=False)
ax.text(15.15, 2.3, 'agent.manual.feedback Kafka topic', ha='center',
        fontsize=7.5, color='#2E7D32', zorder=4, style='italic', clip_on=False)
ax.text(15.15, 1.9, 'Reward: 2x (review) / 3x (override)', ha='center',
        fontsize=7.5, color='#2E7D32', zorder=4, clip_on=False)

# Arrow from feedback to manual review states
draw_arrow(ax, 13.0, 3.0, 9.0 + bw, 3.4, None, '#66BB6A', lw=1.2, ls='--')

# ── Immutable event store annotation ─────────────────────────────────
ax.text(8.0, -0.8, 'All transitions persisted as immutable Axon domain events',
        ha='center', fontsize=8.5, color='#546E7A', style='italic', clip_on=False)
ax.text(8.0, -1.25, 'Total: 30,566 events across 11 types  (10,082 payments)',
        ha='center', fontsize=8.5, fontweight='bold', color='#37474F', clip_on=False)

# ── Legend ─────────────────────────────────────────────────────────────
legend_items = [
    mpatches.Patch(facecolor=C['init'], edgecolor='white', label='Initial state'),
    mpatches.Patch(facecolor=C['pending'], edgecolor='white', label='Pending / In-progress'),
    mpatches.Patch(facecolor=C['approved'], edgecolor='white', label='Approved'),
    mpatches.Patch(facecolor=C['review'], edgecolor='white', label='Manual review required'),
    mpatches.Patch(facecolor=C['blocked'], edgecolor='white', label='Blocked / Rejected'),
    mpatches.Patch(facecolor=C['charge'], edgecolor='white', label='Account charge'),
    mpatches.Patch(facecolor=C['complete'], edgecolor='white', label='Completed'),
    mpatches.Patch(facecolor=C['failed'], edgecolor='white', label='Failed'),
]
ax.legend(handles=legend_items, loc='lower left', fontsize=7,
          framealpha=0.9, edgecolor='#B0BEC5', ncol=4,
          bbox_to_anchor=(0.02, -0.14))

# ── Title ──────────────────────────────────────────────────────────────
fig.suptitle('Payment Aggregate State Machine: Event-Sourced Lifecycle  (n = 10,082)',
             fontsize=14, fontweight='bold', color=C['text'], y=0.99)

# ── Save ───────────────────────────────────────────────────────────────
output_path = '/Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/simulation_tests/reports/charts/figN_payment_state_machine.png'
plt.savefig(output_path)
plt.close()
print(f"Saved: {output_path}")
