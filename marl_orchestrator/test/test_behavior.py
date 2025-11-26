"""
Behavior analysis for MADDPG - Understanding decision patterns

Run: python test_behavior.py

Author: Ismail Dogan
"""

import sys
sys.path.append('..')

import numpy as np
from maddpg import maddpg_coordinator

def analyze_risk_gradient():
    """Analyze: How does MADDPG respond to increasing risk?"""
    print("\n" + "="*60)
    print("BEHAVIOR ANALYSIS: Risk Gradient")
    print("="*60)
    
    print("\n📊 Testing MADDPG response to increasing risk levels")
    print("We'll gradually increase risk from 0% to 100%\n")
    
    risk_levels = np.linspace(0, 1, 11)  # 0%, 10%, 20%, ..., 100%
    results = []
    
    print("Risk%  | Decision | Confidence | Q-Value")
    print("─" * 60)
    
    for risk in risk_levels:
        observations = {
            'transaction': {'probability': risk, 'risk_score': risk * 100},
            'customer': {'probability': risk, 'risk_score': risk * 100},
            'network': {'probability': risk, 'risk_score': risk * 100}
        }
        
        decision = maddpg_coordinator.decide(observations)
        
        print(f"{risk*100:5.0f}% | {decision['action']:8s} | {decision['confidence']:10.3f} | {decision['q_value']:10.4f}")
        
        results.append({
            'risk': risk,
            'action': decision['action'],
            'confidence': decision['confidence'],
            'q_value': decision['q_value']
        })
    
    # Analysis
    print("\n💡 Analysis:")
    
    # Count blocks vs allows
    blocks = sum(1 for r in results if r['action'] == 'BLOCK')
    allows = sum(1 for r in results if r['action'] == 'ALLOW')
    
    print(f"  Total BLOCK decisions: {blocks}/11")
    print(f"  Total ALLOW decisions: {allows}/11")
    
    # Find decision boundary
    transitions = []
    for i in range(len(results) - 1):
        if results[i]['action'] != results[i+1]['action']:
            transitions.append(results[i]['risk'] * 100)
    
    if transitions:
        print(f"  Decision boundary at: ~{transitions[0]:.0f}% risk")
    else:
        print(f"  No decision boundary (always {results[0]['action']})")
    
    # Check if confidence increases with certainty
    low_risk_conf = results[0]['confidence']
    high_risk_conf = results[-1]['confidence']
    
    print(f"\n  Confidence at 0% risk:   {low_risk_conf:.3f}")
    print(f"  Confidence at 100% risk: {high_risk_conf:.3f}")
    
    print("\n⚠️  Expected behavior (trained model):")
    print("  - Low risk → ALLOW with high confidence")
    print("  - High risk → BLOCK with high confidence")
    print("  - Clear decision boundary around 50%")
    print("  - Smooth probability transition")
    
    return results

def analyze_agent_disagreement():
    """Analyze: How does MADDPG handle conflicting agent opinions?"""
    print("\n" + "="*60)
    print("BEHAVIOR ANALYSIS: Agent Disagreement")
    print("="*60)
    
    print("\n📊 Testing scenarios where agents disagree")
    
    scenarios = [
        {
            'name': 'All Agree (Low)',
            'observations': {
                'transaction': {'probability': 0.1, 'risk_score': 10},
                'customer': {'probability': 0.1, 'risk_score': 10},
                'network': {'probability': 0.1, 'risk_score': 10}
            }
        },
        {
            'name': 'All Agree (High)',
            'observations': {
                'transaction': {'probability': 0.9, 'risk_score': 90},
                'customer': {'probability': 0.9, 'risk_score': 90},
                'network': {'probability': 0.9, 'risk_score': 90}
            }
        },
        {
            'name': 'One Disagrees (2 low, 1 high)',
            'observations': {
                'transaction': {'probability': 0.2, 'risk_score': 20},
                'customer': {'probability': 0.2, 'risk_score': 20},
                'network': {'probability': 0.9, 'risk_score': 90}
            }
        },
        {
            'name': 'One Disagrees (2 high, 1 low)',
            'observations': {
                'transaction': {'probability': 0.9, 'risk_score': 90},
                'customer': {'probability': 0.9, 'risk_score': 90},
                'network': {'probability': 0.1, 'risk_score': 10}
            }
        },
        {
            'name': 'Evenly Split (low-med-high)',
            'observations': {
                'transaction': {'probability': 0.2, 'risk_score': 20},
                'customer': {'probability': 0.5, 'risk_score': 50},
                'network': {'probability': 0.8, 'risk_score': 80}
            }
        }
    ]
    
    print("\nScenario | Txn | Cust | Net | Decision | Conf | Rationale")
    print("─" * 80)
    
    for scenario in scenarios:
        obs = scenario['observations']
        decision = maddpg_coordinator.decide(obs)
        
        txn_risk = obs['transaction']['probability'] * 100
        cust_risk = obs['customer']['probability'] * 100
        net_risk = obs['network']['probability'] * 100
        
        print(f"{scenario['name']:28s} | {txn_risk:3.0f} | {cust_risk:4.0f} | {net_risk:3.0f} | {decision['action']:8s} | {decision['confidence']:.2f}")
    
    print("\n💡 Key Questions:")
    print("  1. Does majority vote win? (2 vs 1)")
    print("  2. Are certain agents weighted more?")
    print("  3. How does confidence reflect disagreement?")
    print("  4. Is the model learning coordination strategies?")
    
    print("\n⚠️  Expected behavior (trained model):")
    print("  - Should learn agent importance (not just majority vote)")
    print("  - Lower confidence when agents disagree")
    print("  - May learn that certain agents are more reliable")

