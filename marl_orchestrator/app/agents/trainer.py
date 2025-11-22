"""
Trainer - Handles MADDPG training logic

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import torch
import torch.nn as nn
from typing import Optional, Dict, List

from .network_manager import NetworkManager
from .replay_buffer import ReplayBuffer
from ..core.logging import logger


class MADDPGTrainer:
    """
    Handles training logic for MADDPG
    
    Updates actors and critic using experience replay
    """
    
    def __init__(
        self,
        network_manager: NetworkManager,
        replay_buffer: ReplayBuffer,
        gamma: float = 0.99
    ):
        """
        Initialize Trainer
        
        Args:
            network_manager: NetworkManager instance
            replay_buffer: ReplayBuffer instance
            gamma: Discount factor
        """
        self.network_manager = network_manager
        self.replay_buffer = replay_buffer
        self.gamma = gamma
        self.training_step = 0
    
    def update(self, batch_size: int = 64) -> Optional[Dict[str, float]]:
        """
        Update actors and critic using experience replay
        
        Args:
            batch_size: Batch size for training
        
        Returns:
            Dict of losses or None if not enough samples
        """
        if len(self.replay_buffer) < batch_size:
            return None
        
        # Sample batch
        states, actions, rewards, next_states, dones = self.replay_buffer.sample(batch_size)
        
        device = self.network_manager.device
        states = states.to(device)
        rewards = rewards.to(device)
        next_states = next_states.to(device)
        dones = dones.to(device)
        actions = [a.to(device) for a in actions]
        
        # Update Critic
        critic_loss = self._update_critic(states, actions, rewards, next_states, dones)
        
        # Update Actors
        actor_losses = self._update_actors(states, actions)
        
        # Soft update target networks
        self.network_manager.soft_update()
        
        self.training_step += 1
        
        return {
            "critic_loss": critic_loss,
            **{f"actor_loss_{name}": loss for name, loss in actor_losses.items()}
        }
    
    def _update_critic(
        self,
        states: torch.Tensor,
        actions: List[torch.Tensor],
        rewards: torch.Tensor,
        next_states: torch.Tensor,
        dones: torch.Tensor
    ) -> float:
        """
        Update centralized critic
        
        Args:
            states: Current states [batch_size, state_dim]
            actions: List of action tensors from each agent
            rewards: Rewards [batch_size, 1]
            next_states: Next states [batch_size, state_dim]
            dones: Done flags [batch_size, 1]
        
        Returns:
            Critic loss value
        """
        with torch.no_grad():
            # Get next actions from target actors
            next_action_probs = []
            for name in self.network_manager.agent_names:
                next_probs = self.network_manager.actor_targets[name].get_action_probs(next_states)
                next_action_probs.append(next_probs)
            
            # Compute target Q-value
            target_q = self.network_manager.critic_target.evaluate(next_states, next_action_probs)
            target_q = rewards + (1 - dones) * self.gamma * target_q
        
        # Current Q-value
        current_q = self.network_manager.critic.evaluate(states, actions)
        
        # Critic loss (MSE)
        critic_loss = nn.MSELoss()(current_q, target_q)
        
        # Backpropagation
        self.network_manager.critic_optimizer.zero_grad()
        critic_loss.backward()
        torch.nn.utils.clip_grad_norm_(self.network_manager.critic.parameters(), 1.0)
        self.network_manager.critic_optimizer.step()
        
        return critic_loss.item()
    
    def _update_actors(
        self,
        states: torch.Tensor,
        actions: List[torch.Tensor]
    ) -> Dict[str, float]:
        """
        Update all actor networks
        
        Args:
            states: Current states [batch_size, state_dim]
            actions: List of action tensors from each agent
        
        Returns:
            Dict of {agent_name: loss_value}
        """
        actor_losses = {}
        
        for i, name in enumerate(self.network_manager.agent_names):
            # Get action probs from current actor
            current_action_probs = []
            for j, agent_name in enumerate(self.network_manager.agent_names):
                if j == i:
                    # Use current actor's output
                    probs = self.network_manager.actors[name].get_action_probs(states)
                else:
                    # Use actual actions from other agents
                    probs = actions[j]
                current_action_probs.append(probs)
            
            # Actor loss = -Q(s, a)
            # Maximize Q-value by minimizing negative Q-value
            q = self.network_manager.critic.evaluate(states, current_action_probs)
            actor_loss = -q.mean()
            
            # Backpropagation
            self.network_manager.actor_optimizers[name].zero_grad()
            actor_loss.backward()
            torch.nn.utils.clip_grad_norm_(
                self.network_manager.actors[name].parameters(), 1.0
            )
            self.network_manager.actor_optimizers[name].step()
            
            actor_losses[name] = actor_loss.item()
        
        return actor_losses
