"""
Figure O: Three-Tier Reward Flow — Progressive Human-in-the-Loop Learning
Shows how reward signals flow from automated heuristics (Tier 1),
manual officer review (Tier 2), and decision overrides (Tier 3)
into the replay buffer for MADDPG training.
For: Chapter 3, Section 3.4.8 (or new Section 3.4.10)
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
    'savefig.pad_inches': 0.4,
})

fig, ax = plt.subplots(figsize=(18, 12))
ax.set_xlim(-0.5, 18)
ax.set_ylim(-0.5, 12.5)
ax.set_aspect('equal')
ax.axis('off')

# ── Colors ─────────────────────────────────────────────────────────────
C = {
    'tier1':     '#42A5F5',  # Blue — Automated
    'tier2':     '#FFA726',  # Orange — Manual Review
    'tier3':     '#EF5350',  # Red — Decision Override
    'kafka':     '#37474F',  # Dark slate — Kafka
    'buffer':    '#7E57C2',  # Purple — Replay buffer
    'training':  '#26A69A',  # Teal — Training
    'config':    '#78909C',  # Grey — Configuration
    'positive':  '#66BB6A',  # Green — Positive reward
    'negative':  '#EC407A',  # Pink — Negative reward
    'text':      '#263238',
    'bg_light':  '#FAFAFA',
}

def draw_box(ax, x, y, w, h, label, sublabel, color,
             fontsize_l=9, fontsize_s=7, text_color='white', alpha=0.92):
    box = FancyBboxPatch((x, y), w, h,
                         boxstyle="round,pad=0.15",
                         facecolor=color, edgecolor='white',
                         linewidth=2, alpha=alpha, zorder=5)
    ax.add_patch(box)
    if sublabel:
        ax.text(x + w/2, y + h/2 + 0.15, label,
                ha='center', va='center', fontsize=fontsize_l,
                fontweight='bold', color=text_color, zorder=6, clip_on=False)
        ax.text(x + w/2, y + h/2 - 0.2, sublabel,
                ha='center', va='center', fontsize=fontsize_s,
                color=text_color, alpha=0.9, zorder=6, clip_on=False)
    else:
        ax.text(x + w/2, y + h/2, label,
                ha='center', va='center', fontsize=fontsize_l,
                fontweight='bold', color=text_color, zorder=6, clip_on=False)

def draw_arrow(ax, x1, y1, x2, y2, label=None, color='#455A64', lw=1.8,
               ls='-', label_offset=(0, 0.2), fontsize=7, rad=0):
    style = f'arc3,rad={rad}' if rad else 'arc3,rad=0'
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle='->', color=color,
                                lw=lw, linestyle=ls,
                                connectionstyle=style),
                zorder=4)
    if label:
        mx = (x1 + x2) / 2 + label_offset[0]
        my = (y1 + y2) / 2 + label_offset[1]
        ax.text(mx, my, label, ha='center', va='center',
                fontsize=fontsize, color=color, style='italic',
                clip_on=False,
                bbox=dict(boxstyle='round,pad=0.1', facecolor='white',
                          edgecolor='none', alpha=0.9))

# ═══════════════════════════════════════════════════════════════════════
# LEFT COLUMN: Source systems
# ═══════════════════════════════════════════════════════════════════════

# Payment Engine (source of transactions)
draw_box(ax, 0.0, 10.5, 3.5, 0.9, 'Payment Engine Service',
         'Risk assessment request', C['kafka'], fontsize_l=8.5)

# Backoffice UI (source of feedback)
draw_box(ax, 0.0, 4.0, 3.5, 0.9, 'Backoffice Application',
         'Compliance officer', C['kafka'], fontsize_l=8.5)

# ═══════════════════════════════════════════════════════════════════════
# KAFKA TOPICS (vertical strip)
# ═══════════════════════════════════════════════════════════════════════

# Topic 1: fraud.analysis.requested
kafka_x = 4.5
draw_box(ax, kafka_x, 10.5, 3.0, 0.9,
         'fraud.analysis.requested', 'Kafka Topic', '#546E7A',
         fontsize_l=7.5, fontsize_s=6.5)

# Topic 2: agent.manual.feedback
draw_box(ax, kafka_x, 4.0, 3.0, 0.9,
         'agent.manual.feedback', 'Kafka Topic', '#546E7A',
         fontsize_l=7.5, fontsize_s=6.5)

# Arrows: sources → Kafka
draw_arrow(ax, 3.5, 10.95, kafka_x, 10.95, color=C['kafka'])
draw_arrow(ax, 3.5, 4.45, kafka_x, 4.45, color=C['kafka'])

# ═══════════════════════════════════════════════════════════════════════
# THREE TIERS (center column)
# ═══════════════════════════════════════════════════════════════════════

tier_x = 8.5
tw, th = 4.5, 1.8  # tier box width/height

# ── TIER 1: Automated Heuristic ──────────────────────────────────────
t1_y = 9.8
tier1_bg = FancyBboxPatch((tier_x - 0.2, t1_y - 0.1), tw + 0.4, th + 0.2,
                          boxstyle="round,pad=0.1",
                          facecolor='#E3F2FD', edgecolor=C['tier1'],
                          linewidth=1.5, alpha=0.5, zorder=2)
ax.add_patch(tier1_bg)

ax.text(tier_x + tw/2, t1_y + th - 0.15, 'TIER 1 — Automated Heuristic  (x1)',
        ha='center', fontsize=10, fontweight='bold', color=C['tier1'],
        zorder=6, clip_on=False)

# Reward table for Tier 1
t1_rows = [
    ('BLOCK + high risk',  '+0.30'),
    ('ALLOW + low risk',   '+0.30'),
    ('Action conflicts',   '-0.30'),
    ('REVIEW / ESCALATE',  ' 0.00'),
]
for i, (scenario, val) in enumerate(t1_rows):
    ypos = t1_y + th - 0.6 - i * 0.32
    vc = C['positive'] if val.startswith('+') else (C['negative'] if val.startswith('-') else '#78909C')
    ax.text(tier_x + 0.3, ypos, scenario, fontsize=7, color=C['text'],
            va='center', zorder=6, clip_on=False)
    ax.text(tier_x + tw - 0.3, ypos, val, fontsize=7.5, fontweight='bold',
            color=vc, ha='right', va='center', zorder=6, clip_on=False,
            fontfamily='monospace')

# Arrow: Kafka topic 1 → Tier 1
draw_arrow(ax, kafka_x + 3.0, 10.95, tier_x - 0.2, t1_y + th/2, color=C['tier1'], lw=2)

# ── TIER 2: Manual Review ────────────────────────────────────────────
t2_y = 5.8
th2 = 2.2
tier2_bg = FancyBboxPatch((tier_x - 0.2, t2_y - 0.1), tw + 0.4, th2 + 0.2,
                          boxstyle="round,pad=0.1",
                          facecolor='#FFF3E0', edgecolor=C['tier2'],
                          linewidth=1.5, alpha=0.5, zorder=2)
ax.add_patch(tier2_bg)

ax.text(tier_x + tw/2, t2_y + th2 - 0.15, 'TIER 2 — Manual Review  (x2)',
        ha='center', fontsize=10, fontweight='bold', color='#E65100',
        zorder=6, clip_on=False)

t2_rows = [
    ('BLOCK confirmed fraud',     '+1.0 x2 = +2.00'),
    ('ALLOW confirmed legit',     '+0.5 x2 = +1.00'),
    ('BLOCK but was legit (FP)',   '-0.5 x2 = -1.00'),
    ('ALLOW but was fraud (FN)',   '-1.0 x2 = -2.00'),
    ('REVIEW  \u2192  fraud found', '+0.3 x2 = +0.60'),
    ('REVIEW  \u2192  legit',       '+0.15x2 = +0.30'),
]
for i, (scenario, val) in enumerate(t2_rows):
    ypos = t2_y + th2 - 0.55 - i * 0.3
    vc = C['positive'] if '+' in val else C['negative']
    ax.text(tier_x + 0.3, ypos, scenario, fontsize=6.8, color=C['text'],
            va='center', zorder=6, clip_on=False)
    ax.text(tier_x + tw - 0.3, ypos, val, fontsize=6.8, fontweight='bold',
            color=vc, ha='right', va='center', zorder=6, clip_on=False,
            fontfamily='monospace')

# ── TIER 3: Decision Override ─────────────────────────────────────────
t3_y = 3.5
th3 = 1.5
tier3_bg = FancyBboxPatch((tier_x - 0.2, t3_y - 0.1), tw + 0.4, th3 + 0.2,
                          boxstyle="round,pad=0.1",
                          facecolor='#FFEBEE', edgecolor=C['tier3'],
                          linewidth=1.5, alpha=0.5, zorder=2)
ax.add_patch(tier3_bg)

ax.text(tier_x + tw/2, t3_y + th3 - 0.15, 'TIER 3 — Decision Override  (x3)',
        ha='center', fontsize=10, fontweight='bold', color=C['tier3'],
        zorder=6, clip_on=False)

t3_rows = [
    ('Override BLOCK \u2192 APPROVE',  '-0.9 x3 = -2.70'),
    ('Override ALLOW \u2192 REJECT',   '-1.2 x3 = -3.60'),
]
for i, (scenario, val) in enumerate(t3_rows):
    ypos = t3_y + th3 - 0.55 - i * 0.35
    ax.text(tier_x + 0.3, ypos, scenario, fontsize=7, color=C['text'],
            va='center', zorder=6, clip_on=False)
    ax.text(tier_x + tw - 0.3, ypos, val, fontsize=7, fontweight='bold',
            color=C['negative'], ha='right', va='center', zorder=6, clip_on=False,
            fontfamily='monospace')

# Arrow: Kafka topic 2 → Tier 2 and Tier 3
draw_arrow(ax, kafka_x + 3.0, 4.8, tier_x - 0.2, t2_y + 0.5,
           'feedbackType =\nMANUAL_REVIEW', C['tier2'], lw=2,
           label_offset=(0, 0.6), fontsize=6.5)
draw_arrow(ax, kafka_x + 3.0, 4.1, tier_x - 0.2, t3_y + th3/2,
           'feedbackType =\nDECISION_OVERRIDE', C['tier3'], lw=2,
           label_offset=(0, -0.5), fontsize=6.5)

# ═══════════════════════════════════════════════════════════════════════
# RIGHT COLUMN: Replay Buffer & Training
# ═══════════════════════════════════════════════════════════════════════

buf_x = 14.0
buf_w = 3.5

# Replay Buffer (central, tall)
buf_y = 5.5
buf_h = 3.0
buffer_bg = FancyBboxPatch((buf_x, buf_y), buf_w, buf_h,
                           boxstyle="round,pad=0.15",
                           facecolor=C['buffer'], edgecolor='white',
                           linewidth=2, alpha=0.9, zorder=5)
ax.add_patch(buffer_bg)

ax.text(buf_x + buf_w/2, buf_y + buf_h - 0.3, 'Replay Buffer',
        ha='center', fontsize=10, fontweight='bold', color='white',
        zorder=6, clip_on=False)
ax.text(buf_x + buf_w/2, buf_y + buf_h - 0.65, 'agent_replay_buffer',
        ha='center', fontsize=7.5, color='white', alpha=0.85,
        zorder=6, style='italic', clip_on=False)

buf_fields = [
    'state (s)',
    'actions (a)',
    'effective_reward (r)',
    'next_state (s\')',
    'reward_source',
    'is_used_in_training',
]
for i, field in enumerate(buf_fields):
    ypos = buf_y + buf_h - 1.15 - i * 0.32
    ax.text(buf_x + 0.4, ypos, field, fontsize=6.8, color='white',
            alpha=0.9, zorder=6, clip_on=False, fontfamily='monospace')

# Arrows: Tiers → Replay buffer
draw_arrow(ax, tier_x + tw + 0.2, t1_y + th/2, buf_x, buf_y + buf_h - 0.5,
           'save_experience()', C['tier1'], lw=2, label_offset=(0, 0.35), fontsize=6.5)

draw_arrow(ax, tier_x + tw + 0.2, t2_y + th2/2, buf_x, buf_y + buf_h/2,
           'apply_manual_reward()\nis_used = False', C['tier2'], lw=2,
           label_offset=(0.1, 0.35), fontsize=6.5)

draw_arrow(ax, tier_x + tw + 0.2, t3_y + th3/2, buf_x, buf_y + 0.5,
           'apply_manual_reward()\nis_used = False', C['tier3'], lw=2,
           label_offset=(0.1, -0.35), fontsize=6.5)

# ── Offline Training ─────────────────────────────────────────────────
train_y = 2.0
train_h = 2.2
training_bg = FancyBboxPatch((buf_x, train_y), buf_w, train_h,
                             boxstyle="round,pad=0.15",
                             facecolor=C['training'], edgecolor='white',
                             linewidth=2, alpha=0.9, zorder=5)
ax.add_patch(training_bg)

ax.text(buf_x + buf_w/2, train_y + train_h - 0.3, 'Offline Training',
        ha='center', fontsize=10, fontweight='bold', color='white',
        zorder=6, clip_on=False)
ax.text(buf_x + buf_w/2, train_y + train_h - 0.65, 'OfflineTrainerService',
        ha='center', fontsize=7.5, color='white', alpha=0.85,
        zorder=6, style='italic', clip_on=False)

train_steps = [
    '1. refresh_config()',
    '2. sample unused entries',
    '3. entries_to_numpy()',
    '4. MADDPG critic update',
    '5. update actor networks',
]
for i, step in enumerate(train_steps):
    ypos = train_y + train_h - 1.1 - i * 0.26
    ax.text(buf_x + 0.4, ypos, step, fontsize=6.5, color='white',
            alpha=0.9, zorder=6, clip_on=False)

# Arrow: Buffer → Training
draw_arrow(ax, buf_x + buf_w/2, buf_y, buf_x + buf_w/2, train_y + train_h,
           'sample(batch_size)', C['buffer'], lw=2, label_offset=(1.2, 0.0), fontsize=6.5)

# Arrow: Training → Buffer (mark as used)
draw_arrow(ax, buf_x + buf_w - 0.3, train_y + train_h, buf_x + buf_w - 0.3, buf_y,
           'mark used', '#78909C', lw=1.2, ls='--', label_offset=(0.5, 0.0), fontsize=6)

# ── Configuration Service ──────────────────────────────────────────────
config_y = 0.3
config_bg = FancyBboxPatch((buf_x, config_y), buf_w, 1.0,
                           boxstyle="round,pad=0.12",
                           facecolor=C['config'], edgecolor='white',
                           linewidth=1.5, alpha=0.85, zorder=5)
ax.add_patch(config_bg)

ax.text(buf_x + buf_w/2, config_y + 0.65, 'Configuration Service',
        ha='center', fontsize=8.5, fontweight='bold', color='white',
        zorder=6, clip_on=False)
ax.text(buf_x + buf_w/2, config_y + 0.3, 'Hot-reload: weights, thresholds, freeze',
        ha='center', fontsize=6.5, color='white', alpha=0.85,
        zorder=6, clip_on=False)

# Arrow: Config → Training
draw_arrow(ax, buf_x + buf_w/2, config_y + 1.0, buf_x + buf_w/2, train_y,
           None, C['config'], lw=1.2, ls='--')

# ═══════════════════════════════════════════════════════════════════════
# KEY INSIGHT ANNOTATION
# ═══════════════════════════════════════════════════════════════════════

insight_bg = FancyBboxPatch((0.0, 0.3), 7.0, 2.5,
                            boxstyle="round,pad=0.15",
                            facecolor='#F3E5F5', edgecolor='#AB47BC',
                            linewidth=1.2, alpha=0.7, zorder=3)
ax.add_patch(insight_bg)

ax.text(3.5, 2.5, 'Design Rationale', ha='center',
        fontsize=9, fontweight='bold', color='#6A1B9A', zorder=4, clip_on=False)
ax.text(3.5, 2.1, 'Higher tiers carry stronger signal because they',
        ha='center', fontsize=7.5, color='#4A148C', zorder=4, clip_on=False)
ax.text(3.5, 1.75, 'reflect ground truth from human domain experts.',
        ha='center', fontsize=7.5, color='#4A148C', zorder=4, clip_on=False)
ax.text(3.5, 1.3, 'Tier 3 (x3) is strongest: a decision override means',
        ha='center', fontsize=7.5, color='#4A148C', zorder=4, clip_on=False)
ax.text(3.5, 0.95, 'the system allowed a fraudulent payment to complete,',
        ha='center', fontsize=7.5, color='#4A148C', zorder=4, clip_on=False)
ax.text(3.5, 0.6, 'the costliest possible error in AML compliance.',
        ha='center', fontsize=7.5, color='#4A148C', zorder=4, clip_on=False)

# ═══════════════════════════════════════════════════════════════════════
# RE-SAMPLING ANNOTATION
# ═══════════════════════════════════════════════════════════════════════

resample_bg = FancyBboxPatch((0.0, 6.3), 3.5, 2.2,
                             boxstyle="round,pad=0.12",
                             facecolor='#E8EAF6', edgecolor='#5C6BC0',
                             linewidth=1.0, alpha=0.7, zorder=3)
ax.add_patch(resample_bg)

ax.text(1.75, 8.15, 'Re-Sampling Mechanism', ha='center',
        fontsize=8, fontweight='bold', color='#283593', zorder=4, clip_on=False)
ax.text(1.75, 7.7, 'When Tier 2/3 reward arrives:', ha='center',
        fontsize=7, color='#283593', zorder=4, clip_on=False)
ax.text(1.75, 7.35, 'effective_reward = new value', ha='center',
        fontsize=6.8, color='#283593', zorder=4, clip_on=False,
        fontfamily='monospace')
ax.text(1.75, 7.0, 'is_used_in_training = False', ha='center',
        fontsize=6.8, color='#283593', zorder=4, clip_on=False,
        fontfamily='monospace')
ax.text(1.75, 6.6, 'Ensures corrected experience is', ha='center',
        fontsize=7, color='#283593', zorder=4, clip_on=False)
ax.text(1.75, 6.3, '', ha='center', fontsize=7, clip_on=False)

# ── APScheduler annotation ───────────────────────────────────────────
ax.text(buf_x + buf_w/2, train_y - 0.15, 'APScheduler: every 300s',
        ha='center', fontsize=7, color='#00695C', style='italic',
        clip_on=False,
        bbox=dict(boxstyle='round,pad=0.08', facecolor='#E0F2F1',
                  edgecolor='#26A69A', alpha=0.8))

# ── Title ──────────────────────────────────────────────────────────────
fig.suptitle('Three-Tier Reward Flow: Progressive Human-in-the-Loop Learning',
             fontsize=14, fontweight='bold', color=C['text'], y=0.99)

ax.text(9.0, 12.0, 'Automated (x1)  \u2192  Manual Review (x2)  \u2192  Decision Override (x3)',
        ha='center', fontsize=10, color='#546E7A', clip_on=False)

# ── Save ───────────────────────────────────────────────────────────────
output_path = '/Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/simulation_tests/reports/charts/figO_three_tier_reward_flow.png'
plt.savefig(output_path)
plt.close()
print(f"Saved: {output_path}")
