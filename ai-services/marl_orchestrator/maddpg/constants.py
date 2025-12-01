"""
Constants - Centralized constants for MADDPG orchestrator

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from typing import List


# ===========================
# Agent Configuration
# ===========================

# Detection agent names
AGENT_TRANSACTION = "transaction"
AGENT_CUSTOMER = "customer"
AGENT_NETWORK = "network"

# List of all agents (order matters for state vector construction)
AGENT_NAMES: List[str] = [
    AGENT_TRANSACTION,
    AGENT_CUSTOMER,
    AGENT_NETWORK
]

# Number of agents
NUM_AGENTS = len(AGENT_NAMES)


# ===========================
# Action Configuration
# ===========================

# Action types
ACTION_BLOCK = 0
ACTION_ALLOW = 1

# Action labels
ACTION_LABELS = {
    ACTION_BLOCK: "BLOCK",
    ACTION_ALLOW: "ALLOW"
}


# ===========================
# State Configuration
# ===========================

# State vector indices (for reference)
# Format: [txn_prob, txn_score/100, cust_prob, cust_score/100, net_prob, net_score/100]
STATE_TXN_PROB_IDX = 0
STATE_TXN_SCORE_IDX = 1
STATE_CUST_PROB_IDX = 2
STATE_CUST_SCORE_IDX = 3
STATE_NET_PROB_IDX = 4
STATE_NET_SCORE_IDX = 5

# State dimension (2 features per agent: probability, normalized score)
STATE_DIM = NUM_AGENTS * 2


# ===========================
# Model Configuration
# ===========================

# Model file names
MODEL_FILE_CRITIC = "critic.pth"
MODEL_FILE_ACTOR_PREFIX = "actor_"  # e.g., actor_transaction.pth


# ===========================
# Observation Keys
# ===========================

# Expected keys in agent observations
OBS_KEY_PROBABILITY = "probability"
OBS_KEY_RISK_SCORE = "risk_score"

# Score normalization factor
SCORE_NORMALIZATION_FACTOR = 100.0
