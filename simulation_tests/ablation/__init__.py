"""
Ablation package — individual AI agent clients for standalone benchmarking.

These clients call the AI agents DIRECTLY (bypassing Kafka / payment-svc).
Use these when you want to evaluate each agent in isolation for Chapter 5
or run SHAP feature attribution without a full payment flow.

They are SEPARATE from the main simulation — the main simulation only
uses payment-svc as its entry point (the Kafka chain calls agents internally).
"""
