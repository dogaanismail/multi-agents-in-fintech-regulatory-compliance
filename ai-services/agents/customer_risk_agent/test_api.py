"""
Test script for Customer Risk Agent API
"""

import requests
import json
import time

BASE_URL = "http://localhost:1002"
API_PREFIX = "/api/v1"

def test_health():
    """Test health endpoint"""
    print("\n🔍 Testing health endpoint...")
    response = requests.get(f"{BASE_URL}{API_PREFIX}/health")
    print(f"Status Code: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    return response.status_code == 200

def test_model_info():
    """Test model info endpoint"""
    print("\n🔍 Testing model info endpoint...")
    response = requests.get(f"{BASE_URL}{API_PREFIX}/model-info")
    print(f"Status Code: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"Model Name: {data['model_name']}")
        print(f"Training Date: {data['training_date']}")
        print(f"Features: {data['num_features']}")
        print(f"Metrics: {json.dumps(data['performance_metrics'], indent=2)}")
    return response.status_code == 200

def test_single_prediction_high_risk():
    """Test single customer risk assessment (high risk)"""
    print("\n🔍 Testing single customer assessment (HIGH RISK)...")
    
    with open('examples/high_risk_customer.json', 'r') as f:
        customer_data = json.load(f)
    
    response = requests.post(
        f"{BASE_URL}{API_PREFIX}/assess-risk",
        json=customer_data
    )
    
    print(f"Status Code: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"\n📊 Risk Assessment for {data['customer_id']}:")
        print(f"   Is High Risk: {data['is_high_risk']}")
        print(f"   Risk Probability: {data['risk_probability']:.2%}")
        print(f"   Risk Score: {data['risk_score']:.1f}/100")
        print(f"   Risk Level: {data['risk_level']}")
        print(f"   Confidence: {data['confidence']}")
        print(f"   Recommendation: {data['recommendation']}")
        if data.get('contributing_factors'):
            print(f"\n   Contributing Factors:")
            for factor in data['contributing_factors']:
                print(f"      • {factor}")
    
    return response.status_code == 200

def test_single_prediction_low_risk():
    """Test single customer risk assessment (low risk)"""
    print("\n🔍 Testing single customer assessment (LOW RISK)...")
    
    with open('examples/low_risk_customer.json', 'r') as f:
        customer_data = json.load(f)
    
    response = requests.post(
        f"{BASE_URL}{API_PREFIX}/assess-risk",
        json=customer_data
    )
    
    print(f"Status Code: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"\n📊 Risk Assessment for {data['customer_id']}:")
        print(f"   Is High Risk: {data['is_high_risk']}")
        print(f"   Risk Probability: {data['risk_probability']:.2%}")
        print(f"   Risk Score: {data['risk_score']:.1f}/100")
        print(f"   Risk Level: {data['risk_level']}")
        print(f"   Confidence: {data['confidence']}")
        print(f"   Recommendation: {data['recommendation']}")
    
    return response.status_code == 200

def test_batch_prediction():
    """Test batch customer risk assessment"""
    print("\n🔍 Testing batch customer assessment...")
    
    # Load both example customers
    with open('examples/high_risk_customer.json', 'r') as f:
        customer1 = json.load(f)
    
    with open('examples/low_risk_customer.json', 'r') as f:
        customer2 = json.load(f)
    
    batch_data = {
        "customers": [customer1, customer2]
    }
    
    response = requests.post(
        f"{BASE_URL}{API_PREFIX}/batch-assess-risk",
        json=batch_data
    )
    
    print(f"Status Code: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"\n📊 Batch Assessment Results:")
        print(f"   Total Customers: {data['total_customers']}")
        print(f"   High Risk: {data['high_risk_count']}")
        print(f"   Low Risk: {data['low_risk_count']}")
        print(f"   Average Risk Score: {data['average_risk_score']:.2f}")
        print(f"   Processing Time: {data['processing_time_ms']:.2f}ms")
        
        print(f"\n   Individual Results:")
        for pred in data['predictions']:
            print(f"      {pred['customer_id']}: {pred['risk_level']} ({pred['risk_score']:.1f}/100)")
    
    return response.status_code == 200

def main():
    """Run all tests"""
    print("=" * 70)
    print("🚀 Customer Risk Agent API - Test Suite")
    print("=" * 70)
    
    # Wait for server to be ready
    print("\n⏳ Waiting for server to start...")
    max_retries = 10
    for i in range(max_retries):
        try:
            response = requests.get(f"{BASE_URL}/")
            if response.status_code == 200:
                print("✅ Server is ready!")
                break
        except requests.exceptions.ConnectionError:
            if i < max_retries - 1:
                print(f"   Retry {i+1}/{max_retries}...")
                time.sleep(2)
            else:
                print("❌ Server not responding. Please start the server first:")
                print("   python main.py")
                return
    
    # Run tests
    results = []
    results.append(("Health Check", test_health()))
    results.append(("Model Info", test_model_info()))
    results.append(("Single Prediction (High Risk)", test_single_prediction_high_risk()))
    results.append(("Single Prediction (Low Risk)", test_single_prediction_low_risk()))
    results.append(("Batch Prediction", test_batch_prediction()))
    
    # Summary
    print("\n" + "=" * 70)
    print("📊 Test Summary")
    print("=" * 70)
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for test_name, result in results:
        status = "✅ PASSED" if result else "❌ FAILED"
        print(f"{status}: {test_name}")
    
    print(f"\nTotal: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n🎉 All tests passed successfully!")
    else:
        print("\n⚠️  Some tests failed. Please check the output above.")

if __name__ == "__main__":
    main()
