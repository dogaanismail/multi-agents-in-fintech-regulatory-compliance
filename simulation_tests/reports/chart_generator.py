"""
Chart Generator
===============
Produces dissertation-quality matplotlib figures from simulation results.

All figures are saved as high-resolution PNG (300 dpi) into charts_dir.
They are designed for direct insertion into Chapter 5 of the dissertation.

Figures produced:
  Figure A — Confusion Matrix Heatmap
  Figure B — Detection Rate by Fraud Typology (all 5 typologies)
  Figure C — Payment-Svc Latency Distribution (histogram + percentile lines)
  Figure D — Orchestrator Action Distribution (horizontal bar)
  Figure E — Agent Risk Score Distributions (violin plot: TPA / CRA / NAA)
  Figure F — Precision–Recall summary bar chart
  Figure G — CRA SHAP Contributing Factors frequency bar
  Figure H — Agent Specialisation Heatmap (mean score per agent per typology) [Ch5]
  Figure I — XAI Violin: risk score by fraud label per agent [RQ3]
  Figure J — Aggregated Feature Attribution across all agents [RQ3]
"""

import logging
import os
from typing import List

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import matplotlib.ticker as mticker
import numpy as np
import pandas as pd
import seaborn as sns

from simulation_tests.config import SIM_CONFIG
from simulation_tests.domain.transaction.models import TransactionRecord
from simulation_tests.reports.aggregator import SimulationSummary

logger = logging.getLogger(__name__)

# ── Palette ────────────────────────────────────────────────────────────────
PAL = {
    "tp":      "#27AE60",
    "fp":      "#E74C3C",
    "tn":      "#2980B9",
    "fn":      "#E67E22",
    "tpa":     "#8E44AD",
    "cra":     "#16A085",
    "naa":     "#D35400",
    "allow":   "#27AE60",
    "review":  "#F39C12",
    "block":   "#C0392B",
    "latency": "#2980B9",
    "neutral": "#7F8C8D",
}

_TYPOLOGY_COLORS = [PAL["tp"], PAL["tpa"], PAL["naa"], PAL["review"], PAL["cra"]]
_AGENT_COLORS    = [PAL["tpa"], PAL["cra"], PAL["naa"]]


def _apply_style() -> None:
    """Apply consistent academic style to all subsequent plots."""
    sns.set_theme(style="whitegrid", font="DejaVu Serif")
    plt.rcParams.update({
        "font.family":       "serif",
        "font.size":         11,
        "axes.titlesize":    13,
        "axes.titleweight":  "bold",
        "axes.labelsize":    11,
        "axes.labelweight":  "bold",
        "axes.spines.top":   False,
        "axes.spines.right": False,
        "figure.dpi":        150,
        "xtick.labelsize":   9,
        "ytick.labelsize":   9,
        "legend.fontsize":   9,
        "legend.framealpha": 0.85,
    })


def _save(fig: plt.Figure, name: str) -> str:
    path = os.path.join(SIM_CONFIG.charts_dir, name)
    fig.savefig(path, dpi=300, bbox_inches="tight", facecolor="white")
    plt.close(fig)
    logger.info("Chart saved → %s", path)
    return path


def _bar_value_labels(ax, bars, fmt="{:.1f}%", offset_frac=0.012, color="black", fontsize=9):
    """Place value labels above each bar."""
    ylim_top = ax.get_ylim()[1]
    for bar in bars:
        h = bar.get_height()
        ax.text(
            bar.get_x() + bar.get_width() / 2,
            h + ylim_top * offset_frac,
            fmt.format(h),
            ha="center", va="bottom",
            fontsize=fontsize, fontweight="bold", color=color,
        )


