"""
Compliance Officer MCP Server.

Exposes the platform's persisted decision artifacts — MARL actions, per-agent
SHAP attributions, officer overrides, ledger postings — as MCP tools, so an
officer can interrogate a decision in natural language through any MCP client.

Design principles:
- Read-only. Approving, rejecting and overriding stay in the backoffice UI;
  this server explains decisions, it never makes or changes them.
- Grounded. Every answer is retrieved from persisted decision data. SHAP values
  are the ones stored at decision time, never recomputed: after a model retrain
  a recomputation would explain the decision with a model that did not make it.
"""

import os
from typing import Any

import httpx
from mcp.server.fastmcp import FastMCP

GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:3030/api/v1")

mcp = FastMCP("compliance-officer")

AGENT_OBSERVATION_KEYS = {
    "transaction": "transactionAgentObservation",
    "customer": "customerAgentObservation",
    "network": "networkAgentObservation",
}


def _get(path: str, params: dict | None = None) -> Any:
    with httpx.Client(base_url=GATEWAY_URL, timeout=15) as client:
        response = client.get(path, params=params)
        response.raise_for_status()
        return response.json()


def _payment(payment_id: str) -> dict:
    return _get(f"/payment-history/{payment_id}")


def _observation(payment: dict, agent_type: str) -> dict | None:
    marl = payment.get("marlAssessment") or {}
    key = AGENT_OBSERVATION_KEYS.get(agent_type.lower())
    if key is None:
        raise ValueError(
            f"Unknown agent_type '{agent_type}'; expected one of {sorted(AGENT_OBSERVATION_KEYS)}"
        )
    return marl.get(key)


@mcp.tool()
def get_decision(payment_id: str) -> dict:
    """Final decision for a payment: status, MARL action and confidence, risk
    assessment, per-agent votes, block/failure reasons and lifecycle timestamps.
    Start here when asked why a payment was blocked, escalated or completed."""
    payment = _payment(payment_id)
    marl = payment.get("marlAssessment") or {}

    agent_votes = {}
    for agent_type, key in AGENT_OBSERVATION_KEYS.items():
        observation = marl.get(key) or {}
        agent_votes[agent_type] = {
            "isSuspicious": observation.get("isSuspicious"),
            "fraudProbability": observation.get("probability"),
            "riskScore": observation.get("riskScore"),
            "confidence": observation.get("confidence"),
        }

    return {
        "paymentId": payment.get("paymentId"),
        "referenceNumber": payment.get("referenceNumber"),
        "amount": payment.get("amount"),
        "currency": payment.get("fromCurrency"),
        "paymentType": payment.get("paymentType"),
        "status": payment.get("status"),
        "fraudStatus": payment.get("fraudStatus"),
        "riskLevel": payment.get("riskLevel"),
        "riskAction": payment.get("riskAction"),
        "fraudIndicators": payment.get("fraudIndicators"),
        "marlAction": marl.get("action"),
        "marlConfidence": marl.get("confidence"),
        "maddpgQValue": marl.get("maddpgQValue"),
        "agentVotes": agent_votes,
        "blockReason": payment.get("blockReason"),
        "failureReason": payment.get("failureReason"),
        "timeline": {
            "initiatedAt": payment.get("initiatedAt"),
            "ledgerAuthorisedAt": payment.get("ledgerAuthorisedAt"),
            "riskCheckCompletedAt": payment.get("riskCheckCompletedAt"),
            "ledgerSettledAt": payment.get("ledgerSettledAt"),
            "ledgerReleasedAt": payment.get("ledgerReleasedAt"),
            "completedAt": payment.get("completedAt"),
            "blockedAt": payment.get("blockedAt"),
        },
    }


