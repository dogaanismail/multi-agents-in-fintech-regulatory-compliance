# Understanding MADDPG Implementation

## 📚 Table of Contents
1. [What is MADDPG?](#what-is-maddpg)
2. [Architecture Overview](#architecture-overview)
3. [Component-by-Component Breakdown](#component-breakdown)
4. [Step-by-Step Execution Flow](#execution-flow)
5. [Testing & Verification](#testing)
6. [Debugging Guide](#debugging)

---

## What is MADDPG?

**MADDPG** = Multi-Agent Deep Deterministic Policy Gradient

### Core Concept
- **Problem**: You have 3 detection agents (transaction, customer, network) making independent decisions
- **Challenge**: How to coordinate them for better overall decision?
- **Solution**: Train a multi-agent system that learns to coordinate their outputs

### Key Innovation
- Each agent has its own **Actor** (policy) - makes decisions
- One shared **Critic** (evaluator) - evaluates joint actions
- Learns through trial and error (reinforcement learning)

### Why for AML?
```
Transaction Agent: "30% fraud probability"
Customer Agent:    "60% risk probability"  
Network Agent:     "90% suspicious probability"

MADDPG decides: Should we BLOCK or ALLOW?
→ Learns optimal coordination strategy
```

---

## Architecture Overview

### High-Level Structure
```
┌─────────────────────────────────────────────────┐
│         MADDPG Coordinator (maddpg/)            │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────┐  ┌──────────────┐           │
│  │ 3 Actors     │  │ 1 Critic     │           │
│  │ (Policies)   │  │ (Evaluator)  │           │
│  └──────────────┘  └──────────────┘           │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │ Components:                              │  │
│  │ • StateManager    - Observation → State │  │
│  │ • DecisionMaker   - Actions → Decision  │  │
│  │ • NetworkManager  - Neural Networks     │  │
│  │ • Trainer         - Learning Logic      │  │
│  │ • ModelPersistence - Save/Load Models   │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### Data Flow
```
Agent Observations → State Vector → Actors → Actions → Decision
     ↓                                                    ↓
[prob, score]      [6 values]    [3×2]    [BLOCK/ALLOW]
```

---

## Component Breakdown

### 1. **State Manager** (`maddpg/core/state_manager.py`)

**Purpose**: Convert agent observations to neural network input

**Input** (from 3 agents):
```python
{
  'transaction': {'probability': 0.3, 'risk_score': 25},
  'customer':    {'probability': 0.6, 'risk_score': 60},
  'network':     {'probability': 0.9, 'risk_score': 85}
}
```

**Output** (state vector):
```python
[0.3, 0.25, 0.6, 0.60, 0.9, 0.85]
 ↑    ↑     ↑    ↑     ↑    ↑
 txn  txn   cust cust  net  net
 prob score prob score prob score
```

**Key Function**:
```python
def build_state(observations: Dict) -> np.ndarray:
    # Extract features in fixed order
    # Normalize scores (divide by 100)
    # Return 6-dimensional vector
```

**Why it matters**: Neural networks need fixed-size numeric input

---

### 2. **Actor Networks** (`maddpg/networks/actor.py`)

**Purpose**: Learn policy for each agent (what action to take)

**Architecture**:
```
Input (6) → FC(256) → ReLU → FC(256) → ReLU → FC(2) → Softmax → Output
State         Hidden Layer 1    Hidden Layer 2    Actions   Probabilities
```

**Input**: State vector [6 values]
**Output**: Action probabilities [P(BLOCK), P(ALLOW)]

**Example**:
```python
state = [0.3, 0.25, 0.6, 0.60, 0.9, 0.85]  # High risk!
actor_output = [0.85, 0.15]  # 85% BLOCK, 15% ALLOW
```

**3 Actors**:
- `actor_transaction`: Learns transaction agent's best action
- `actor_customer`: Learns customer agent's best action  
- `actor_network`: Learns network agent's best action

**Key Method**:
```python
def forward(state: torch.Tensor) -> torch.Tensor:
    x = F.relu(self.fc1(state))  # First hidden layer
    x = F.relu(self.fc2(x))      # Second hidden layer
    action_probs = F.softmax(self.fc3(x), dim=-1)  # Action probabilities
    return action_probs
```

---

### 3. **Critic Network** (`maddpg/networks/critic.py`)

**Purpose**: Evaluate how good the joint actions are

**Architecture**:
```
State (6) + All Actions (6) → FC(256) → ReLU → FC(256) → ReLU → FC(1) → Q-value
```

**Input**: 
- State: [6 values]
- Actions from all 3 actors: [2, 2, 2] = 6 values
- Total: 12 values

**Output**: Q-value (expected reward)
```python
Q = -5.2   # Bad joint action
Q = +8.7   # Good joint action
```

**Why centralized?**
- Sees ALL actors' actions (coordination)
- Evaluates joint performance
- Solves "credit assignment" problem

**Key Method**:
```python
def forward(state: torch.Tensor, actions: torch.Tensor) -> torch.Tensor:
    x = torch.cat([state, actions], dim=1)  # Concatenate inputs
    x = F.relu(self.fc1(x))   # Process
    x = F.relu(self.fc2(x))
    q_value = self.fc3(x)     # Output Q-value
    return q_value
```

---

### 4. **Decision Maker** (`maddpg/core/decision_maker.py`)

**Purpose**: Convert actor outputs to final decision

**Process**:
```python
# Step 1: Get actions from all 3 actors
actor_outputs = {
    'transaction': [0.6, 0.4],  # 60% BLOCK
    'customer':    [0.7, 0.3],  # 70% BLOCK
    'network':     [0.9, 0.1]   # 90% BLOCK
}

# Step 2: Average action probabilities
avg_probs = [0.733, 0.267]  # Average: 73.3% BLOCK

# Step 3: Pick action with highest probability
decision = 'BLOCK' if avg_probs[0] > avg_probs[1] else 'ALLOW'
confidence = 0.733

# Step 4: Calculate contributions
contributions = {
    'transaction': 0.6,  # How much it contributed to BLOCK
    'customer': 0.7,
    'network': 0.9
}
```

**Key Function**:
```python
def make_decision(actions: Dict) -> Dict:
    # Aggregate all actor outputs
    # Select final action
    # Compute confidence and contributions
```

---

### 5. **Network Manager** (`maddpg/core/network_manager.py`)

**Purpose**: Manage all neural networks and optimizers

**What it manages**:
```python
# Main networks (used for action selection)
- 3 Actors (transaction, customer, network)
- 1 Critic (shared)

# Target networks (used for stable training)
- 3 Target Actors
- 1 Target Critic

# Optimizers (for learning)
- 3 Actor optimizers
- 1 Critic optimizer
```

**Target Networks**: Slowly updated copies for stable learning
```python
# Soft update: target = 0.995 * target + 0.005 * main
# Prevents oscillation during training
```

**Key Methods**:
```python
def get_actions(state) -> Dict:
    # Forward pass through all actors
    
def update_targets():
    # Soft update target networks
```

---

### 6. **Trainer** (`maddpg/core/trainer.py`)

**Purpose**: Implement MADDPG learning algorithm

**Training Loop**:
```python
1. Sample batch from replay buffer
   ↓
2. Compute target Q-values
   Q_target = reward + γ * Q'(next_state, next_actions)
   ↓
3. Update Critic
   Loss = MSE(Q_current, Q_target)
   ↓
4. Update Actors
   Loss = -Q(state, actor_action)  # Maximize Q-value
   ↓
5. Soft update target networks
```

**Key Components**:
- **Replay Buffer**: Stores past experiences
- **Batch Learning**: Sample random mini-batches
- **Target Networks**: Stable learning targets
- **Policy Gradient**: Actor learns from critic feedback

**Key Method**:
```python
def train_step(experiences: Dict):
    # 1. Compute target Q-values (what should Q be?)
    # 2. Update critic (improve evaluation)
    # 3. Update actors (improve policies)
    # 4. Update target networks (stability)
```

---

### 7. **Replay Buffer** (`maddpg/networks/replay_buffer.py`)

**Purpose**: Store and sample past experiences

**Experience Tuple**:
```python
(state, actions, rewards, next_state, done)

Example:
state = [0.3, 0.25, 0.6, 0.60, 0.9, 0.85]
actions = [[0.8, 0.2], [0.7, 0.3], [0.9, 0.1]]
rewards = [1.0, 1.0, 1.0]  # Good decision
next_state = [0.2, 0.20, 0.5, 0.50, 0.8, 0.80]
done = False
```

**Why needed?**
- **Break correlation**: Sample random experiences
- **Reuse data**: Learn from same experience multiple times
- **Stabilize learning**: Smooth out training

---

## Execution Flow

### Mode 1: Inference (Decision Making)

```python
# 1. API receives request
request = {
    'transaction': {...},
    'customer': {...},
    'network': {...}
}

# 2. Get agent observations
observations = await agent_orchestrator.get_all_observations(...)

# 3. Build state vector
state = state_manager.build_state(observations)
# → [0.3, 0.25, 0.6, 0.60, 0.9, 0.85]

# 4. Get actions from actors
actions = network_manager.get_actions(state)
# → {'transaction': [0.6, 0.4], 'customer': [0.7, 0.3], 'network': [0.9, 0.1]}

# 5. Make final decision
decision = decision_maker.make_decision(actions)
# → {'action': 'BLOCK', 'confidence': 0.733, ...}

# 6. Return response
response = CoordinatedDecisionResponse(...)
```

### Mode 2: Training (Learning)

```python
# 1. Collect experience
for episode in range(num_episodes):
    # Get observations
    # Get current actions
    # Execute in environment
    # Get reward (1 if correct, 0 if wrong)
    # Store in replay buffer
    
    # 2. Learn from experiences
    if buffer has enough samples:
        batch = replay_buffer.sample(batch_size=64)
        trainer.train_step(batch)
        
# 3. Save trained models
model_persistence.save_models()
```

---

## Testing & Verification

### Test 1: Component Unit Tests

Create: `marl_orchestrator/test/test_components.py`

```python
import sys
sys.path.append('..')

import torch
import numpy as np
from maddpg.networks import Actor, Critic, ReplayBuffer
from maddpg.core import StateManager, DecisionMaker

def test_state_manager():
    """Test: Does state manager correctly build state vectors?"""
    print("\n=== Testing State Manager ===")
    
    observations = {
        'transaction': {'probability': 0.5, 'risk_score': 50},
        'customer': {'probability': 0.7, 'risk_score': 70},
        'network': {'probability': 0.9, 'risk_score': 90}
    }
    
    sm = StateManager()
    state = sm.build_state(observations)
    
    # Verify shape
    assert state.shape == (6,), f"Expected shape (6,), got {state.shape}"
    
    # Verify values
    expected = [0.5, 0.5, 0.7, 0.7, 0.9, 0.9]
    np.testing.assert_array_almost_equal(state, expected, decimal=2)
    
    print(f"✅ State shape: {state.shape}")
    print(f"✅ State values: {state}")
    print("✅ State Manager works correctly!")

def test_actor():
    """Test: Does actor produce valid action probabilities?"""
    print("\n=== Testing Actor ===")
    
    actor = Actor(state_dim=6, action_dim=2, hidden_dim=256)
    
    # Create dummy state
    state = torch.FloatTensor([0.3, 0.25, 0.6, 0.60, 0.9, 0.85])
    
    # Get action
    action_probs = actor(state)
    
    # Verify shape
    assert action_probs.shape == (2,), f"Expected shape (2,), got {action_probs.shape}"
    
    # Verify probabilities sum to 1
    prob_sum = action_probs.sum().item()
    assert abs(prob_sum - 1.0) < 0.01, f"Probabilities should sum to 1, got {prob_sum}"
    
    # Verify all positive
    assert (action_probs >= 0).all(), "All probabilities should be positive"
    
    print(f"✅ Action shape: {action_probs.shape}")
    print(f"✅ Action probs: {action_probs.detach().numpy()}")
    print(f"✅ Sum: {prob_sum:.4f}")
    print("✅ Actor works correctly!")

def test_critic():
    """Test: Does critic produce Q-values?"""
    print("\n=== Testing Critic ===")
    
    critic = Critic(state_dim=6, action_dim=2, num_agents=3, hidden_dim=256)
    
    # Create dummy inputs
    state = torch.FloatTensor([0.3, 0.25, 0.6, 0.60, 0.9, 0.85])
    actions = torch.FloatTensor([0.6, 0.4, 0.7, 0.3, 0.9, 0.1])  # 3 agents × 2 actions
    
    # Get Q-value
    q_value = critic(state, actions)
    
    # Verify shape
    assert q_value.shape == (1,), f"Expected shape (1,), got {q_value.shape}"
    
    print(f"✅ Q-value shape: {q_value.shape}")
    print(f"✅ Q-value: {q_value.item():.4f}")
    print("✅ Critic works correctly!")

def test_decision_maker():
    """Test: Does decision maker produce valid decisions?"""
    print("\n=== Testing Decision Maker ===")
    
    dm = DecisionMaker()
    
    actions = {
        'transaction': np.array([0.6, 0.4]),
        'customer': np.array([0.7, 0.3]),
        'network': np.array([0.9, 0.1])
    }
    
    decision = dm.make_decision(actions)
    
    # Verify structure
    assert 'action' in decision
    assert 'confidence' in decision
    assert 'action_probabilities' in decision
    assert 'contributions' in decision
    
    # Verify action
    assert decision['action'] in ['BLOCK', 'ALLOW']
    
    # Verify confidence
    assert 0 <= decision['confidence'] <= 1
    
    print(f"✅ Action: {decision['action']}")
    print(f"✅ Confidence: {decision['confidence']:.3f}")
    print(f"✅ Contributions: {decision['contributions']}")
    print("✅ Decision Maker works correctly!")

def test_replay_buffer():
    """Test: Does replay buffer store and sample correctly?"""
    print("\n=== Testing Replay Buffer ===")
    
    buffer = ReplayBuffer(capacity=1000)
    
    # Add some experiences
    for i in range(100):
        state = np.random.rand(6)
        actions = [np.random.rand(2) for _ in range(3)]
        rewards = [np.random.rand() for _ in range(3)]
        next_state = np.random.rand(6)
        done = False
        
        buffer.add(state, actions, rewards, next_state, done)
    
    # Verify size
    assert len(buffer) == 100, f"Expected 100 experiences, got {len(buffer)}"
    
    # Sample batch
    batch = buffer.sample(batch_size=32)
    
    # Verify batch structure
    assert 'state' in batch
    assert 'actions' in batch
    assert 'rewards' in batch
    assert 'next_state' in batch
    assert 'done' in batch
    
    print(f"✅ Buffer size: {len(buffer)}")
    print(f"✅ Batch size: {batch['state'].shape[0]}")
    print(f"✅ State shape: {batch['state'].shape}")
    print("✅ Replay Buffer works correctly!")

if __name__ == "__main__":
    test_state_manager()
    test_actor()
    test_critic()
    test_decision_maker()
    test_replay_buffer()
    
    print("\n" + "="*50)
    print("🎉 ALL TESTS PASSED!")
    print("="*50)
```

### Test 2: Integration Test

Create: `marl_orchestrator/test/test_integration.py`

```python
import sys
sys.path.append('..')

from maddpg import maddpg_coordinator

def test_end_to_end():
    """Test: Does the full MADDPG pipeline work?"""
    print("\n=== End-to-End Integration Test ===")
    
    # Simulate agent observations
    observations = {
        'transaction': {
            'probability': 0.85,
            'risk_score': 85
        },
        'customer': {
            'probability': 0.65,
            'risk_score': 65
        },
        'network': {
            'probability': 0.92,
            'risk_score': 92
        }
    }
    
    print("\n📊 Input Observations:")
    for agent, obs in observations.items():
        print(f"  {agent}: prob={obs['probability']:.2f}, score={obs['risk_score']}")
    
    # Get decision
    decision = maddpg_coordinator.decide(observations)
    
    print("\n🎯 MADDPG Decision:")
    print(f"  Action: {decision['action']}")
    print(f"  Confidence: {decision['confidence']:.3f}")
    print(f"  Action Probabilities: {decision['action_probabilities']}")
    print(f"\n  Contributions:")
    for agent, contrib in decision['contributions'].items():
        print(f"    {agent}: {contrib:.3f}")
    
    # Verify structure
    assert 'action' in decision
    assert 'confidence' in decision
    assert decision['action'] in ['BLOCK', 'ALLOW']
    assert 0 <= decision['confidence'] <= 1
    
    print("\n✅ End-to-end test passed!")
    return decision

if __name__ == "__main__":
    test_end_to_end()
```

### Test 3: Behavior Analysis

Create: `marl_orchestrator/test/test_behavior.py`

```python
import sys
sys.path.append('..')

from maddpg import maddpg_coordinator

def test_low_risk_scenario():
    """Test: Low risk should → ALLOW"""
    print("\n=== Test: Low Risk Scenario ===")
    
    observations = {
        'transaction': {'probability': 0.1, 'risk_score': 10},
        'customer': {'probability': 0.15, 'risk_score': 15},
        'network': {'probability': 0.2, 'risk_score': 20}
    }
    
    decision = maddpg_coordinator.decide(observations)
    
    print(f"Decision: {decision['action']} (confidence: {decision['confidence']:.3f})")
    print(f"Expected: ALLOW (low risk)")
    
    # Note: Untrained model may not behave correctly yet!
    print("⚠️  If decision is unexpected, model needs training")

def test_high_risk_scenario():
    """Test: High risk should → BLOCK"""
    print("\n=== Test: High Risk Scenario ===")
    
    observations = {
        'transaction': {'probability': 0.95, 'risk_score': 95},
        'customer': {'probability': 0.90, 'risk_score': 90},
        'network': {'probability': 0.98, 'risk_score': 98}
    }
    
    decision = maddpg_coordinator.decide(observations)
    
    print(f"Decision: {decision['action']} (confidence: {decision['confidence']:.3f})")
    print(f"Expected: BLOCK (high risk)")
    
    print("⚠️  If decision is unexpected, model needs training")

def test_mixed_scenario():
    """Test: Mixed signals"""
    print("\n=== Test: Mixed Risk Scenario ===")
    
    observations = {
        'transaction': {'probability': 0.3, 'risk_score': 30},  # Low
        'customer': {'probability': 0.5, 'risk_score': 50},     # Medium
        'network': {'probability': 0.9, 'risk_score': 90}       # High
    }
    
    decision = maddpg_coordinator.decide(observations)
    
    print(f"Decision: {decision['action']} (confidence: {decision['confidence']:.3f})")
    print(f"Contributions: {decision['contributions']}")
    print(f"Expected: Depends on training - this is where MADDPG adds value!")

if __name__ == "__main__":
    test_low_risk_scenario()
    test_high_risk_scenario()
    test_mixed_scenario()
    
    print("\n" + "="*50)
    print("📊 Behavior analysis complete!")
    print("="*50)
```

---

## Debugging Guide

### Enable Detailed Logging

```python
# In your code
from maddpg import logger
logger.setLevel('DEBUG')

# Or from command line
export LOG_LEVEL=DEBUG
python main.py
```

### Inspect Internal State

```python
# Check network parameters
print(f"Actor parameters: {sum(p.numel() for p in actor.parameters())}")
print(f"Critic parameters: {sum(p.numel() for p in critic.parameters())}")

# Check gradients
for name, param in actor.named_parameters():
    if param.grad is not None:
        print(f"{name}: grad_mean={param.grad.mean():.6f}")
```

### Visualize Decisions

```python
import matplotlib.pyplot as plt

# Test multiple scenarios
scenarios = []
decisions = []

for prob in np.linspace(0, 1, 20):
    obs = {
        'transaction': {'probability': prob, 'risk_score': prob * 100},
        'customer': {'probability': prob, 'risk_score': prob * 100},
        'network': {'probability': prob, 'risk_score': prob * 100}
    }
    decision = maddpg_coordinator.decide(obs)
    scenarios.append(prob)
    decisions.append(1 if decision['action'] == 'BLOCK' else 0)

plt.plot(scenarios, decisions)
plt.xlabel('Risk Probability')
plt.ylabel('Decision (0=ALLOW, 1=BLOCK)')
plt.title('MADDPG Decision Boundary')
plt.show()
```

---

## Next Steps for Learning

1. **Run the tests** above to verify each component
2. **Read the source code** with comments (start with `StateManager`)
3. **Experiment** with different observations
4. **Implement training** (we can add this next)
5. **Visualize** decision boundaries
6. **Compare** with single-agent approaches

Would you like me to create any of these test files or add more explanation on specific components?
