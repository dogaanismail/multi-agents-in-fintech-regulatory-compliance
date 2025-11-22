"""
Network Manager - Manages actor and critic networks

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import torch
import torch.optim as optim
import copy
from pathlib import Path
from typing import Dict, List

from .actor import Actor
from .critic import Critic
from .model_persistence import ModelPersistence
from ..core.logging import logger
from ..core.constants import AGENT_NAMES


class NetworkManager:
    """
    Manages neural networks for MADDPG
    
    Handles creation, updates, and persistence of actors and critic
    """
    
    def __init__(
        self,
        state_dim: int,
        action_dim: int,
        num_agents: int,
        hidden_dim: int,
        learning_rate: float,
        tau: float,
        device: torch.device
    ):
        """
        Initialize Network Manager
        
        Args:
            state_dim: State dimension
            action_dim: Action dimension
            num_agents: Number of agents
            hidden_dim: Hidden layer size
            learning_rate: Learning rate for optimizers
            tau: Soft update parameter
            device: torch device (cpu or cuda)
        """
        self.state_dim = state_dim
        self.action_dim = action_dim
        self.num_agents = num_agents
        self.tau = tau
        self.device = device
        
        # Agent names
        self.agent_names = AGENT_NAMES
        
        # Create Actor networks (one per agent)
        self.actors = {}
        self.actor_targets = {}
        self.actor_optimizers = {}
        
        for name in self.agent_names:
            actor = Actor(state_dim, action_dim, hidden_dim, name).to(device)
            actor_target = copy.deepcopy(actor)
            
            self.actors[name] = actor
            self.actor_targets[name] = actor_target
            self.actor_optimizers[name] = optim.Adam(actor.parameters(), lr=learning_rate)
        
        # Create Centralized Critic
        self.critic = Critic(state_dim, action_dim, num_agents, hidden_dim).to(device)
        self.critic_target = copy.deepcopy(self.critic)
        self.critic_optimizer = optim.Adam(self.critic.parameters(), lr=learning_rate)
        
        # Model persistence handler
        self.model_persistence = ModelPersistence(self.agent_names, device)
        
        logger.info("Networks initialized")
        logger.info(f"  - Actors: {len(self.actors)}")
        logger.info(f"  - State dim: {state_dim}, Action dim: {action_dim}")
        logger.info(f"  - Hidden dim: {hidden_dim}, Device: {device}")
    
    def select_actions(
        self,
        state: torch.Tensor,
        explore: bool = True,
        epsilon: float = 0.1
    ) -> Dict[str, int]:
        """
        Select actions from all actors
        
        Args:
            state: State tensor [state_dim]
            explore: Whether to explore (training) or exploit (inference)
            epsilon: Exploration rate
        
        Returns:
            Dict of {agent_name: action} where action is 0 (BLOCK) or 1 (ALLOW)
        """
        actions = {}
        for name in self.agent_names:
            action = self.actors[name].select_action(state, explore, epsilon)
            actions[name] = action.item()
        
        return actions
    
    def get_action_probs(self, state: torch.Tensor) -> List[torch.Tensor]:
        """
        Get action probabilities from all actors
        
        Args:
            state: State tensor [batch_size, state_dim]
        
        Returns:
            List of action probability tensors [batch_size, action_dim]
        """
        action_probs = []
        for name in self.agent_names:
            probs = self.actors[name].get_action_probs(state)
            action_probs.append(probs)
        
        return action_probs
    
    def soft_update(self):
        """Soft update target networks"""
        # Update critic target
        for param, target_param in zip(
            self.critic.parameters(), 
            self.critic_target.parameters()
        ):
            target_param.data.copy_(
                self.tau * param.data + (1 - self.tau) * target_param.data
            )
        
        # Update actor targets
        for name in self.agent_names:
            for param, target_param in zip(
                self.actors[name].parameters(),
                self.actor_targets[name].parameters()
            ):
                target_param.data.copy_(
                    self.tau * param.data + (1 - self.tau) * target_param.data
                )
    
    def save_models(self, path: Path):
        """
        Save all models to disk
        
        Args:
            path: Directory to save models
        """
        self.model_persistence.save_actors(self.actors, path)
        self.model_persistence.save_critic(self.critic, path)
        logger.info(f"✅ All models saved to {path}")
    
    def load_models(self, path: Path):
        """
        Load all models from disk
        
        Args:
            path: Directory to load models from
        """
        self.model_persistence.load_actors(self.actors, self.actor_targets, path)
        self.model_persistence.load_critic(self.critic, self.critic_target, path)
        logger.info(f"✅ All models loaded from {path}")
