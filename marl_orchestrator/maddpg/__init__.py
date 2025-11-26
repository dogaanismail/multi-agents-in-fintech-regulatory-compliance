"""
MADDPG - Multi-Agent Deep Deterministic Policy Gradient

A reinforcement learning algorithm for multi-agent coordination in AML compliance.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance

Main Components:
- networks: Actor, Critic, ReplayBuffer
- core: Coordinator, Trainer, State/Decision managers
- constants: Shared configuration constants

Usage:
    from maddpg.core import maddpg_coordinator
    
    decision = maddpg_coordinator.decide(observations)
"""

from .core import (
    MADDPGCoordinator,
    maddpg_coordinator,
    StateManager,
    DecisionMaker,
    NetworkManager,
    MADDPGTrainer,
    ModelPersistence,
)

from .networks import Actor, Critic, ReplayBuffer

from .constants import (
    AGENT_NAMES,
    AGENT_TRANSACTION,
    AGENT_CUSTOMER,
    AGENT_NETWORK,
    ACTION_BLOCK,
    ACTION_ALLOW,
    ACTION_LABELS,
)

from .logger import logger, get_logger

__all__ = [
    # Core
    "MADDPGCoordinator",
    "maddpg_coordinator",
    "StateManager",
    "DecisionMaker",
    "NetworkManager",
    "MADDPGTrainer",
    "ModelPersistence",
    # Networks
    "Actor",
    "Critic",
    "ReplayBuffer",
    # Constants
    "AGENT_NAMES",
    "AGENT_TRANSACTION",
    "AGENT_CUSTOMER",
    "AGENT_NETWORK",
    "ACTION_BLOCK",
    "ACTION_ALLOW",
    "ACTION_LABELS",
    # Logging
    "logger",
    "get_logger",
]

__version__ = "1.0.0"
