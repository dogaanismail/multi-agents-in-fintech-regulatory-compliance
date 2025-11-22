# MADDPG Training Guide

## 📚 Overview

This guide explains how to train the MADDPG (Multi-Agent Deep Deterministic Policy Gradient) coordinator for AML (Anti-Money Laundering) compliance.

## 🎯 Training Objectives

The MADDPG coordinator learns to:
1. **Coordinate decisions** across 3 detection agents (transaction, customer, network)
2. **Maximize detection accuracy** for money laundering
3. **Minimize false positives** to avoid blocking legitimate transactions
4. **Learn optimal policy** through experience replay

## 🏗️ Architecture

### Components:
- **3 Actor Networks**: One per agent (transaction, customer, network)
  - Input: Global state (6 features)
  - Output: Action probabilities [BLOCK, ALLOW]
  
- **1 Centralized Critic**: Evaluates joint actions
  - Input: Global state + all agent actions
  - Output: Q-value (expected future reward)
  
- **Replay Buffer**: Stores experiences for off-policy learning
  - Capacity: 100,000 transitions
  - Sample batch size: 64

### State Space (6 dimensions):
```python
state = [
    txn_prob,        # Transaction agent probability
    txn_score/100,   # Transaction risk score (normalized)
    cust_prob,       # Customer agent probability
    cust_score/100,  # Customer risk score (normalized)
    net_prob,        # Network agent probability
    net_score/100    # Network risk score (normalized)
]
```

### Action Space (2 actions per agent):
- 0: **BLOCK** transaction
- 1: **ALLOW** transaction

### Reward Function:
```python
if action == BLOCK and label == MONEY_LAUNDERING:
    reward = +10.0   # Correct detection (True Positive)
elif action == ALLOW and label == LEGITIMATE:
    reward = +5.0    # Correct approval (True Negative)
elif action == BLOCK and label == LEGITIMATE:
    reward = -5.0    # False Positive (unnecessary block)
else:  # action == ALLOW and label == MONEY_LAUNDERING
    reward = -20.0   # False Negative (missed detection) - CRITICAL
```

## 🚀 Training Methods

### Option 1: Jupyter Notebook (Recommended for exploration)

**Best for:** Interactive training, visualization, hyperparameter tuning

```bash
cd marl_orchestrator
jupyter notebook notebooks/01_MADDPG_Training.ipynb
```

**Features:**
- ✅ Step-by-step execution
- ✅ Real-time visualization
- ✅ Easy hyperparameter tuning
- ✅ Training curve plots
- ✅ Evaluation metrics

### Option 2: Command-line Script

**Best for:** Production training, automated runs

```bash
cd marl_orchestrator
python train.py --episodes 1000 --steps 100 --batch-size 64 --save-freq 100
```

**Arguments:**
- `--episodes`: Number of training episodes (default: 1000)
- `--steps`: Max steps per episode (default: 100)
- `--batch-size`: Batch size for updates (default: 64)
- `--save-freq`: Save checkpoint every N episodes (default: 100)

## ⚙️ Training Configuration

### Hyperparameters:

```python
CONFIG = {
    # Network architecture
    'state_dim': 6,
    'action_dim': 2,
    'num_agents': 3,
    'hidden_dim': 256,
    
    # Learning
    'learning_rate': 0.001,
    'gamma': 0.99,           # Discount factor
    'tau': 0.01,             # Soft update rate
    'buffer_size': 100000,   # Replay buffer capacity
    'batch_size': 64,
    
    # Training
    'num_episodes': 1000,
    'max_steps_per_episode': 100,
    'update_frequency': 10,   # Update every N steps
    
    # Exploration
    'epsilon_start': 0.5,     # Initial exploration rate
    'epsilon_end': 0.01,      # Final exploration rate
    'epsilon_decay': 0.995,   # Decay rate per episode
}
```

### Tuning Tips:

1. **Learning Rate (0.001)**
   - Too high: Unstable training
   - Too low: Slow convergence
   - Try: 0.0005, 0.001, 0.005

2. **Gamma (0.99)**
   - Higher: Prioritize long-term rewards
   - Lower: Prioritize immediate rewards
   - Try: 0.95, 0.99, 0.995

3. **Epsilon Decay (0.995)**
   - Faster decay: Exploit earlier
   - Slower decay: Explore longer
   - Try: 0.99, 0.995, 0.999

4. **Hidden Dim (256)**
   - Larger: More capacity (slower training)
   - Smaller: Faster training (may underfit)
   - Try: 128, 256, 512

## 📊 Training Pipeline

### 1. Prerequisites

