"""
Test script for MARL Orchestrator
Tests coordinated prediction with all 3 agents

Author: Ismail Dogan
"""

import requests
import json
from pprint import pprint

# Base URL
BASE_URL = "http://localhost:1004"

def test_health():
    """Test health endpoint"""
    print("\n" + "="*60)
    print("Testing Health Endpoint")
    print("="*60)
    
    response = requests.get(f"{BASE_URL}/api/v1/health")
    print(f"Status Code: {response.status_code}")
    pprint(response.json())

def test_coordinated_prediction():
    """Test coordinated prediction"""
    print("\n" + "="*60)
    print("Testing Coordinated Prediction")
    print("="*60)
    
    # Sample request
    request_data = {
        "transaction_id": "TXN_TEST_001",
        "transaction": {
            "Date": "2024-01-15",
            "Time": "14:30:00",
            "From_Bank": "HSBC Bank",
            "Account": "ACC_SENDER_001",
            "To_Bank": "Chase Bank",
            "Account_1": "ACC_RECEIVER_002",
            "Amount_Received": 15000.50,
            "Receiving_Currency": "USD",
            "Amount_Paid": 15000.50,
            "Payment_Currency": "USD",
            "Payment_type": "Wire",
            "Sender_bank_location": "USA",
            "Receiver_bank_location": "UK"
        },
        "customer": {
            "transaction_count": 25,
            "total_amount": 125000.00,
            "avg_amount": 5000.00,
            "median_amount": 3000.00,
            "max_amount": 25000.00,
            "min_amount": 500.00,
            "std_amount": 5000.00,
            "active_days": 30,
            "transactions_per_day": 0.83,
            "cross_border_ratio": 0.6,
            "cash_transaction_ratio": 0.1,
            "amount_consistency": 1.0,
            "large_transaction_ratio": 0.2,
            "unique_receivers": 15,
            "unique_receiver_countries": 5,
            "receiver_diversity": 0.75,
            "night_transaction_ratio": 0.05,
            "weekend_transaction_ratio": 0.15,
            "unique_currencies": 3
        },
        "network": {
            "in_degree": 12,
            "out_degree": 8,
            "degree_centrality": 0.0069,
            "in_degree_centrality": 0.0041,
            "out_degree_centrality": 0.0028,
            "betweenness_centrality": 0.00001,
            "closeness_centrality": 0.3234,
            "pagerank": 0.00012,
            "eigenvector_centrality": 0.0023,
            "clustering_coefficient": 0.1456,
            "community": 12
        }
    }
    
    print("\nSending request...")
    response = requests.post(
        f"{BASE_URL}/api/v1/predict",
        json=request_data,
        headers={"Content-Type": "application/json"}
    )
    
    print(f"\nStatus Code: {response.status_code}")
    
    if response.status_code == 200:
        result = response.json()
        print(json.dumps(result, indent=2))
        print("\n📊 COORDINATED DECISION:")
        print(f"  Action: {result['action']}")
        print(f"  Confidence: {result['confidence']:.3f}")
        print(f"  Q-Value: {result['maddpg_q_value']:.3f}")
        print(f"  Processing Time: {result['processing_time_ms']:.2f}ms")
        
        print("\n🤖 AGENT OBSERVATIONS:")
        print(f"  Transaction: {result['transaction_agent_observation']['probability']:.3f} (score: {result['transaction_agent_observation']['risk_score']:.1f})")
        print(f"  Customer: {result['customer_agent_observation']['probability']:.3f} (score: {result['customer_agent_observation']['risk_score']:.1f})")
        print(f"  Network: {result['network_agent_observation']['probability']:.3f} (score: {result['network_agent_observation']['risk_score']:.1f})")
        
        print("\n🎯 AGENT CONTRIBUTIONS:")
        for agent, contribution in result['agent_contributions'].items():
            print(f"  {agent.capitalize()}: {contribution:.3f}")
        
        print("\n" + "="*60)
        print("✅ Test Passed!")
        print("="*60)
    else:
        print(f"\n❌ Test Failed!")
        print(response.text)

if __name__ == "__main__":
    print("\n🚀 MARL Orchestrator Test Suite")
    print("="*60)
    
    try:
        test_health()
        test_coordinated_prediction()
    except requests.exceptions.ConnectionError:
        print("\n❌ Error: Could not connect to MARL Orchestrator")
        print("Make sure the service is running on http://localhost:1004")
    except Exception as e:
        print(f"\n❌ Error: {str(e)}")
