"""
Model Persistence - Handles saving and loading of MADDPG models

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import torch
from pathlib import Path
from typing import Dict

from ..logger import logger


class ModelPersistence:
    """
    Handles model persistence (save/load) for MADDPG networks
    
    Manages file I/O for actor and critic models
    """
    
    def __init__(self, agent_names: list, device: torch.device):
        """
        Initialize Model Persistence
        
        Args:
            agent_names: List of agent names (e.g., ['transaction', 'customer', 'network'])
            device: Torch device for loading models
        """
        self.agent_names = agent_names
        self.device = device
    
    def save_actors(self, actors: Dict, path: Path):
        """
        Save all actor models
        
        Args:
            actors: Dict of {agent_name: actor_model}
            path: Directory to save models
        """
        path.mkdir(parents=True, exist_ok=True)
        
        for name in self.agent_names:
            actor_path = path / f"actor_{name}.pth"
            torch.save(actors[name].state_dict(), actor_path)
            logger.info(f"Saved actor: {name} -> {actor_path}")
    
    def save_critic(self, critic, path: Path):
        """
        Save critic model
        
        Args:
            critic: Critic model
            path: Directory to save model
        """
        path.mkdir(parents=True, exist_ok=True)
        
        critic_path = path / "critic.pth"
        torch.save(critic.state_dict(), critic_path)
        logger.info(f"Saved critic -> {critic_path}")
    
    def load_actors(self, actors: Dict, actor_targets: Dict, path: Path):
        """
        Load all actor models
        
        Args:
            actors: Dict of {agent_name: actor_model}
            actor_targets: Dict of {agent_name: actor_target_model}
            path: Directory to load models from
        """
        for name in self.agent_names:
            actor_path = path / f"actor_{name}.pth"
            
            if actor_path.exists():
                state_dict = torch.load(actor_path, map_location=self.device)
                actors[name].load_state_dict(state_dict)
                actor_targets[name].load_state_dict(state_dict)
                
                # Set to evaluation mode
                actors[name].eval()
                actor_targets[name].eval()
                
                logger.info(f"Loaded actor: {name} <- {actor_path}")
            else:
                logger.warning(f"Actor model not found: {actor_path}")
    
    def load_critic(self, critic, critic_target, path: Path):
        """
        Load critic model
        
        Args:
            critic: Critic model
            critic_target: Critic target model
            path: Directory to load model from
        """
        critic_path = path / "critic.pth"
        
        if critic_path.exists():
            state_dict = torch.load(critic_path, map_location=self.device)
            critic.load_state_dict(state_dict)
            critic_target.load_state_dict(state_dict)
            
            # Set to evaluation mode
            critic.eval()
            critic_target.eval()
            
            logger.info(f"Loaded critic <- {critic_path}")
        else:
            logger.warning(f"Critic model not found: {critic_path}")
