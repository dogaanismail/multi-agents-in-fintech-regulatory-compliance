"""
MADDPG Core - Core coordination and training logic

Components:
- MADDPGCoordinator: High-level coordinator
- StateManager: State conversion
- DecisionMaker: Decision aggregation
- NetworkManager: Network management
- MADDPGTrainer: Training logic
- ModelPersistence: Model save/load
"""

from .coordinator import MADDPGCoordinator, maddpg_coordinator
from .state_manager import StateManager
from .decision_maker import DecisionMaker
from .network_manager import NetworkManager
from .trainer import MADDPGTrainer
from .model_persistence import ModelPersistence

__all__ = [
    "MADDPGCoordinator",
    "maddpg_coordinator",
    "StateManager",
    "DecisionMaker",
    "NetworkManager",
    "MADDPGTrainer",
    "ModelPersistence",
]
