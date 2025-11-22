"""
Agents module - Multi-Agent Deep Deterministic Policy Gradient (MADDPG)

Components:
- MADDPGCoordinator: High-level coordinator
- StateManager: Converts observations to state vectors
- DecisionMaker: Aggregates actor outputs into decisions
- NetworkManager: Manages actor and critic networks
- MADDPGTrainer: Handles training logic
- ModelPersistence: Saves/loads models to/from disk
- Actor: Individual actor network
- Critic: Centralized critic network
- ReplayBuffer: Experience replay buffer
"""

from .actor import Actor
from .critic import Critic
from .replay_buffer import ReplayBuffer
from .state_manager import StateManager
from .decision_maker import DecisionMaker
from .network_manager import NetworkManager
from .trainer import MADDPGTrainer
from .model_persistence import ModelPersistence
from .maddpg_agent import MADDPGCoordinator, maddpg_coordinator

__all__ = [
    "Actor",
    "Critic",
    "ReplayBuffer",
    "StateManager",
    "DecisionMaker",
    "NetworkManager",
    "MADDPGTrainer",
    "ModelPersistence",
    "MADDPGCoordinator",
    "maddpg_coordinator"
]
