"""
Centralized Critic Network for MADDPG
Evaluates the joint actions of all agents

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import torch
import torch.nn as nn
import torch.nn.functional as F
from typing import List


class Critic(nn.Module):
    """
    Centralized Critic network for MADDPG
    Evaluates Q-value of joint state-action pairs
    
    Architecture:
    - Input: Global state + all agents' actions
    - Hidden layers: Configurable size (default 256)
    - Output: Q-value (single scalar)
    
    Key insight: Critic has access to ALL agents' observations and actions
    during training, enabling coordinated learning
    """
    
    def __init__(
        self,
        state_dim: int,
        action_dim: int,
        num_agents: int = 3,
        hidden_dim: int = 256
    ):
        """
        Initialize Centralized Critic
        
        Args:
            state_dim: Dimension of state space (6 for 3 agents × 2 features)
            action_dim: Dimension of action space per agent (2: BLOCK, ALLOW)
            num_agents: Number of agents (3: transaction, customer, network)
            hidden_dim: Size of hidden layers
        """
        super(Critic, self).__init__()
        
        self.num_agents = num_agents
        self.state_dim = state_dim
        self.action_dim = action_dim
        
        # Input: state + all actions
        input_dim = state_dim + (action_dim * num_agents)
        
        # Network architecture
        self.fc1 = nn.Linear(input_dim, hidden_dim)
        self.bn1 = nn.BatchNorm1d(hidden_dim)
        
        self.fc2 = nn.Linear(hidden_dim, hidden_dim)
        self.bn2 = nn.BatchNorm1d(hidden_dim)
        
        self.fc3 = nn.Linear(hidden_dim, hidden_dim // 2)
        self.bn3 = nn.BatchNorm1d(hidden_dim // 2)
        
        self.fc4 = nn.Linear(hidden_dim // 2, 1)  # Output: Q-value
        
        # Initialize weights
        self._init_weights()
    
    def _init_weights(self):
        """Initialize network weights using Xavier initialization"""
        for m in self.modules():
            if isinstance(m, nn.Linear):
                nn.init.xavier_uniform_(m.weight)
                nn.init.constant_(m.bias, 0.0)
    
    def forward(self, state: torch.Tensor, actions: List[torch.Tensor]) -> torch.Tensor:
        """
        Forward pass
        
        Args:
            state: Global state tensor [batch_size, state_dim]
            actions: List of action tensors from all agents
                     Each: [batch_size, action_dim]
        
        Returns:
            q_value: Estimated Q-value [batch_size, 1]
        """
        # Concatenate state and all actions
        actions_cat = torch.cat(actions, dim=-1)  # [batch_size, num_agents * action_dim]
        x = torch.cat([state, actions_cat], dim=-1)  # [batch_size, input_dim]
        
        # Layer 1
        x = self.fc1(x)
        if x.size(0) > 1:  # Only use batch norm if batch size > 1
            x = self.bn1(x)
        x = F.relu(x)
        
        # Layer 2
        x = self.fc2(x)
        if x.size(0) > 1:
            x = self.bn2(x)
        x = F.relu(x)
        
        # Layer 3
        x = self.fc3(x)
        if x.size(0) > 1:
            x = self.bn3(x)
        x = F.relu(x)
        
        # Output layer (no activation, Q-value can be any real number)
        q_value = self.fc4(x)
        
        return q_value
    
    def evaluate(self, state: torch.Tensor, actions: List[torch.Tensor]) -> torch.Tensor:
        """
        Evaluate Q-value for given state-action pair
        
        Args:
            state: State tensor [batch_size, state_dim]
            actions: List of action probabilities from all agents
        
        Returns:
            q_value: Q-value [batch_size, 1]
        """
        return self.forward(state, actions)
