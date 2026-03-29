"""
Figure R: MADDPG Orchestrator Confidence Distribution
Shows the confidence profile of 10,000 decisions split by detection outcome.
Real data from results.csv.
For: Chapter 5, Section 5.3.5 (MADDPG Decision Analysis)
"""

import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import numpy as np
import csv
import os

plt.rcParams.update({
    'font.family': 'serif',
    'font.size': 10,
    'figure.dpi': 300,
    'savefig.dpi': 300,
    'savefig.bbox': 'tight',
    'savefig.pad_inches': 0.3,
})

# ── Load data ─────────────────────────────────────────────────────────
BASE = '/Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance'
results_path = os.path.join(BASE, 'simulation_tests/reports/results.csv')

with open(results_path) as f:
    reader = csv.DictReader(f)
    rows = list(reader)

# Split by detection result
conf_tp = [float(r['orchestrator_conf']) for r in rows if r['detection_result'] == 'TP']
conf_fp = [float(r['orchestrator_conf']) for r in rows if r['detection_result'] == 'FP']
conf_tn = [float(r['orchestrator_conf']) for r in rows if r['detection_result'] == 'TN']
conf_fn = [float(r['orchestrator_conf']) for r in rows if r['detection_result'] == 'FN']

# Also split by action
conf_review = [float(r['orchestrator_conf']) for r in rows if r['orchestrator_action'] == 'REVIEW']
conf_allow  = [float(r['orchestrator_conf']) for r in rows if r['orchestrator_action'] == 'ALLOW']

print(f"TP: {len(conf_tp)}, FP: {len(conf_fp)}, TN: {len(conf_tn)}, FN: {len(conf_fn)}")
print(f"REVIEW: {len(conf_review)}, ALLOW: {len(conf_allow)}")

# ── Figure: 2 rows ───────────────────────────────────────────────────
fig, axes = plt.subplots(2, 1, figsize=(14, 10), gridspec_kw={'height_ratios': [1.3, 1]})

C = {
    'tp':     '#66BB6A',  # Green
    'fp':     '#FFA726',  # Orange
    'tn':     '#42A5F5',  # Blue
    'fn':     '#EF5350',  # Red
    'review': '#7E57C2',  # Purple
    'allow':  '#26A69A',  # Teal
}

bins = np.linspace(0.50, 0.58, 50)

# ── Top: By detection outcome ────────────────────────────────────────
ax1 = axes[0]
ax1.hist(conf_tp, bins=bins, alpha=0.65, color=C['tp'], label=f'True Positive (n={len(conf_tp):,})',
         edgecolor='white', linewidth=0.5, zorder=4)
ax1.hist(conf_fn, bins=bins, alpha=0.8, color=C['fn'], label=f'False Negative (n={len(conf_fn)})',
         edgecolor='white', linewidth=0.5, zorder=5)

# FP and TN on twin axis (different scale)
ax1_twin = ax1.twinx()
ax1_twin.hist(conf_fp, bins=bins, alpha=0.3, color=C['fp'],
              label=f'False Positive (n={len(conf_fp):,})',
              edgecolor='white', linewidth=0.5, zorder=2)
ax1_twin.hist(conf_tn, bins=bins, alpha=0.3, color=C['tn'],
              label=f'True Negative (n={len(conf_tn):,})',
              edgecolor='white', linewidth=0.5, zorder=2)

ax1.set_ylabel('Count (TP / FN)', fontsize=11, fontweight='bold', color=C['tp'])
ax1_twin.set_ylabel('Count (FP / TN)', fontsize=11, fontweight='bold', color=C['fp'])
ax1.set_title('Confidence Distribution by Detection Outcome', fontsize=12,
              fontweight='bold', pad=8)

# Combined legend
lines1, labels1 = ax1.get_legend_handles_labels()
lines2, labels2 = ax1_twin.get_legend_handles_labels()
ax1.legend(lines1 + lines2, labels1 + labels2, loc='upper right', fontsize=8.5,
           framealpha=0.9, edgecolor='#B0BEC5')

ax1.grid(True, alpha=0.3, ls='--', zorder=0)

# Statistics annotation
stats_text = (
    f'Overall: mean={np.mean(conf_tp + conf_fp + conf_tn + conf_fn):.4f}, '
    f'range=[{min(conf_tp + conf_fp + conf_tn + conf_fn):.4f}, '
    f'{max(conf_tp + conf_fp + conf_tn + conf_fn):.4f}]'
)
ax1.text(0.02, 0.95, stats_text, transform=ax1.transAxes,
         fontsize=8, color='#37474F', va='top',
         bbox=dict(boxstyle='round,pad=0.2', facecolor='#F5F5F5', alpha=0.9))

# ── Bottom: By orchestrator action ───────────────────────────────────
ax2 = axes[1]

# Violin plot approach
parts = ax2.violinplot([conf_review, conf_allow], positions=[1, 2],
                        showmeans=True, showmedians=True, showextrema=True)

for i, (pc, color) in enumerate(zip(parts['bodies'], [C['review'], C['allow']])):
    pc.set_facecolor(color)
    pc.set_alpha(0.4)
    pc.set_edgecolor(color)

for partname in ('cmeans', 'cmedians', 'cbars', 'cmins', 'cmaxes'):
    parts[partname].set_color('#37474F')
    parts[partname].set_linewidth(1.5)

# Overlay box plot
bp = ax2.boxplot([conf_review, conf_allow], positions=[1, 2],
                  widths=0.15, patch_artist=True,
                  showfliers=False, zorder=6)
for patch, color in zip(bp['boxes'], [C['review'], C['allow']]):
    patch.set_facecolor(color)
    patch.set_alpha(0.7)
for med in bp['medians']:
    med.set_color('white')
    med.set_linewidth(2)

ax2.set_xticks([1, 2])
ax2.set_xticklabels([f'REVIEW\n(n={len(conf_review):,})', f'ALLOW\n(n={len(conf_allow):,})'],
                      fontsize=11, fontweight='bold')
ax2.set_ylabel('Confidence Score', fontsize=11, fontweight='bold')
ax2.set_title('Confidence Distribution by Orchestrator Action', fontsize=12,
              fontweight='bold', pad=8)
ax2.grid(True, alpha=0.3, ls='--', axis='y')

# Stats annotations on violin
for pos, data, color in [(1, conf_review, C['review']), (2, conf_allow, C['allow'])]:
    mean_val = np.mean(data)
    std_val = np.std(data)
    ax2.text(pos + 0.35, mean_val, f'mean={mean_val:.4f}\nstd={std_val:.4f}',
             fontsize=8, color=color, fontweight='bold', va='center',
             bbox=dict(boxstyle='round,pad=0.15', facecolor='white',
                       edgecolor=color, alpha=0.8))

# Insight annotation
insight_box = ax2.text(0.02, 0.05,
    'Narrow confidence range (0.52-0.56) reflects early-stage training;\n'
    'REVIEW actions show slightly higher confidence than ALLOW.',
    transform=ax2.transAxes, fontsize=8, color='#37474F', va='bottom',
    style='italic',
    bbox=dict(boxstyle='round,pad=0.3', facecolor='#FFF8E1',
              edgecolor='#FFB74D', alpha=0.9))

fig.suptitle('MADDPG Orchestrator Confidence Analysis (n = 10,000)',
             fontsize=14, fontweight='bold', y=1.01)
plt.tight_layout()

output_path = os.path.join(BASE, 'simulation_tests/reports/charts/figR_confidence_distribution.png')
plt.savefig(output_path)
plt.close()
print(f"Saved: {output_path}")
