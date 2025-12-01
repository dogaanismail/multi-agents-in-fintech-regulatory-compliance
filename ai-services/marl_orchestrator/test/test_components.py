"""
Test MADDPG Components Individually

This script tests each component of the MADDPG system to ensure
they work correctly before testing the full system.

Author: Ismail Dogan
"""

import sys
import os
import numpy as np
import torch

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from maddpg.networks.actor import Actor
from maddpg.networks.critic import Critic
from maddpg.networks.replay_buffer import ReplayBuffer
from maddpg.core.state_manager import StateManager
from maddpg.core.decision_maker import DecisionMaker


def test_state_manager():
    """Test State Manager: converts observations to state vector"""
    print("\n" + "="*60)
    print("TEST 1: State Manager")
    print("="*60)
    
    # Create sample observations
    observations = {
        "transaction": {"probability": 0.5, "risk_score": 50},
        "customer": {"probability": 0.7, "risk_score": 70},
        "network": {"probability": 0.9, "risk_score": 90}
    }
    
    print("\n📥 Input:")
    for agent, obs in observations.items():
        print(f"  {agent:12s}: prob={obs['probability']:.2f}, score={obs['risk_score']}")
    
    sm = StateManager()
    state = sm.observations_to_state(observations)  # FIXED: correct method name
    
    # Verify shape
    assert state.shape == (6,), f"❌ Expected shape (6,), got {state.shape}"
    
    # Verify values
    expected = [0.5, 0.5, 0.7, 0.7, 0.9, 0.9]
    np.testing.assert_array_almost_equal(state, expected, decimal=2)
    
    print("\n📤 Output:")
    print(f"  State shape: {state.shape}")
    print(f"  State vector: {state}")
    print(f"\n  Breakdown:")
    print(f"    [0-1] Transaction: {state[0]:.2f} (prob), {state[1]:.2f} (score/100)")
    print(f"    [2-3] Customer:    {state[2]:.2f} (prob), {state[3]:.2f} (score/100)")
    print(f"    [4-5] Network:     {state[4]:.2f} (prob), {state[5]:.2f} (score/100)")
    
    print("\n✅ State Manager works correctly!")
    return True


def test_actor():
    """Test: Does actor produce valid action probabilities?"""
    print("\n" + "="*60)
    print("TEST 2: Actor Network")
    print("="*60)
    
    actor = Actor(state_dim=6, action_dim=2, hidden_dim=256)
    
    print("\n📊 Architecture:")
    print(f"  Input:  6 (state features)")
    print(f"  Hidden: 256 → 256")
    print(f"  Output: 2 (action probabilities)")
    print(f"  Total parameters: {sum(p.numel() for p in actor.parameters()):,}")
    
    # Create dummy state - FIXED: add batch dimension
    state = torch.FloatTensor([[0.3, 0.25, 0.6, 0.60, 0.9, 0.85]])
    
    print("\n📥 Input state:")
    print(f"  {state.numpy()[0]}")
    
    # Get action
    action_probs = actor(state)
    
    # Verify shape - FIXED: expect batch dimension
    assert action_probs.shape == (1, 2), f"❌ Expected shape (1, 2), got {action_probs.shape}"
    
    # Verify probabilities sum to 1
    prob_sum = action_probs.sum(dim=-1).item()
    assert abs(prob_sum - 1.0) < 0.01, f"❌ Probabilities should sum to 1, got {prob_sum}"
    
    # Verify all positive
    assert (action_probs >= 0).all(), "❌ All probabilities should be positive"
    
    print("\n📤 Output:")
    print(f"  Action probabilities: {action_probs.detach().numpy()[0]}")
    print(f"    P(BLOCK) = {action_probs[0][0].item():.4f}")
    print(f"    P(ALLOW) = {action_probs[0][1].item():.4f}")
    print(f"  Sum: {prob_sum:.6f}")
    
    print("\n✅ Actor works correctly!")
    return True


def test_critic():
    """Test: Does critic produce Q-values?"""
    print("\n" + "="*60)
    print("TEST 3: Critic Network")
    print("="*60)
    
    critic = Critic(state_dim=6, action_dim=2, num_agents=3, hidden_dim=256)
    
    print("\n📊 Architecture:")
    print(f"  Input:  12 (6 state + 6 actions from 3 agents)")
    print(f"  Hidden: 256 → 256")
    print(f"  Output: 1 (Q-value)")
    print(f"  Total parameters: {sum(p.numel() for p in critic.parameters()):,}")
    
    # Create dummy inputs - FIXED: add batch dimension and proper shape
    state = torch.FloatTensor([[0.3, 0.25, 0.6, 0.60, 0.9, 0.85]])
    # Critic expects actions as list of tensors, one per agent
    actions = [
        torch.FloatTensor([[0.6, 0.4]]),  # Agent 0
        torch.FloatTensor([[0.7, 0.3]]),  # Agent 1
        torch.FloatTensor([[0.9, 0.1]])   # Agent 2
    ]
    
    print("\n📥 Inputs:")
    print(f"  State:   {state.numpy()[0]}")
    print(f"  Actions:")
    print(f"    Agent 0: [0.6, 0.4] (60% BLOCK)")
    print(f"    Agent 1: [0.7, 0.3] (70% BLOCK)")
    print(f"    Agent 2: [0.9, 0.1] (90% BLOCK)")
    
    # Get Q-value
    q_value = critic(state, actions)
    
    # Verify shape
    assert q_value.shape == (1, 1), f"❌ Expected shape (1, 1), got {q_value.shape}"
    
    print("\n📤 Output:")
    print(f"  Q-value: {q_value.item():.4f}")
    print(f"  (Higher = better joint action)")
    
    print("\n✅ Critic works correctly!")
    return True


