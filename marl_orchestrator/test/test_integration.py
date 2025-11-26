"""
Integration tests for MADDPG - End-to-end testing

Run: python test_integration.py

Author: Ismail Dogan
"""

import sys
sys.path.append('..')

from maddpg import maddpg_coordinator

def test_end_to_end():
    """Test: Does the full MADDPG pipeline work?"""
    print("\n" + "="*60)
    print("INTEGRATION TEST: End-to-End MADDPG")
    print("="*60)
    
    print("\n📋 Scenario: High-risk transaction")
    print("A customer with suspicious behavior makes a high-value")
    print("transaction in a known fraud network.")
    
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
    print("─" * 60)
    for agent, obs in observations.items():
        print(f"  {agent.capitalize():12s} Agent:")
        print(f"    Fraud Probability: {obs['probability']*100:.1f}%")
        print(f"    Risk Score:        {obs['risk_score']:.0f}/100")
    
    # Get decision
    print("\n⏳ Processing through MADDPG...")
    print("  1. Building state vector from observations...")
    print("  2. Running 3 actor networks...")
    print("  3. Aggregating decisions...")
    
    decision = maddpg_coordinator.decide(observations)
    
    print("\n🎯 MADDPG Decision:")
    print("─" * 60)
    print(f"  Final Action:  {decision['action']}")
    print(f"  Confidence:    {decision['confidence']*100:.1f}%")
    print(f"  Q-Value:       {decision['q_value']:.4f}")
    
    print(f"\n  Agent Contributions (to {decision['action']}):")
    for agent, contrib in decision['contributions'].items():
        print(f"    {agent.capitalize():12s}: {contrib*100:.1f}%")
    
    # Verify structure
    assert 'action' in decision, "❌ Missing 'action' in decision"
    assert 'confidence' in decision, "❌ Missing 'confidence' in decision"
    assert decision['action'] in ['BLOCK', 'ALLOW'], f"❌ Invalid action: {decision['action']}"
    assert 0 <= decision['confidence'] <= 1, f"❌ Invalid confidence: {decision['confidence']}"
    
    print("\n✅ End-to-end test passed!")
    print("\n💡 Note: The decision depends on the trained model.")
    print("   Untrained models may produce random-looking decisions.")
    
    return decision

def test_multiple_scenarios():
    """Test: How does MADDPG behave across different scenarios?"""
    print("\n" + "="*60)
    print("INTEGRATION TEST: Multiple Scenarios")
    print("="*60)
    
    scenarios = [
        {
            'name': '🟢 Low Risk',
            'description': 'All agents report low risk',
            'observations': {
                'transaction': {'probability': 0.1, 'risk_score': 10},
                'customer': {'probability': 0.15, 'risk_score': 15},
                'network': {'probability': 0.08, 'risk_score': 8}
            }
        },
        {
            'name': '🟡 Medium Risk',
            'description': 'Moderate risk signals',
            'observations': {
                'transaction': {'probability': 0.45, 'risk_score': 45},
                'customer': {'probability': 0.50, 'risk_score': 50},
                'network': {'probability': 0.48, 'risk_score': 48}
            }
        },
        {
            'name': '🔴 High Risk',
            'description': 'All agents report high risk',
            'observations': {
                'transaction': {'probability': 0.95, 'risk_score': 95},
                'customer': {'probability': 0.90, 'risk_score': 90},
                'network': {'probability': 0.98, 'risk_score': 98}
            }
        },
        {
            'name': '🟠 Conflicting Signals',
            'description': 'Mixed risk signals across agents',
            'observations': {
                'transaction': {'probability': 0.2, 'risk_score': 20},
                'customer': {'probability': 0.5, 'risk_score': 50},
                'network': {'probability': 0.9, 'risk_score': 90}
            }
        }
    ]
    
    results = []
    
    for scenario in scenarios:
        print(f"\n{scenario['name']}: {scenario['description']}")
        print("─" * 60)
        
        # Show inputs
        print("Observations:")
        for agent, obs in scenario['observations'].items():
            print(f"  {agent:12s}: {obs['probability']*100:5.1f}% risk")
        
        # Get decision
        decision = maddpg_coordinator.decide(scenario['observations'])
        
        # Show output
        print(f"\nDecision: {decision['action']} (confidence: {decision['confidence']*100:.1f}%)")
        
        results.append({
            'scenario': scenario['name'],
            'decision': decision['action'],
            'confidence': decision['confidence']
        })
    
    # Summary
    print("\n" + "="*60)
    print("SCENARIO SUMMARY")
    print("="*60)
    
    for result in results:
        print(f"{result['scenario']:25s} → {result['decision']:5s} ({result['confidence']*100:5.1f}%)")
    
    print("\n💡 Analysis:")
    print("  - Compare decisions across risk levels")
    print("  - Check if confidence correlates with risk")
    print("  - Conflicting signals test coordination ability")
    print("\n⚠️  Remember: Untrained models may show random behavior!")
    
    print("\n✅ Multiple scenario test completed!")

