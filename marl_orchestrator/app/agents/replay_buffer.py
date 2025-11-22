"""
Experience Replay Buffer for MADDPG

Author: Ismail Dogan
"""

import numpy as np
import torch
from collections import deque
from typing import Tuple, List
import random


class ReplayBuffer:
    """
    Experience replay buffer for MADDPG
    Stores transitions: (state, actions, reward, next_state, done)
    """
    
    def __init__(self, capacity: int = 100000):
        """
        Initialize replay buffer
        
        Args:
            capacity: Maximum number of transitions to store
        """
        self.capacity = capacity
        self.buffer = deque(maxlen=capacity)
    
    def push(
        self,
        state: np.ndarray,
        actions: List[np.ndarray],
        reward: float,
        next_state: np.ndarray,
        done: bool
    ):
        """
        Add transition to buffer
        
        Args:
            state: Current state [state_dim]
            actions: List of actions from all agents [num_agents, action_dim]
            reward: Reward received
            next_state: Next state [state_dim]
            done: Whether episode is done
        """
        self.buffer.append((state, actions, reward, next_state, done))
    
    def sample(self, batch_size: int) -> Tuple:
        """
        Sample random batch of transitions
        
        Args:
            batch_size: Number of transitions to sample
        
        Returns:
            Tuple of (states, actions, rewards, next_states, dones)
            All as torch tensors
        """
        batch = random.sample(self.buffer, batch_size)
        
        states, actions, rewards, next_states, dones = zip(*batch)
        
        # Convert to tensors
        states = torch.FloatTensor(np.array(states))
        rewards = torch.FloatTensor(rewards).unsqueeze(1)
        next_states = torch.FloatTensor(np.array(next_states))
        dones = torch.FloatTensor(dones).unsqueeze(1)
        
        # Actions: List of tensors (one per agent)
        num_agents = len(actions[0])
        actions_tensors = []
        for i in range(num_agents):
            agent_actions = torch.FloatTensor([a[i] for a in actions])
            actions_tensors.append(agent_actions)
        
        return states, actions_tensors, rewards, next_states, dones
    
    def __len__(self) -> int:
        """Return current size of buffer"""
        return len(self.buffer)
    
    def clear(self):
        """Clear the buffer"""
        self.buffer.clear()