def test_decision_maker():
    """Test: Does decision maker aggregate properly?"""
    print("\n" + "="*60)
    print("TEST 4: Decision Maker")
    print("="*60)
    
    dm = DecisionMaker()
    
    # Create dummy actor outputs - FIXED: proper format
    action_probs = [
        torch.FloatTensor([[0.6, 0.4]]),  # transaction: 60% BLOCK
        torch.FloatTensor([[0.7, 0.3]]),  # customer: 70% BLOCK
        torch.FloatTensor([[0.9, 0.1]])   # network: 90% BLOCK
    ]
    q_value = torch.FloatTensor([[0.85]])
    
    print("\n📥 Actor outputs:")
    print(f"  transaction : [0.60, 0.40] → 60% BLOCK")
    print(f"  customer    : [0.70, 0.30] → 70% BLOCK")
    print(f"  network     : [0.90, 0.10] → 90% BLOCK")
    print(f"  Q-value     : 0.85")
    
    # Make decision
    decision = dm.make_decision(action_probs, q_value)
    
    # Verify structure
    assert "action" in decision, "❌ Missing 'action' field"
    assert "confidence" in decision, "❌ Missing 'confidence' field"
    assert "q_value" in decision, "❌ Missing 'q_value' field"
    assert "contributions" in decision, "❌ Missing 'contributions' field"
    
    # Verify values
    assert decision["action"] in ["BLOCK", "ALLOW"], f"❌ Invalid action: {decision['action']}"
    assert 0 <= decision["confidence"] <= 1, f"❌ Invalid confidence: {decision['confidence']}"
    
    print("\n📤 Final decision:")
    print(f"  Action:     {decision['action']}")
    print(f"  Confidence: {decision['confidence']:.4f}")
    print(f"  Q-value:    {decision['q_value']:.4f}")
    print(f"  Contributions:")
    for agent, contrib in decision['contributions'].items():
        print(f"    {agent:12s}: {contrib:.4f}")
    
    print("\n  Explanation:")
    print(f"  Average of [0.6, 0.7, 0.9] = 0.733")
    print(f"  0.733 > 0.267, so choose BLOCK")
    print(f"  Confidence = 0.733")
    
    print("\n✅ Decision Maker works correctly!")
    return True


def test_replay_buffer():
    """Test: Can replay buffer store and sample?"""
    print("\n" + "="*60)
    print("TEST 5: Replay Buffer")
    print("="*60)
    
    buffer = ReplayBuffer(capacity=1000)
    
    print("\n📊 Configuration:")
    print(f"  Capacity: 1000")
    
    print("\n📥 Adding 100 experiences...")
    
    # Add experiences
    for i in range(100):
        state = np.random.rand(6)
        actions = [np.random.rand(2), np.random.rand(2), np.random.rand(2)]
        reward = np.random.rand()
        next_state = np.random.rand(6)
        done = False
        
        buffer.push(state, actions, reward, next_state, done)  # FIXED: correct method name
    
    print(f"  Added: {len(buffer)} experiences")
    
    # Sample batch
    batch_size = 32
    states, actions, rewards, next_states, dones = buffer.sample(batch_size)
    
    # Verify shapes
    assert states.shape == (batch_size, 6), f"❌ Wrong states shape: {states.shape}"
    assert len(actions) == 3, f"❌ Should have 3 agent actions, got {len(actions)}"
    assert rewards.shape == (batch_size, 1), f"❌ Wrong rewards shape: {rewards.shape}"
    assert next_states.shape == (batch_size, 6), f"❌ Wrong next_states shape: {next_states.shape}"
    assert dones.shape == (batch_size, 1), f"❌ Wrong dones shape: {dones.shape}"
    
    print("\n📤 Sample batch:")
    print(f"  Batch size: {batch_size}")
    print(f"  States shape:      {states.shape}")
    print(f"  Actions per agent: {actions[0].shape}")
    print(f"  Rewards shape:     {rewards.shape}")
    print(f"  Next states shape: {next_states.shape}")
    print(f"  Dones shape:       {dones.shape}")
    
    print("\n  First experience:")
    print(f"    State:      {states[0].numpy()}")
    print(f"    Actions:    {[a[0].numpy() for a in actions]}")
    print(f"    Reward:     {rewards[0].item():.4f}")
    print(f"    Next state: {next_states[0].numpy()}")
    print(f"    Done:       {bool(dones[0].item())}")
    
    print("\n✅ Replay Buffer works correctly!")
    return True


def main():
    """Run all component tests"""
    print("="*60)
    print("MADDPG COMPONENT TESTS")
    print("="*60)
    print("\nThis will test each component individually to ensure")
    print("they work correctly before testing the full system.")
    
    tests = [
        ("State Manager", test_state_manager),
        ("Actor Network", test_actor),
        ("Critic Network", test_critic),
        ("Decision Maker", test_decision_maker),
        ("Replay Buffer", test_replay_buffer)
    ]
    
    results = {}
    for name, test_fn in tests:
        try:
            result = test_fn()
            results[name] = ("PASS", None)
        except Exception as e:
            results[name] = ("FAIL", str(e))
            print(f"\n❌ {name} FAILED: {e}")
    
    # Summary
    print("\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    
    for name, (status, error) in results.items():
        if status == "PASS":
            print(f"✅ PASS - {name}")
        else:
            print(f"❌ FAIL - {name}")
            print(f"       Error: {error}")
    
    passed = sum(1 for s, _ in results.values() if s == "PASS")
    total = len(tests)
    print(f"\nResult: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n🎉 All tests passed! Components work correctly.")
        return 0
    else:
        print("\n⚠️  Some tests failed. Please review the errors above.")
        return 1


if __name__ == "__main__":
    exit(main())