def test_consistency():
    """Test: Does MADDPG give consistent results?"""
    print("\n" + "="*60)
    print("INTEGRATION TEST: Consistency Check")
    print("="*60)
    
    print("\n📋 Testing if same input → same output")
    
    observations = {
        'transaction': {'probability': 0.7, 'risk_score': 70},
        'customer': {'probability': 0.6, 'risk_score': 60},
        'network': {'probability': 0.8, 'risk_score': 80}
    }
    
    # Run multiple times
    decisions = []
    for i in range(5):
        decision = maddpg_coordinator.decide(observations)
        decisions.append(decision)
        print(f"  Run {i+1}: {decision['action']} (conf: {decision['confidence']:.3f})")
    
    # Check consistency
    actions = [d['action'] for d in decisions]
    confidences = [d['confidence'] for d in decisions]
    
    all_same_action = len(set(actions)) == 1
    conf_variance = max(confidences) - min(confidences)
    
    print(f"\n📊 Results:")
    print(f"  Action consistency: {all_same_action} (all {actions[0] if all_same_action else 'MIXED'})")
    print(f"  Confidence variance: {conf_variance:.6f}")
    
    assert all_same_action, "❌ Actions are not consistent!"
    assert conf_variance < 0.001, f"❌ Confidence varies too much: {conf_variance}"
    
    print("\n✅ Consistency check passed!")
    print("   MADDPG produces deterministic results for same input.")

def main():
    """Run all integration tests"""
    print("\n" + "="*60)
    print("MADDPG INTEGRATION TESTS")
    print("="*60)
    print("\nThese tests verify the complete MADDPG system works")
    print("end-to-end with realistic scenarios.")
    
    tests = [
        ("End-to-End", test_end_to_end),
        ("Multiple Scenarios", test_multiple_scenarios),
        ("Consistency", test_consistency)
    ]
    
    results = []
    for name, test_func in tests:
        try:
            test_func()
            results.append((name, True, None))
        except Exception as e:
            results.append((name, False, str(e)))
            print(f"\n❌ {name} FAILED: {e}")
    
    # Summary
    print("\n" + "="*60)
    print("INTEGRATION TEST SUMMARY")
    print("="*60)
    
    passed = sum(1 for _, success, _ in results if success)
    total = len(results)
    
    for name, success, error in results:
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"{status} - {name}")
        if error:
            print(f"       Error: {error}")
    
    print(f"\nResult: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n🎉 ALL INTEGRATION TESTS PASSED!")
        print("\nYour MADDPG system is working correctly!")
        print("\n📚 Next steps:")
        print("  1. Read docs/UNDERSTANDING_MADDPG.md for detailed explanation")
        print("  2. Implement training to improve decision quality")
        print("  3. Analyze behavior with test_behavior.py")
    else:
        print("\n⚠️  Some tests failed. Please review the errors above.")
    
    return passed == total

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
