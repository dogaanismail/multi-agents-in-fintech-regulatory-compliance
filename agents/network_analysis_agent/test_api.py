"""
Test script for Network Analysis Agent API
"""

import requests
import json
from pathlib import Path

# API base URL
BASE_URL = "http://localhost:8003"
API_URL = f"{BASE_URL}/api/v1"


def print_section(title):
    """Print a section header"""
    print("\n" + "="*80)
    print(f"  {title}")
    print("="*80)


def test_root():
    """Test root endpoint"""
    print_section("Testing Root Endpoint")
    response = requests.get(BASE_URL)
    print(f"Status: {response.status_code}")
    print(json.dumps(response.json(), indent=2))


def test_health():
    """Test health check endpoint"""
    print_section("Testing Health Check")
    response = requests.get(f"{API_URL}/health")
    print(f"Status: {response.status_code}")
    print(json.dumps(response.json(), indent=2))


def test_model_info():
    """Test model info endpoint"""
    print_section("Testing Model Info")
    response = requests.get(f"{API_URL}/model")
    print(f"Status: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"Model: {data['model_name']}")
        print(f"Type: {data['model_type']}")
        print(f"Features: {data['num_features']}")
        print(f"ROC-AUC: {data['performance_metrics']['roc_auc']:.4f}")
        print(f"Network Nodes: {data['network_stats']['nodes']:,}")
        print(f"Network Edges: {data['network_stats']['edges']:,}")


def test_features():
    """Test feature names endpoint"""
    print_section("Testing Feature Names")
    response = requests.get(f"{API_URL}/model/features")
    print(f"Status: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"Number of features: {data['num_features']}")
        print(f"Features: {', '.join(data['feature_names'])}")


def test_prediction_example():
    """Test prediction example endpoint"""
    print_section("Testing Prediction Example")
    response = requests.get(f"{API_URL}/predict/example")
    print(f"Status: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print("Request Example:")
        print(json.dumps(data['request_example'], indent=2))
        print("\nResponse Example:")
        print(json.dumps(data['response_example'], indent=2))


def test_single_prediction(filepath):
    """Test single account prediction"""
    print_section(f"Testing Single Prediction: {filepath.name}")
    
    with open(filepath, 'r') as f:
        account_data = json.load(f)
    
    print(f"Account ID: {account_data['account_id']}")
    
    response = requests.post(
        f"{API_URL}/predict",
        json=account_data
    )
    
    print(f"Status: {response.status_code}")
    
    if response.status_code == 200:
        result = response.json()
        print(f"\n🎯 Prediction Results:")
        print(f"   Suspicious: {result['is_suspicious']}")
        print(f"   Probability: {result['suspicion_probability']:.4f}")
        print(f"   Risk Score: {result['risk_score']:.2f}")
        print(f"   Risk Level: {result['risk_level']}")
        print(f"   Confidence: {result['confidence']}")
        print(f"   Recommendation: {result['recommendation']}")
        
        if result.get('network_indicators'):
            print(f"\n🔗 Network Indicators:")
            indicators = result['network_indicators']
            
            print(f"   Centrality Metrics:")
            for key, value in indicators['centrality_metrics'].items():
                print(f"      {key}: {value:.6f}")
            
            print(f"   Connectivity:")
            for key, value in indicators['connectivity'].items():
                print(f"      {key}: {value}")
            
            if indicators.get('risk_flags'):
                print(f"   Risk Flags:")
                for flag in indicators['risk_flags']:
                    print(f"      - {flag}")
    else:
        print(f"Error: {response.text}")


def test_batch_prediction():
    """Test batch prediction"""
    print_section("Testing Batch Prediction")
    
    # Load example files
    examples_dir = Path(__file__).parent / "examples"
    high_risk_file = examples_dir / "high_risk_account.json"
    low_risk_file = examples_dir / "low_risk_account.json"
    
    with open(high_risk_file, 'r') as f:
        high_risk = json.load(f)
    
    with open(low_risk_file, 'r') as f:
        low_risk = json.load(f)
    
    # Create batch request
    batch_data = {
        "accounts": [high_risk, low_risk]
    }
    
    response = requests.post(
        f"{API_URL}/predict/batch",
        json=batch_data
    )
    
    print(f"Status: {response.status_code}")
    
    if response.status_code == 200:
        result = response.json()
        print(f"\n📊 Batch Results:")
        print(f"   Total Accounts: {result['total_accounts']}")
        print(f"   Suspicious: {result['suspicious_count']}")
        print(f"   Normal: {result['normal_count']}")
        print(f"   Average Risk Score: {result['average_risk_score']:.2f}")
        print(f"   Processing Time: {result['processing_time_ms']:.2f}ms")
        
        print(f"\n   Individual Results:")
        for pred in result['predictions']:
            print(f"   - {pred['account_id']}: {pred['risk_level']} "
                  f"(score: {pred['risk_score']:.2f}, "
                  f"suspicious: {pred['is_suspicious']})")
    else:
        print(f"Error: {response.text}")


def main():
    """Run all tests"""
    print("\n" + "🧪 Network Analysis Agent API Test Suite")
    print("="*80)
    
    try:
        # Basic endpoints
        test_root()
        test_health()
        test_model_info()
        test_features()
        test_prediction_example()
        
        # Prediction endpoints
        examples_dir = Path(__file__).parent / "examples"
        test_single_prediction(examples_dir / "high_risk_account.json")
        test_single_prediction(examples_dir / "low_risk_account.json")
        test_batch_prediction()
        
        print_section("✅ All Tests Completed")
        
    except requests.exceptions.ConnectionError:
        print("\n❌ Error: Could not connect to API")
        print("   Make sure the server is running: python main.py")
    except Exception as e:
        print(f"\n❌ Error: {str(e)}")


if __name__ == "__main__":
    main()