**Start detection agents:**
```bash
# Terminal 1: Transaction Agent
cd agents/transaction_pattern_agent
python main.py

# Terminal 2: Customer Agent
cd agents/customer_risk_agent
python main.py

# Terminal 3: Network Agent
cd agents/network_analysis_agent
python main.py
```

**Verify agents are running:**
```bash
curl http://localhost:8001/api/v1/health
curl http://localhost:8002/api/v1/health
curl http://localhost:8003/api/v1/health
```

### 2. Training

**Set mode:**
```bash
export MADDPG_MODE=training
```

**Run training:**
```bash
cd marl_orchestrator
python train.py --episodes 1000
```

**Or use notebook:**
```bash
jupyter notebook notebooks/01_MADDPG_Training.ipynb
```

### 3. Monitoring

**Training logs:**
- Console output shows episode rewards, accuracy, epsilon
- TensorBoard support (coming soon)

**Checkpoints:**
- Saved every 100 episodes to `trained_models/checkpoints/`
- Best model saved to `trained_models/`

### 4. Evaluation

After training, evaluate the model:

```python
# Load trained model
maddpg.load_models("trained_models")

# Test on evaluation set
accuracy, precision, recall, f1 = evaluate_model(maddpg, test_data)
```

### 5. Deployment

**Switch to inference mode:**
```bash
export MADDPG_MODE=inference
```

**Restart orchestrator:**
```bash
cd marl_orchestrator
python main.py
```

**Test inference:**
```bash
python test_orchestrator.py
```

## 📈 Expected Results

### Training Progress:

| Episode | Avg Reward | Accuracy | Epsilon |
|---------|------------|----------|---------|
| 100     | -50.2      | 52%      | 0.45    |
| 300     | 120.5      | 68%      | 0.30    |
| 500     | 350.8      | 78%      | 0.15    |
| 700     | 520.3      | 85%      | 0.05    |
| 1000    | 650.1      | 90%      | 0.01    |

### Final Metrics (Expected):
- **Accuracy**: 85-92%
- **Precision**: 80-88%
- **Recall**: 75-85%
- **F1-Score**: 78-86%

## 📁 Output Files

After training, you'll have:

```
trained_models/
├── actor_transaction.pth       # Transaction agent actor
├── actor_customer.pth          # Customer agent actor
├── actor_network.pth           # Network agent actor
├── critic.pth                  # Centralized critic
├── training_metadata.json      # Training info
└── checkpoints/
    ├── checkpoint_ep100/
    ├── checkpoint_ep200/
    └── ...
```

## 🐛 Troubleshooting

### Issue: Reward not improving

**Solution:**
- Check if detection agents are running
- Verify data loading correctly
- Reduce learning rate (try 0.0005)
- Increase exploration (higher epsilon_start)

### Issue: Training unstable

**Solution:**
- Decrease learning rate
- Increase batch size (try 128)
- Use gradient clipping (already implemented)
- Check for NaN in losses

### Issue: Low accuracy

**Solution:**
- Train longer (2000+ episodes)
- Tune reward function weights
- Increase network capacity (hidden_dim=512)
- Collect more training data

### Issue: High false negatives

**Solution:**
- Increase penalty for false negatives (reward = -30)
- Adjust decision threshold
- Balance training data

## 🔄 Training Workflow

```
1. Prepare Data
   ↓
2. Start Detection Agents
   ↓
3. Set MADDPG_MODE=training
   ↓
4. Run Training (notebook or script)
   ↓
5. Monitor Progress
   ↓
6. Evaluate Performance
   ↓
7. Save Best Model
   ↓
8. Set MADDPG_MODE=inference
   ↓
9. Deploy Trained Model
```

## 📚 Additional Resources

- **Paper**: [Multi-Agent Actor-Critic for Mixed Cooperative-Competitive Environments](https://arxiv.org/abs/1706.02275)
- **PyTorch Docs**: [torch.nn](https://pytorch.org/docs/stable/nn.html)
- **RL Book**: Sutton & Barto - Reinforcement Learning

## 🎓 Key Concepts

### MADDPG vs DDPG:
- **DDPG**: Single agent
- **MADDPG**: Multiple agents with centralized training, decentralized execution

### Centralized Training:
- Critic has access to ALL agent observations
- Enables coordinated learning

### Decentralized Execution:
- Each actor only uses local observations
- Enables scalable deployment

### Experience Replay:
- Breaks correlation between consecutive samples
- Enables stable off-policy learning

## ✅ Next Steps After Training

1. **Evaluate thoroughly** on test set
2. **Compare with baselines** (individual agents)
3. **Analyze agent contributions**
4. **A/B test** in production
5. **Continue fine-tuning** with real feedback

---

**Author:** Ismail Dogan  
**Date:** November 2024  
**Version:** 1.0.0