@mcp.tool()
def explain_agent(payment_id: str, agent_type: str) -> dict:
    """SHAP feature attributions for one agent's score on a payment.
    agent_type: transaction | customer | network. Returns the complete stored
    contribution set (positive values push toward suspicious), the SHAP base
    value, and an additivity check proving the factors reconstruct the score."""
    payment = _payment(payment_id)
    observation = _observation(payment, agent_type)
    if not observation:
        return {"paymentId": payment_id, "agentType": agent_type,
                "error": "No observation stored for this agent on this payment"}

    contributions = observation.get("featureContributions") or []
    base_value = observation.get("shapBaseValue")
    probability = observation.get("probability")

    additivity = None
    if contributions and base_value is not None and probability is not None:
        import math
        margin = base_value + sum(c["shapValue"] for c in contributions)
        reconstructed = 1 / (1 + math.exp(-margin))
        additivity = {
            "reconstructedProbability": reconstructed,
            "reportedProbability": probability,
            "factorsFullyAccountForScore": abs(reconstructed - probability) < 0.01,
        }

    return {
        "paymentId": payment_id,
        "agentType": agent_type,
        "agentName": observation.get("agentName"),
        "isSuspicious": observation.get("isSuspicious"),
        "fraudProbability": probability,
        "confidence": observation.get("confidence"),
        "shapBaseValue": base_value,
        "featureContributions": contributions,
        "additivityCheck": additivity,
        "note": "Attributions are the ones persisted at decision time; they are"
                " never recomputed against a possibly-retrained model.",
    }


@mcp.tool()
def get_agent_contributions(payment_id: str) -> dict:
    """MADDPG coordination view for a payment: how much weight each agent
    carried in the final decision, plus the Q-value and decision mode."""
    payment = _payment(payment_id)
    marl = payment.get("marlAssessment") or {}
    weights = marl.get("agentContributions") or {}
    total = sum(abs(w) for w in weights.values())

    return {
        "paymentId": payment_id,
        "marlAction": marl.get("action"),
        "marlConfidence": marl.get("confidence"),
        "maddpgQValue": marl.get("maddpgQValue"),
        "mode": marl.get("mode"),
        "agentWeights": weights,
        "agentWeightShares": {
            agent: (abs(weight) / total if total else None)
            for agent, weight in weights.items()
        },
    }


@mcp.tool()
def get_override_history(payment_id: str) -> dict:
    """Human decisions recorded against a payment: manual review outcome and
    any compliance-officer override, with names, notes and reasons."""
    payment = _payment(payment_id)
    return {
        "paymentId": payment_id,
        "status": payment.get("status"),
        "manualReview": {
            "reviewedBy": payment.get("manualReviewedBy"),
            "notes": payment.get("manualReviewNotes"),
            "requestedAt": payment.get("manualReviewRequestedAt"),
            "approvedAt": payment.get("manualReviewApprovedAt"),
            "rejectedAt": payment.get("manualReviewRejectedAt"),
        },
        "decisionOverride": {
            "overriddenBy": payment.get("decisionOverriddenBy"),
            "reason": payment.get("decisionOverrideReason"),
            "overriddenAt": payment.get("decisionOverriddenAt"),
        },
    }


@mcp.tool()
def get_ledger_postings(payment_id: str) -> list:
    """Double-entry ledger transfers recorded for a payment: authorisations,
    settlements and releases with amounts and the accounts debited/credited.
    This is what physically happened to the money."""
    return _get(f"/ledger/postings/{payment_id}")


@mcp.tool()
def find_pending_reviews(limit: int = 10) -> list:
    """Payments currently awaiting a compliance officer's manual review,
    newest first."""
    page = _get(
        "/payment-history/status/MANUAL_REVIEW_REQUIRED",
        params={"page": 0, "size": limit, "sort": "createdAt,desc"},
    )
    return [
        {
            "paymentId": p.get("paymentId"),
            "referenceNumber": p.get("referenceNumber"),
            "amount": p.get("amount"),
            "currency": p.get("fromCurrency"),
            "riskLevel": p.get("riskLevel"),
            "marlAction": (p.get("marlAssessment") or {}).get("action"),
            "requestedAt": p.get("manualReviewRequestedAt"),
        }
        for p in page.get("content", [])
    ]


if __name__ == "__main__":
    mcp.run()
