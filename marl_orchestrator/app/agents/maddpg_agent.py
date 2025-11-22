"""
MADDPG Coordinator - Multi-Agent Deep Deterministic Policy Gradient
Coordinates 3 detection agents for AML compliance

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import torch
import numpy as np
from pathlib import Path
from typing import Dict, Optional

from .state_manager import StateManager
from .decision_maker import DecisionMaker
from .network_manager import NetworkManager
from .trainer import MADDPGTrainer
from .replay_buffer import ReplayBuffer
from ..core.config import settings
from ..core.logging import logger

class MADDPGCoordinator:
    """
    MADDPG Coordinator for multi-agent AML detection
    
    High-level coordinator that orchestrates:
    - State management
    - Neural networks (actors + critic)
    - Decision making
    - Training
    """
    
    def __init__(
        self,
        state_dim: int = 6,
        action_dim: int = 2,
        num_agents: int = 3,
        hidden_dim: int = 256,
        learning_rate: float = 0.001,
        gamma: float = 0.99,
        tau: float = 0.01,
        buffer_size: int = 100000
    ):
        """
        Initialize MADDPG Coordinator
        
        Args:
            state_dim: State dimension (6: [txn_prob, txn_score, cust_prob, cust_score, net_prob, net_score])
            action_dim: Action dimension (2: [BLOCK, ALLOW])
            num_agents: Number of agents (3: transaction, customer, network)
            hidden_dim: Hidden layer size
            learning_rate: Learning rate for optimizers
            gamma: Discount factor
            tau: Soft update parameter
            buffer_size: Replay buffer capacity
        """
        self.state_dim = state_dim
        self.action_dim = action_dim
        self.num_agents = num_agents
        
        # Device
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        logger.info(f"Using device: {self.device}")
        
        # Initialize components
        self.state_manager = StateManager(state_dim)
        self.decision_maker = DecisionMaker()
        
        self.network_manager = NetworkManager(
            state_dim=state_dim,
            action_dim=action_dim,
            num_agents=num_agents,
            hidden_dim=hidden_dim,
            learning_rate=learning_rate,
            tau=tau,
            device=self.device
        )
        
        self.replay_buffer = ReplayBuffer(buffer_size)
        
        self.trainer = MADDPGTrainer(
            network_manager=self.network_manager,
            replay_buffer=self.replay_buffer,
            gamma=gamma
        )
        
        # Training statistics
        self.episode_count = 0
        
        logger.info("MADDPG Coordinator initialized")
        logger.info(f"  - Components: StateManager, NetworkManager, DecisionMaker, Trainer")
        logger.info(f"  - Agents: {num_agents} (transaction, customer, network)")
    
    def select_actions(
        self, 
        state: np.ndarray, 
        explore: bool = True,
        epsilon: float = 0.1
    ) -> Dict[str, int]:
        """
        Select actions for all agents
        
        Args:
            state: State array [state_dim]
            explore: Whether to explore (training) or exploit (inference)
            epsilon: Exploration rate
        
        Returns:
            Dict of {agent_name: action} where action is 0 (BLOCK) or 1 (ALLOW)
        """
        state_tensor = torch.FloatTensor(state).to(self.device)
        return self.network_manager.select_actions(state_tensor, explore, epsilon)
    
    def decide(self, observations: Dict[str, Dict]) -> Dict:
        """
        Make coordinated decision based on detection agent observations
        
        Args:
            observations: Dict with keys 'transaction', 'customer', 'network'
                         Each contains: {probability, risk_score, ...}
        
        Returns:
            Decision dict with action, confidence, q_value, contributions
        
        Example:
            >>> observations = {
            ...     'transaction': {'probability': 0.95, 'risk_score': 87.5},
            ...     'customer': {'probability': 0.82, 'risk_score': 72.0},
            ...     'network': {'probability': 0.78, 'risk_score': 65.3}
            ... }
            >>> decision = coordinator.decide(observations)
            >>> decision['action']  # 'BLOCK' or 'ALLOW'
            >>> decision['confidence']  # 0.0 to 1.0
        """
        # Convert observations to state vector
        state = self.state_manager.observations_to_state(observations)
        state_tensor = torch.FloatTensor(state).unsqueeze(0).to(self.device)
        
        # Get action probabilities from each actor
        with torch.no_grad():
            action_probs = self.network_manager.get_action_probs(state_tensor)
            
            # Evaluate with critic
            q_value = self.network_manager.critic.evaluate(state_tensor, action_probs)
            
            # Make decision
            decision = self.decision_maker.make_decision(action_probs, q_value)
        
        return decision
    
    def update(self, batch_size: int = 64) -> Optional[Dict[str, float]]:
        """
        Update actors and critic using experience replay
        
        Args:
            batch_size: Batch size for training
        
        Returns:
            Dict of losses or None if not enough samples
        """
        return self.trainer.update(batch_size)
    
    def save_models(self, path: Optional[str] = None):
        """
        Save all models to disk
        
        Args:
            path: Directory path to save models (default: from config)
        """
        if path is None:
            path = settings.model_path
        
        self.network_manager.save_models(Path(path))
    
    def load_models(self, path: Optional[str] = None):
        """
        Load all models from disk
        
        Args:
            path: Directory path to load models from (default: from config)
        """
        if path is None:
            path = settings.model_path
        
        self.network_manager.load_models(Path(path))


# Global instance
maddpg_coordinator = MADDPGCoordinator(
    state_dim=settings.state_dim,
    action_dim=settings.action_dim,
    num_agents=3,
    hidden_dim=settings.hidden_dim,
    learning_rate=settings.learning_rate,
    gamma=settings.gamma,
    tau=settings.tau,
    buffer_size=settings.buffer_size
)