class ChartGenerator:
    def __init__(self):
        os.makedirs(SIM_CONFIG.charts_dir, exist_ok=True)
        _apply_style()

    def generate_all(
        self,
        transactions: List[TransactionRecord],
        summary: SimulationSummary,
    ) -> List[str]:
        paths = []
        paths.append(self.confusion_matrix(summary))
        paths.append(self.detection_by_typology(summary))
        paths.append(self.latency_distribution(transactions))
        paths.append(self.orchestrator_actions(summary))
        paths.append(self.agent_risk_scores(transactions))
        paths.append(self.precision_recall_bar(summary))
        paths.append(self.shap_factors_bar(transactions))
        paths.append(self.agent_specialisation_heatmap(transactions))
        paths.append(self.xai_violin_by_label(transactions))
        paths.append(self.feature_attribution_bar(transactions))
        paths = [p for p in paths if p]
        logger.info("All %d charts generated in %s", len(paths), SIM_CONFIG.charts_dir)
        return paths

    # ── Figure A — Confusion Matrix ────────────────────────────────────────
    def confusion_matrix(self, summary: SimulationSummary) -> str:
        matrix = np.array([
            [summary.true_negatives,  summary.false_positives],
            [summary.false_negatives, summary.true_positives],
        ])
        cell_labels = np.array([
            [f"TN\n{summary.true_negatives:,}",  f"FP\n{summary.false_positives:,}"],
            [f"FN\n{summary.false_negatives:,}", f"TP\n{summary.true_positives:,}"],
        ])

        fig, ax = plt.subplots(figsize=(5.5, 4.5))
        cmap = sns.light_palette("#2980B9", as_cmap=True)
        sns.heatmap(
            matrix, annot=cell_labels, fmt="", cmap=cmap,
            linewidths=2, linecolor="white",
            xticklabels=["Predicted: Legitimate", "Predicted: Fraud"],
            yticklabels=["Actual: Legitimate", "Actual: Fraud"],
            ax=ax, cbar=False, annot_kws={"size": 14, "weight": "bold"},
        )
        ax.set_title("Confusion Matrix — MADDPG AML Detection System")
        ax.tick_params(axis="x", labelsize=10)
        ax.tick_params(axis="y", labelsize=10, rotation=0)
        fig.tight_layout()
        return _save(fig, "figA_confusion_matrix.png")

    # ── Figure B — Detection Rate by Typology ─────────────────────────────
    def detection_by_typology(self, summary: SimulationSummary) -> str:
        typologies = ["Smurfing\n(Structuring)", "Fan-Out\n(Dispersal)",
                      "Cross-Border\nLayering", "High-Value\nSingle",
                      "Round-Trip\n(Boomerang)"]
        rates  = [summary.smurfing_detection_rate * 100,
                  summary.fanout_detection_rate * 100,
                  summary.layering_detection_rate * 100,
                  summary.high_value_detection_rate * 100,
                  summary.round_trip_detection_rate * 100]
        counts = [summary.smurfing_count, summary.fanout_count,
                  summary.layering_count, summary.high_value_count,
                  summary.round_trip_count]

        fig, ax = plt.subplots(figsize=(10, 5))
        bars = ax.bar(typologies, rates, color=_TYPOLOGY_COLORS,
                      edgecolor="white", linewidth=1.2, width=0.55, zorder=3)

        for bar, rate, n in zip(bars, rates, counts):
            ax.text(bar.get_x() + bar.get_width() / 2,
                    bar.get_height() + 2.0,
                    f"{rate:.1f}%\nn={n:,}",
                    ha="center", va="bottom", fontsize=9, fontweight="bold")

        ax.set_ylim(0, 125)
        ax.yaxis.set_major_formatter(mticker.PercentFormatter())
        ax.set_ylabel("Detection Rate")
        ax.set_title("Fraud Detection Rate by AML Typology")
        ax.axhline(100, color=PAL["neutral"], linestyle="--", linewidth=0.9, alpha=0.6, zorder=2)
        ax.yaxis.grid(True, linestyle="--", alpha=0.5, zorder=0)
        ax.set_axisbelow(True)
        fig.tight_layout()
        return _save(fig, "figB_detection_by_typology.png")

    # ── Figure C — Latency Distribution ───────────────────────────────────
    def latency_distribution(self, transactions: List[TransactionRecord]) -> str:
        latencies = [t.latency_ms for t in transactions
                     if t.latency_ms is not None and t.latency_ms > 0]
        if not latencies:
            logger.warning("No latency data for chart C — skipping.")
            return ""

        p50 = np.percentile(latencies, 50)
        p95 = np.percentile(latencies, 95)
        p99 = np.percentile(latencies, 99)

        fig, ax = plt.subplots(figsize=(8, 4.5))
        ax.hist(latencies, bins=50, color=PAL["latency"], edgecolor="white",
                alpha=0.85, zorder=3)

        for val, label, color in [
            (p50, f"P50 = {p50:.0f} ms", "#2C3E50"),
            (p95, f"P95 = {p95:.0f} ms", PAL["review"]),
            (p99, f"P99 = {p99:.0f} ms", PAL["fp"]),
        ]:
            ax.axvline(val, linestyle="--", linewidth=1.5, color=color, zorder=4)
            ax.text(val + 1, ax.get_ylim()[1] * 0.88, label,
                    color=color, fontsize=8.5, rotation=90, va="top")

        ax.set_xlabel("Latency (ms) — payment-svc round-trip")
        ax.set_ylabel("Request Count")
        ax.set_title(f"Payment Submission Latency  "
                     f"(n={len(latencies):,}, mean={np.mean(latencies):.0f} ms)")
        ax.yaxis.grid(True, linestyle="--", alpha=0.4, zorder=0)
        ax.set_axisbelow(True)
        fig.tight_layout()
        return _save(fig, "figC_latency_distribution.png")

    # ── Figure D — Orchestrator Action Distribution (horizontal bar) ───────
    def orchestrator_actions(self, summary: SimulationSummary) -> str:
        labels = ["APPROVE", "REVIEW / FLAG", "BLOCK"]
        counts = [summary.orchestrator_approve_count,
                  summary.orchestrator_flag_count,
                  summary.orchestrator_block_count]
        colors = [PAL["allow"], PAL["review"], PAL["block"]]
        total  = sum(counts) or 1

        data = [(l, c, col) for l, c, col in zip(labels, counts, colors) if c > 0]
        if not data:
            logger.warning("No orchestrator data for chart D.")
            return ""
        labels_, counts_, colors_ = zip(*data)

        fig, ax = plt.subplots(figsize=(7, 3.5))
        bars = ax.barh(labels_, counts_, color=colors_, edgecolor="white",
                       linewidth=1.2, height=0.5, zorder=3)
        for bar, cnt in zip(bars, counts_):
            ax.text(bar.get_width() + total * 0.005, bar.get_y() + bar.get_height() / 2,
                    f"{cnt:,}  ({cnt / total * 100:.1f}%)",
                    va="center", fontsize=9, fontweight="bold")

        ax.set_xlabel("Number of Payments")
        ax.set_xlim(0, max(counts_) * 1.25)
        ax.set_title("MADDPG Orchestrator Action Distribution")
        ax.xaxis.grid(True, linestyle="--", alpha=0.4, zorder=0)
        ax.set_axisbelow(True)
        fig.tight_layout()
        return _save(fig, "figD_orchestrator_actions.png")

    # ── Figure E — Agent Risk Score Violin Plots ────────────────────────────
    def agent_risk_scores(self, transactions: List[TransactionRecord]) -> str:
        agents = ["TPA", "CRA", "NAA"]
        getters = [
            lambda t: t.agent_signals.tpa_risk_score,
            lambda t: t.agent_signals.cra_risk_score,
            lambda t: t.agent_signals.naa_risk_score,
        ]
        scores_list = [
            [g(t) for t in transactions if g(t) is not None]
            for g in getters
        ]
        available = [(a, s, c) for a, s, c in zip(agents, scores_list, _AGENT_COLORS) if s]
        if not available:
            logger.warning("No agent risk score data for chart E.")
            return ""

        agents_, scores_, colors_ = zip(*available)
        fig, axes = plt.subplots(1, len(agents_), figsize=(4 * len(agents_), 5), sharey=False)
        if len(agents_) == 1:
            axes = [axes]

        for ax, agent, scores, color in zip(axes, agents_, scores_, colors_):
            parts = ax.violinplot(scores, positions=[0], showmedians=True,
                                  showextrema=True, widths=0.7)
            for pc in parts["bodies"]:
                pc.set_facecolor(color)
                pc.set_alpha(0.7)
            parts["cmedians"].set_color("white")
            parts["cmedians"].set_linewidth(2.5)
            parts["cmaxes"].set_color(color)
            parts["cmins"].set_color(color)
            parts["cbars"].set_color(color)

            median = np.median(scores)
            ax.annotate(f"Median\n{median:.1f}",
                        xy=(0, median), xytext=(0.35, median),
                        fontsize=8, ha="left", color=color, fontweight="bold",
                        arrowprops=dict(arrowstyle="->", color=color, lw=1.2))

            ax.set_title(agent, fontsize=12, fontweight="bold", color=color)
            ax.set_ylabel("Risk Score (0–100)")
            ax.set_xticks([])
            ax.yaxis.grid(True, linestyle="--", alpha=0.4)
            ax.set_axisbelow(True)

        fig.suptitle("Agent Risk Score Distributions  (All Transactions)", fontsize=13, fontweight="bold")
        fig.tight_layout()
        return _save(fig, "figE_agent_risk_scores.png")

    # ── Figure F — Classification Metrics Bar ──────────────────────────────
    def precision_recall_bar(self, summary: SimulationSummary) -> str:
        metrics = {
            "Precision":       summary.precision,
            "Recall":          summary.recall,
            "Specificity":     summary.specificity,
            "F1 Score":        summary.f1_score,
            "Accuracy":        summary.accuracy,
        }
        colors = [PAL["tpa"], PAL["tp"], PAL["tn"], PAL["review"], PAL["cra"]]

        fig, ax = plt.subplots(figsize=(8, 4.5))
        bars = ax.bar(list(metrics.keys()),
                      [v * 100 for v in metrics.values()],
                      color=colors, edgecolor="white", linewidth=1.2, zorder=3)

        ax.set_ylim(0, 120)
        ax.yaxis.set_major_formatter(mticker.PercentFormatter())
        _bar_value_labels(ax, bars, fmt="{:.1f}%", offset_frac=0.012)
        ax.set_ylabel("Score")
        ax.set_title("System-Level Classification Metrics")
        ax.yaxis.grid(True, linestyle="--", alpha=0.4, zorder=0)
        ax.set_axisbelow(True)
        fig.tight_layout()
        return _save(fig, "figF_precision_recall.png")

    # ── Figure G — CRA SHAP Contributing Factors ──────────────────────────
    def shap_factors_bar(self, transactions: List[TransactionRecord]) -> str:
        from collections import Counter
        counter: Counter = Counter()
        for t in transactions:
            for f in (t.agent_signals.cra_contributing_factors or []):
                clean = str(f).strip().lower().lstrip("0123456789. -").replace("_", " ")
                if clean:
                    counter[clean] += 1

        if not counter:
            logger.warning("No CRA contributing factors for chart G.")
            return ""

        top_n = counter.most_common(12)
        labels = [item[0].title() for item in top_n]
        values = [item[1] for item in top_n]
        palette = sns.light_palette(PAL["cra"], n_colors=len(labels) + 2, reverse=True)[1:-1][:len(labels)]

        fig, ax = plt.subplots(figsize=(9, 5.5))
        bars = ax.barh(labels[::-1], values[::-1], color=palette[::-1],
                       edgecolor="white", linewidth=0.8, height=0.65, zorder=3)
        for bar, val in zip(bars, values[::-1]):
            ax.text(bar.get_width() + max(values) * 0.01, bar.get_y() + bar.get_height() / 2,
                    f"{val:,}", va="center", fontsize=8.5, fontweight="bold")

        ax.set_xlabel("Frequency (transactions flagged by this factor)")
        ax.set_title("Top CRA SHAP Contributing Factors")
        ax.xaxis.grid(True, linestyle="--", alpha=0.4, zorder=0)
        ax.set_axisbelow(True)
        ax.set_xlim(0, max(values) * 1.2)
        fig.tight_layout()
        return _save(fig, "figG_shap_cra_factors.png")

    # ── Figure H — Agent Specialisation Heatmap ────────────────────────────
    def agent_specialisation_heatmap(self, transactions: List[TransactionRecord]) -> str:
        from simulation_tests.domain.transaction.models import FraudLabel

        labels_order = [FraudLabel.LEGITIMATE, FraudLabel.SMURFING, FraudLabel.FAN_OUT,
                        FraudLabel.LAYERING, FraudLabel.HIGH_VALUE_SINGLE, FraudLabel.ROUND_TRIP]
        label_names  = ["Legitimate", "Smurfing", "Fan-Out", "Layering", "High-Value", "Round-Trip"]
        agents        = ["TPA", "CRA", "NAA"]
        getters       = [
            lambda t: t.agent_signals.tpa_risk_score,
            lambda t: t.agent_signals.cra_risk_score,
            lambda t: t.agent_signals.naa_risk_score,
        ]

        matrix = np.full((len(agents), len(labels_order)), np.nan)
        for j, label in enumerate(labels_order):
            subset = [t for t in transactions if t.fraud_label == label]
            for i, getter in enumerate(getters):
                scores = [getter(t) for t in subset if getter(t) is not None]
                if scores:
                    matrix[i, j] = np.mean(scores)

        if np.all(np.isnan(matrix)):
            logger.warning("No agent score data for Figure H — skipping.")
            return ""

        annot = np.where(np.isnan(matrix), "—", np.vectorize(lambda v: f"{v:.1f}")(matrix))
        display = np.nan_to_num(matrix, nan=0.0)

        fig, ax = plt.subplots(figsize=(11, 3.8))
        sns.heatmap(display, annot=annot, fmt="", cmap="YlOrRd",
                    xticklabels=label_names, yticklabels=agents,
                    linewidths=1.5, linecolor="white", ax=ax,
                    vmin=0, vmax=max(np.nanmax(matrix), 1),
                    cbar_kws={"label": "Mean Risk Score (0–100)", "shrink": 0.8},
                    annot_kws={"size": 10, "weight": "bold"})
        ax.set_title("Agent Specialisation Heatmap — Mean Risk Score per Agent per Typology")
        ax.tick_params(axis="y", rotation=0, labelsize=10)
        ax.tick_params(axis="x", labelsize=10)
        fig.tight_layout()
        return _save(fig, "figH_agent_specialisation_heatmap.png")

    # ── Figure I — XAI Violin: score distribution by fraud label ──────────
    def xai_violin_by_label(self, transactions: List[TransactionRecord]) -> str:
        from simulation_tests.domain.transaction.models import FraudLabel

        label_map = {
            FraudLabel.LEGITIMATE:        "Legit",
            FraudLabel.SMURFING:          "Smurfing",
            FraudLabel.FAN_OUT:           "Fan-Out",
            FraudLabel.LAYERING:          "Layering",
            FraudLabel.HIGH_VALUE_SINGLE: "High-Val",
            FraudLabel.ROUND_TRIP:        "Round-Trip",
        }
        getters = {
            "TPA": lambda t: t.agent_signals.tpa_risk_score,
            "CRA": lambda t: t.agent_signals.cra_risk_score,
            "NAA": lambda t: t.agent_signals.naa_risk_score,
        }

        fig, axes = plt.subplots(1, 3, figsize=(16, 5.5), sharey=False)
        fig.suptitle("XAI Agent Risk Score Distributions by Fraud Typology  [RQ3]",
                     fontsize=13, fontweight="bold")

        for ax, (agent_name, getter), color in zip(axes, getters.items(), _AGENT_COLORS):
            data_by_label, tick_labels = [], []
            for label, short in label_map.items():
                scores = [getter(t) for t in transactions
                          if t.fraud_label == label and getter(t) is not None]
                if scores:
                    data_by_label.append(scores)
                    tick_labels.append(short)

            if not data_by_label:
                ax.set_title(agent_name)
                continue

            positions = list(range(len(data_by_label)))
            parts = ax.violinplot(data_by_label, positions=positions,
                                  showmedians=True, showextrema=False, widths=0.7)
            for pc in parts["bodies"]:
                pc.set_facecolor(color)
                pc.set_alpha(0.65)
            parts["cmedians"].set_color("white")
            parts["cmedians"].set_linewidth(2)

            # Overlay individual medians as scatter
            medians = [np.median(d) for d in data_by_label]
            ax.scatter(positions, medians, color=color, s=40, zorder=5, edgecolors="white", linewidths=1)

            ax.set_xticks(positions)
            ax.set_xticklabels(tick_labels, rotation=30, ha="right", fontsize=8.5)
            ax.set_title(agent_name, fontsize=11, fontweight="bold", color=color)
            ax.set_ylabel("Risk Score (0–100)" if ax == axes[0] else "")
            ax.yaxis.grid(True, linestyle="--", alpha=0.4)
            ax.set_axisbelow(True)

        fig.tight_layout()
        return _save(fig, "figI_xai_violin_by_label.png")

    # ── Figure J — Aggregated Feature Attribution ─────────────────────────
    def feature_attribution_bar(self, transactions: List[TransactionRecord]) -> str:
        from collections import Counter
        import re

        counter: Counter = Counter()
        for t in transactions:
            sigs = t.agent_signals
            for f in (sigs.cra_contributing_factors or []):
                clean = re.sub(r"^[\d.\-\s]+", "", str(f).strip().lower()).replace("_", " ")
                if clean:
                    counter[f"CRA: {clean.title()}"] += 1

            rec = str(sigs.tpa_recommendation or "").lower()
            for kw in ["amount", "cross-border", "velocity", "currency", "threshold",
                       "structuring", "suspicious", "high-risk"]:
                if kw in rec:
                    counter[f"TPA: {kw.title()}"] += 1

            for k in (sigs.naa_network_indicators or {}).keys():
                clean_k = re.sub(r"_", " ", str(k).lower()).title()
                counter[f"NAA: {clean_k}"] += 1

        if not counter:
            logger.warning("No feature attribution data for Figure J — skipping.")
            return ""

        top_n = counter.most_common(15)
        labels = [item[0] for item in top_n]
        values = [item[1] for item in top_n]
        bar_colors = [PAL["tpa"] if l.startswith("TPA") else
                      PAL["cra"] if l.startswith("CRA") else PAL["naa"] for l in labels]

        fig, ax = plt.subplots(figsize=(11, 6))
        ax.barh(labels[::-1], values[::-1], color=bar_colors[::-1],
                edgecolor="white", linewidth=0.8, height=0.65, zorder=3)

        for i, (val, lbl) in enumerate(zip(values[::-1], labels[::-1])):
            ax.text(val + max(values) * 0.01, i, f"{val:,}",
                    va="center", fontsize=8.5, fontweight="bold")

        legend_patches = [
            mpatches.Patch(color=PAL["tpa"], label="TPA — Transaction Pattern Agent"),
            mpatches.Patch(color=PAL["cra"], label="CRA — Customer Risk Agent"),
            mpatches.Patch(color=PAL["naa"], label="NAA — Network Analysis Agent"),
        ]
        ax.legend(handles=legend_patches, loc="lower right", fontsize=9)
        ax.set_xlabel("Frequency (transactions where feature was flagged)")
        ax.set_title("Aggregated Feature Attribution Across All Agents  [RQ3]")
        ax.xaxis.grid(True, linestyle="--", alpha=0.4, zorder=0)
        ax.set_axisbelow(True)
        ax.set_xlim(0, max(values) * 1.2)
        fig.tight_layout()
        return _save(fig, "figJ_feature_attribution.png")

