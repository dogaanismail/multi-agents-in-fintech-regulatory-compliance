"""
MADDPG Agent - Multi-Agent Deep Deterministic Policy Gradient
Coordinates 3 detection agents for AML compliance

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import torch
import torch.nn as nn
import torch.optim as optim
import numpy as np
from pathlib import Path
from typing import Dict, List, Tuple, Optional
import copy

from .actor import Actor
from .critic import Critic
from .replay_buffer import ReplayBuffer
from ..core.config import settings
from ..core.logging import logger


class MADDPGCoordinator:
    """
    MADDPG Coordinator for multi-agent AML detection
    
    Manages 3 Actor networks (one per detection agent) and 1 Centralized Critic
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
        self.gamma = gamma
        self.tau = tau
        
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        logger.info(f"Using device: {self.device}")
        
        # Agent names
        self.agent_names = ["transaction", "customer", "network"]
        
        # Create Actor networks (one per agent)
        self.actors = {}
        self.actor_targets = {}
        self.actor_optimizers = {}
        
        for name in self.agent_names:
            actor = Actor(state_dim, action_dim, hidden_dim, name).to(self.device)
            actor_target = copy.deepcopy(actor)
            
            self.actors[name] = actor
            self.actor_targets[name] = actor_target
            self.actor_optimizers[name] = optim.Adam(actor.parameters(), lr=learning_rate)
        
        # Create Centralized Critic
        self.critic = Critic(state_dim, action_dim, num_agents, hidden_dim).to(self.device)
        self.critic_target = copy.deepcopy(self.critic)
        self.critic_optimizer = optim.Adam(self.critic.parameters(), lr=learning_rate)
        
        # Replay buffer
        self.replay_buffer = ReplayBuffer(buffer_size)
        
        # Training statistics
        self.training_step = 0
        self.episode_count = 0
        
        logger.info("MADDPG Coordinator initialized")
        logger.info(f"  - Actors: {len(self.actors)}")
        logger.info(f"  - State dim: {state_dim}")
        logger.info(f"  - Action dim: {action_dim}")
        logger.info(f"  - Hidden dim: {hidden_dim}")
    
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
        
        actions = {}
        for name in self.agent_names:
            action = self.actors[name].select_action(state_tensor, explore, epsilon)
            actions[name] = action.item()
        
        return actions
    
    def get_action_probs(self, state: torch.Tensor) -> List[torch.Tensor]:
        """
        Get action probabilities from all actors (for critic evaluation)
        
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
    
    def decide(self, observations: Dict[str, Dict]) -> Dict:
        """
        Make coordinated decision based on detection agent observations
        
        Args:
            observations: Dict with keys 'transaction', 'customer', 'network'
                         Each contains: {probability, risk_score, ...}
        
        Returns:
            Decision dict with action, confidence, q_value, contributions
        """
        # Convert observations to state vector
        state = self._observations_to_state(observations)
        state_tensor = torch.FloatTensor(state).unsqueeze(0).to(self.device)
        
        # Get action probabilities from each actor
        with torch.no_grad():
            action_probs = self.get_action_probs(state_tensor)
            
            # Evaluate with critic
            q_value = self.critic.evaluate(state_tensor, action_probs)
            
            # Make decision (weighted voting based on action probs)
            final_action_probs = torch.mean(torch.stack(action_probs), dim=0)
            action = torch.argmax(final_action_probs, dim=-1).item()
            confidence = final_action_probs[0][action].item()
        
        # Calculate agent contributions
        contributions = {}
        for i, name in enumerate(self.agent_names):
            contributions[name] = action_probs[i][0][action].item()
        
        return {
            "action": "BLOCK" if action == 0 else "ALLOW",
            "confidence": float(confidence),
            "q_value": float(q_value.item()),
            "contributions": contributions
        }
    
    def _observations_to_state(self, observations: Dict[str, Dict]) -> np.ndarray:
        """
        Convert detection agent observations to state vector
        
        Args:
            observations: Dict with 'transaction', 'customer', 'network' observations
        
        Returns:
            State vector [6] = [txn_prob, txn_score/100, cust_prob, cust_score/100, net_prob, net_score/100]
        """
        state = np.array([
            observations['transaction']['probability'],
            observations['transaction']['risk_score'] / 100.0,
            observations['customer']['probability'],
            observations['customer']['risk_score'] / 100.0,
            observations['network']['probability'],
            observations['network']['risk_score'] / 100.0
        ], dtype=np.float32)
        
        return state
    
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
        
        states = states.to(self.device)
        rewards = rewards.to(self.device)
        next_states = next_states.to(self.device)
        dones = dones.to(self.device)
        actions = [a.to(self.device) for a in actions]
        
        # Update Critic
        with torch.no_grad():
            # Get next actions from target actors
            next_action_probs = []
            for name in self.agent_names:
                next_probs = self.actor_targets[name].get_action_probs(next_states)
                next_action_probs.append(next_probs)
            
            # Compute target Q-value
            target_q = self.critic_target.evaluate(next_states, next_action_probs)
            target_q = rewards + (1 - dones) * self.gamma * target_q
        
        # Current Q-value
        current_q = self.critic.evaluate(states, actions)
        
        # Critic loss
        critic_loss = nn.MSELoss()(current_q, target_q)
        
        self.critic_optimizer.zero_grad()
        critic_loss.backward()
        torch.nn.utils.clip_grad_norm_(self.critic.parameters(), 1.0)
        self.critic_optimizer.step()
        
        # Update Actors
        actor_losses = {}
        for i, name in enumerate(self.agent_names):
            # Get action probs from current actor
            current_action_probs = []
            for j, agent_name in enumerate(self.agent_names):
                if j == i:
                    probs = self.actors[name].get_action_probs(states)
                else:
                    probs = actions[j]  # Use actual actions from other agents
                current_action_probs.append(probs)
            
            # Actor loss = -Q(s, a)
            q = self.critic.evaluate(states, current_action_probs)
            actor_loss = -q.mean()
            
            self.actor_optimizers[name].zero_grad()
            actor_loss.backward()
            torch.nn.utils.clip_grad_norm_(self.actors[name].parameters(), 1.0)
            self.actor_optimizers[name].step()
            
            actor_losses[name] = actor_loss.item()
        
        # Soft update target networks
        self._soft_update()
        
        self.training_step += 1
        
        return {
            "critic_loss": critic_loss.item(),
            **{f"actor_loss_{name}": loss for name, loss in actor_losses.items()}
        }
    
    def _soft_update(self):
        """Soft update target networks"""
        # Update critic target
        for param, target_param in zip(self.critic.parameters(), self.critic_target.parameters()):
            target_param.data.copy_(self.tau * param.data + (1 - self.tau) * target_param.data)
        
        # Update actor targets
        for name in self.agent_names:
            for param, target_param in zip(
                self.actors[name].parameters(), 
                self.actor_targets[name].parameters()
            ):
                target_param.data.copy_(self.tau * param.data + (1 - self.tau) * target_param.data)
    
    def save_models(self, path: Optional[str] = None):
        """Save all models"""
        if path is None:
            path = settings.model_path
        
        path = Path(path)
        path.mkdir(parents=True, exist_ok=True)
        
        # Save actors
        for name in self.agent_names:
            torch.save(
                self.actors[name].state_dict(),
                path / f"actor_{name}.pth"
            )
        
        # Save critic
        torch.save(self.critic.state_dict(), path / "critic.pth")
        
        logger.info(f"Models saved to {path}")
    
    def load_models(self, path: Optional[str] = None):
        """Load all models"""
        if path is None:
            path = settings.model_path
        
        path = Path(path)
        
        # Load actors
        for name in self.agent_names:
            actor_path = path / f"actor_{name}.pth"
            if actor_path.exists():
                self.actors[name].load_state_dict(torch.load(actor_path, map_location=self.device))
                self.actor_targets[name].load_state_dict(torch.load(actor_path, map_location=self.device))
                logger.info(f"Loaded actor: {name}")
            else:
                logger.warning(f"Actor model not found: {actor_path}")
        
        # Load critic
        critic_path = path / "critic.pth"
        if critic_path.exists():
            self.critic.load_state_dict(torch.load(critic_path, map_location=self.device))
            self.critic_target.load_state_dict(torch.load(critic_path, map_location=self.device))
            logger.info("Loaded critic")
        else:
            logger.warning(f"Critic model not found: {critic_path}")
        
        # Set to evaluation mode
        for name in self.agent_names:
            self.actors[name].eval()
            self.actor_targets[name].eval()
        self.critic.eval()
        self.critic_target.eval()


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