def analyze_individual_contributions():
    """Analyze: Which agents contribute most to decisions?"""
    print("\n" + "="*60)
    print("BEHAVIOR ANALYSIS: Individual Agent Contributions")
    print("="*60)
    
    print("\n📊 Analyzing each agent's influence on the decision")
    
    # Test scenario
    observations = {
        'transaction': {'probability': 0.6, 'risk_score': 60},
        'customer': {'probability': 0.7, 'risk_score': 70},
        'network': {'probability': 0.8, 'risk_score': 80}
    }
    
    decision = maddpg_coordinator.decide(observations)
    
    print("\nScenario: Moderately high risk across all agents")
    print(f"  Transaction: 60% risk")
    print(f"  Customer:    70% risk")
    print(f"  Network:     80% risk")
    
    print(f"\nMADDPG Decision: {decision['action']} (confidence: {decision['confidence']:.3f})")
    
    print(f"\nAgent Contributions:")
    contributions = decision['contributions']
    total_contrib = sum(contributions.values())
    
    for agent, contrib in contributions.items():
        percentage = (contrib / total_contrib * 100) if total_contrib > 0 else 0
        bar = '█' * int(percentage / 2)
        print(f"  {agent:12s}: {contrib:.3f} ({percentage:5.1f}%) {bar}")
    
    print("\n💡 Interpretation:")
    print("  - Higher contribution = more influence on final decision")
    print("  - Ideally should reflect agent reliability/importance")
    print("  - Untrained model may show equal contributions")
    print("\n⚠️  Expected behavior (trained model):")
    print("  - Learned weights based on agent performance")
    print("  - More reliable agents have higher contribution")

def analyze_confidence_patterns():
    """Analyze: When is MADDPG confident vs uncertain?"""
    print("\n" + "="*60)
    print("BEHAVIOR ANALYSIS: Confidence Patterns")
    print("="*60)
    
    print("\n📊 Understanding when MADDPG is confident\n")
    
    scenarios = [
        ('Strong consensus (all low)', {'t': 0.1, 'c': 0.1, 'n': 0.1}),
        ('Strong consensus (all high)', {'t': 0.9, 'c': 0.9, 'n': 0.9}),
        ('Near boundary', {'t': 0.5, 'c': 0.5, 'n': 0.5}),
        ('High variance', {'t': 0.1, 'c': 0.5, 'n': 0.9}),
        ('Moderate all', {'t': 0.6, 'c': 0.6, 'n': 0.6}),
    ]
    
    print("Scenario | Txn | Cust | Net | Variance | Decision | Confidence")
    print("─" * 75)
    
    for name, risks in scenarios:
        observations = {
            'transaction': {'probability': risks['t'], 'risk_score': risks['t'] * 100},
            'customer': {'probability': risks['c'], 'risk_score': risks['c'] * 100},
            'network': {'probability': risks['n'], 'risk_score': risks['n'] * 100}
        }
        
        decision = maddpg_coordinator.decide(observations)
        
        # Calculate variance
        risk_values = [risks['t'], risks['c'], risks['n']]
        variance = np.var(risk_values)
        
        print(f"{name:23s} | {risks['t']:.1f} | {risks['c']:4.1f} | {risks['n']:.1f} | {variance:8.3f} | {decision['action']:8s} | {decision['confidence']:.3f}")
    
    print("\n💡 Hypothesis:")
    print("  - High variance (disagreement) → Lower confidence")
    print("  - Strong consensus → Higher confidence")
    print("  - Near decision boundary → Lower confidence")
    
    print("\n⚠️  Expected behavior (trained model):")
    print("  - Confidence correlates with agreement")
    print("  - Extreme values (0.1 or 0.9) → high confidence")
    print("  - Mid-range values (0.5) → lower confidence")

def main():
    """Run all behavior analyses"""
    print("\n" + "="*60)
    print("MADDPG BEHAVIOR ANALYSIS")
    print("="*60)
    print("\nThis analyzes MADDPG decision patterns to understand")
    print("how it processes different scenarios.")
    
    analyses = [
        ("Risk Gradient", analyze_risk_gradient),
        ("Agent Disagreement", analyze_agent_disagreement),
        ("Individual Contributions", analyze_individual_contributions),
        ("Confidence Patterns", analyze_confidence_patterns)
    ]
    
    for name, analysis_func in analyses:
        try:
            analysis_func()
        except Exception as e:
            print(f"\n❌ {name} failed: {e}")
    
    # Summary
    print("\n" + "="*60)
    print("ANALYSIS COMPLETE")
    print("="*60)
    
    print("\n📚 Understanding Your Results:")
    print("\n1. UNTRAINED MODEL (current state):")
    print("   - Random-looking decisions")
    print("   - No clear decision boundary")
    print("   - Equal agent contributions")
    print("   - Confidence may not correlate with risk")
    
    print("\n2. TRAINED MODEL (after training):")
    print("   - Clear decision patterns")
    print("   - Smooth transition from ALLOW→BLOCK")
    print("   - Learned agent importance")
    print("   - Confidence reflects certainty")
    
    print("\n🎯 Next Steps:")
    print("   1. Train the model with labeled data")
    print("   2. Re-run these analyses to see improvements")
    print("   3. Compare before/after training")
    
    print("\n📖 Read docs/UNDERSTANDING_MADDPG.md for more details!")

if __name__ == "__main__":
    main()
