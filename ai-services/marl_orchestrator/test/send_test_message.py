#!/usr/bin/env python3
"""Send test message to Kafka with proper Avro encoding"""
import sys
sys.path.insert(0, '/Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/ai-services/marl_orchestrator')

import requests
from confluent_kafka import avro
from confluent_kafka.avro import AvroProducer

# Test data matching actual Avro schema
test_data = {
    "requestId": "test-request-001",
    "transactionId": "TXN-1996-06-30-001",
    "timestamp": 1733108400000,
    "transactionFeatures": {
        "transactionId": "TXN-1996-06-30-001",
        "date": "1996-06-30",
        "time": "23:30:00",
        "fromBank": "CRYPTO_EXCHANGE_BANK",
        "fromAccount": "ACC-SENDER-001",
        "toBank": "OFFSHORE_BANK_RU",
        "toAccount": "ACC-RECEIVER-001",
        "amountReceived": 9500.0,
        "receivingCurrency": "USD",
        "amountPaid": 9500.0,
        "paymentCurrency": "USD",
        "paymentType": "Wire",
        "senderBankLocation": "US",
        "receiverBankLocation": "RU"
    },
    "customerFeatures": {
        "customerId": "CUST-HIGH-RISK-001",
        "accountId": "ACC-SENDER-001",
        "transactionCount": 150,
        "totalAmount": 375000.0,
        "avgAmount": 2500.0,
        "medianAmount": 2000.0,
        "maxAmount": 15000.0,
        "minAmount": 500.0,
        "stdAmount": 3500.0,
        "activeDays": 45,
        "transactionsPerDay": 3.33,
        "crossBorderRatio": 0.85,
        "cashTransactionRatio": 0.15,
        "amountConsistency": 0.45,
        "largeTransactionRatio": 0.12,
        "uniqueReceivers": 23,
        "uniqueReceiverCountries": 8,
        "receiverDiversity": 0.78,
        "nightTransactionRatio": 0.35,
        "weekendTransactionRatio": 0.25,
        "uniqueCurrencies": 3
    },
    "networkFeatures": {
        "accountId": "ACC-SENDER-001",
        "inDegree": 45,
        "outDegree": 67,
        "degreeCentrality": 0.65,
        "inDegreeCentrality": 0.58,
        "outDegreeCentrality": 0.72,
        "betweennessCentrality": 0.48,
        "closenessCentrality": 0.55,
        "pagerank": 0.042,
        "eigenvectorCentrality": 0.38,
        "clusteringCoefficient": 0.22,
        "community": 12
    }
}

print("🚀 Sending test message to Kafka...")
print(f"Topic: fraud.detection.request")
print(f"Key: {test_data['requestId']}")

# Fetch schema from Schema Registry
print("\n📥 Fetching schema from Schema Registry...")
schema_registry_url = 'http://localhost:8081'
# Use the correct subject name with dots to match Kafka topic naming convention
response = requests.get(f"{schema_registry_url}/subjects/fraud.detection.request-value/versions/latest")
response.raise_for_status()
schema_str = response.json()['schema']
value_schema = avro.loads(schema_str)
print("✅ Schema loaded")

# Create producer with schema
print("\n🔧 Creating AvroProducer...")
# Define string schema for key
key_schema = avro.loads('{"type": "string"}')
producer = AvroProducer({
    'bootstrap.servers': 'localhost:9092',
    'schema.registry.url': schema_registry_url
}, default_key_schema=key_schema, default_value_schema=value_schema)

# Produce message
print(f"\n📤 Producing message...")
producer.produce(
    topic='fraud.detection.request',
    key=test_data['requestId'],
    value=test_data
)

print("⏳ Flushing...")
producer.flush(timeout=10)

print("✅ Message sent successfully!")
print("\n📊 Check the orchestrator logs for processing...")
