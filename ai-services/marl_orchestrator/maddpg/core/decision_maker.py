"""
Decision Maker - Coordinates final decision from multiple actors

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import torch
from typing import Dict, List

from ..logger import logger
from ..constants import AGENT_NAMES, ACTION_LABELS, ACTION_BLOCK, ACTION_ALLOW


class DecisionMaker:
    """
    Coordinates final decision from multiple actor networks
    
    Aggregates action probabilities and computes confidence scores
    """
    
    def __init__(self, agent_names: List[str] = None):
        """
        Initialize Decision Maker
        
        Args:
            agent_names: List of agent names (default: from constants.AGENT_NAMES)
        """
        self.agent_names = agent_names or AGENT_NAMES
    
    def make_decision(
        self,
        action_probs: List[torch.Tensor],
        q_value: torch.Tensor
    ) -> Dict:
        """
        Make coordinated decision based on actor outputs
        
        Strategy: Weighted voting based on action probabilities
        
        Args:
            action_probs: List of action probability tensors from each actor
                         Shape: [batch_size, action_dim] for each actor
            q_value: Q-value from centralized critic
        
        Returns:
            Decision dict with:
            - action: "BLOCK" or "ALLOW"
            - confidence: float in [0, 1]
            - q_value: float Q-value estimate
            - contributions: dict of {agent_name: contribution}
        
        Example:
            >>> action_probs = [
            ...     torch.tensor([[0.8, 0.2]]),  # transaction: 80% BLOCK
            ...     torch.tensor([[0.7, 0.3]]),  # customer: 70% BLOCK
            ...     torch.tensor([[0.6, 0.4]])   # network: 60% BLOCK
            ... ]
            >>> q_value = torch.tensor([0.85])
            >>> decision = decision_maker.make_decision(action_probs, q_value)
            >>> decision['action']
            'BLOCK'
            >>> decision['confidence']
            0.7  # average of [0.8, 0.7, 0.6]
        """
        # Average action probabilities across all actors
        final_action_probs = torch.mean(torch.stack(action_probs), dim=0)
        
        # Select action with highest probability
        action = torch.argmax(final_action_probs, dim=-1).item()
        
        # Confidence = probability of selected action
        confidence = final_action_probs[0][action].item()
        
        # Calculate individual agent contributions
        contributions = {}
        for i, name in enumerate(self.agent_names):
            contributions[name] = action_probs[i][0][action].item()
        
        decision = {
            "action": ACTION_LABELS[action],
            "confidence": float(confidence),
            "q_value": float(q_value.item()),
            "contributions": contributions
        }
        
        logger.debug(f"Decision: {decision['action']} (confidence: {confidence:.2%})")
        
        return decision
    
    def explain_decision(self, decision: Dict) -> str:
        """
        Generate human-readable explanation of decision
        
        Args:
            decision: Decision dict from make_decision()
        
        Returns:
            Explanation string
        
        Example:
            >>> explanation = decision_maker.explain_decision(decision)
            >>> print(explanation)
            Decision: BLOCK
            Confidence: 70.0%
            Q-Value: 0.85
            Agent Contributions:
              - transaction: 80.0%
              - customer: 70.0%
              - network: 60.0%
        """
        lines = [
            f"Decision: {decision['action']}",
            f"Confidence: {decision['confidence']:.1%}",
            f"Q-Value: {decision['q_value']:.2f}",
            "Agent Contributions:"
        ]
        
        for agent, contrib in decision['contributions'].items():
            lines.append(f"  - {agent}: {contrib:.1%}")
        
        return "\n".join(lines)
