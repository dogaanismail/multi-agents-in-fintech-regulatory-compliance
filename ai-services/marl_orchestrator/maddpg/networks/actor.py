"""
Actor Network for MADDPG
Each agent has its own Actor network that learns to map observations to actions

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import torch
import torch.nn as nn
import torch.nn.functional as F

class Actor(nn.Module):
    """
    Actor network for MADDPG
    Maps state to action probabilities
    
    Architecture:
    - Input: State vector (6 features from 3 agents)
    - Hidden layers: Configurable size (default 256)
    - Output: Action logits (2 actions: BLOCK, ALLOW)
    """
    
    def __init__(
        self,
        state_dim: int,
        action_dim: int,
        hidden_dim: int = 256,
        agent_name: str = "actor"
    ):
        """
        Initialize Actor network
        
        Args:
            state_dim: Dimension of state space (6 for 3 agents × 2 features)
            action_dim: Dimension of action space (2: BLOCK, ALLOW)
            hidden_dim: Size of hidden layers
            agent_name: Name of this actor (transaction, customer, network)
        """
        super(Actor, self).__init__()
        
        self.agent_name = agent_name
        self.state_dim = state_dim
        self.action_dim = action_dim
        
        # Network architecture
        self.fc1 = nn.Linear(state_dim, hidden_dim)
        self.bn1 = nn.BatchNorm1d(hidden_dim)
        
        self.fc2 = nn.Linear(hidden_dim, hidden_dim)
        self.bn2 = nn.BatchNorm1d(hidden_dim)
        
        self.fc3 = nn.Linear(hidden_dim, hidden_dim // 2)
        self.bn3 = nn.BatchNorm1d(hidden_dim // 2)
        
        self.fc4 = nn.Linear(hidden_dim // 2, action_dim)
        
        # Initialize weights
        self._init_weights()
    
    def _init_weights(self):
        """Initialize network weights using Xavier initialization"""
        for m in self.modules():
            if isinstance(m, nn.Linear):
                nn.init.xavier_uniform_(m.weight)
                nn.init.constant_(m.bias, 0.0)
    
    def forward(self, state: torch.Tensor) -> torch.Tensor:
        """
        Forward pass
        
        Args:
            state: State tensor [batch_size, state_dim]
        
        Returns:
            action_probs: Action probabilities [batch_size, action_dim]
        """
        # Layer 1
        x = self.fc1(state)
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
        
        # Output layer with softmax
        x = self.fc4(x)
        action_probs = F.softmax(x, dim=-1)
        
        return action_probs
    
    def select_action(self, state: torch.Tensor, explore: bool = True, epsilon: float = 0.1) -> torch.Tensor:
        """
        Select action given state
        
        Args:
            state: State tensor [state_dim] or [batch_size, state_dim]
            explore: Whether to add exploration noise
            epsilon: Exploration rate for epsilon-greedy
        
        Returns:
            action: Selected action (0 or 1)
        """
        # Ensure batch dimension
        if state.dim() == 1:
            state = state.unsqueeze(0)
        
        with torch.no_grad():
            action_probs = self.forward(state)
        
        if explore and torch.rand(1).item() < epsilon:
            # Random action (exploration)
            action = torch.randint(0, self.action_dim, (state.size(0),))
        else:
            # Greedy action (exploitation)
            action = torch.argmax(action_probs, dim=-1)
        
        return action
    
    def get_action_probs(self, state: torch.Tensor) -> torch.Tensor:
        """
        Get action probabilities (for training)
        
        Args:
            state: State tensor [batch_size, state_dim]
        
        Returns:
            action_probs: Action probabilities [batch_size, action_dim]
        """
        return self.forward(state)
