"""
State Manager - Converts agent observations to state vectors

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import numpy as np
from typing import Dict

from ..constants import (
    AGENT_NAMES,
    AGENT_TRANSACTION,
    AGENT_CUSTOMER,
    AGENT_NETWORK,
    OBS_KEY_PROBABILITY,
    OBS_KEY_RISK_SCORE,
    SCORE_NORMALIZATION_FACTOR
)


class StateManager:
    """
    Manages state representation for MADDPG
    
    Converts detection agent observations into normalized state vectors
    """
    
    def __init__(self, state_dim: int = 6):
        """
        Initialize State Manager
        
        Args:
            state_dim: Dimension of state vector (default: 6)
        """
        self.state_dim = state_dim
    
    def observations_to_state(self, observations: Dict[str, Dict]) -> np.ndarray:
        """
        Convert detection agent observations to state vector
        
        State Vector Format:
        [txn_prob, txn_score/100, cust_prob, cust_score/100, net_prob, net_score/100]
        
        Args:
            observations: Dict with 'transaction', 'customer', 'network' observations
                         Each contains: {probability: float, risk_score: float, ...}
        
        Returns:
            Normalized state vector [state_dim]
        
        Example:
            >>> observations = {
            ...     'transaction': {'probability': 0.95, 'risk_score': 87.5},
            ...     'customer': {'probability': 0.82, 'risk_score': 72.0},
            ...     'network': {'probability': 0.78, 'risk_score': 65.3}
            ... }
            >>> state = state_manager.observations_to_state(observations)
            >>> state
            array([0.95, 0.875, 0.82, 0.72, 0.78, 0.653])
        """
        state = np.array([
            observations[AGENT_TRANSACTION][OBS_KEY_PROBABILITY],
            observations[AGENT_TRANSACTION][OBS_KEY_RISK_SCORE] / SCORE_NORMALIZATION_FACTOR,
            observations[AGENT_CUSTOMER][OBS_KEY_PROBABILITY],
            observations[AGENT_CUSTOMER][OBS_KEY_RISK_SCORE] / SCORE_NORMALIZATION_FACTOR,
            observations[AGENT_NETWORK][OBS_KEY_PROBABILITY],
            observations[AGENT_NETWORK][OBS_KEY_RISK_SCORE] / SCORE_NORMALIZATION_FACTOR
        ], dtype=np.float32)
        
        return state
    
    def validate_observations(self, observations: Dict[str, Dict]) -> bool:
        """
        Validate that observations have required format
        
        Args:
            observations: Agent observations dict
        
        Returns:
            True if valid, False otherwise
        """
        required_fields = [OBS_KEY_PROBABILITY, OBS_KEY_RISK_SCORE]
        
        # Check all agents present
        if not all(agent in observations for agent in AGENT_NAMES):
            return False
        
        # Check all fields present
        for agent in AGENT_NAMES:
            if not all(field in observations[agent] for field in required_fields):
                return False
            
            # Check value ranges
            if not (0 <= observations[agent][OBS_KEY_PROBABILITY] <= 1):
                return False
            if not (0 <= observations[agent][OBS_KEY_RISK_SCORE] <= SCORE_NORMALIZATION_FACTOR):
                return False
        
        return True
