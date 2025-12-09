#!/bin/bash
# Create all required Kafka topics for the Banking Solution

set -e

# Configuration
KAFKA_BROKER="${KAFKA_BROKER:-localhost:9092}"
PARTITIONS="${PARTITIONS:-3}"
REPLICATION_FACTOR="${REPLICATION_FACTOR:-1}"

echo "🚀 Creating Kafka Topics"
echo "Kafka Broker: $KAFKA_BROKER"
echo "Partitions: $PARTITIONS"
echo "Replication Factor: $REPLICATION_FACTOR"
echo ""

# Function to create a topic
create_topic() {
    local topic_name=$1
    local description=$2
    
    echo "Creating topic: $topic_name"
    echo "  Description: $description"
    
    if docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
        --create \
        --topic "$topic_name" \
        --partitions "$PARTITIONS" \
        --replication-factor "$REPLICATION_FACTOR" \
        --if-not-exists 2>/dev/null; then
        echo "  ✅ Created successfully"
    else
        echo "  ⚠️  Topic may already exist or creation failed"
    fi
    echo ""
}

# Check if Kafka is running
echo "Checking Kafka connectivity..."
if ! docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    echo "❌ Error: Cannot connect to Kafka at $KAFKA_BROKER"
    echo ""
    echo "Make sure Kafka is running:"
    echo "  cd libraries/avro-schema-library"
    echo "  docker-compose up -d kafka"
    exit 1
fi
echo "✅ Kafka is reachable"
echo ""

echo "=================================================="
echo "PAYMENT SERVICE TOPICS"
echo "=================================================="
create_topic "payment-created-events" "Payment creation events from payment-service"

echo "=================================================="
echo "PAYMENT ENGINE SERVICE TOPICS"
echo "=================================================="
create_topic "risk.check.request" "Risk check requests from payment-engine-svc to AI services"
create_topic "risk.check.response" "Risk check responses from AI services to payment-engine-svc"
create_topic "payment-snapshot-events" "Payment aggregate snapshots for event sourcing"

echo "=================================================="
echo "FRAUD DETECTION TOPICS (AI Services)"
echo "=================================================="
create_topic "fraud.detection.request" "Fraud detection requests to MARL orchestrator"
create_topic "fraud.detection.response" "Fraud detection responses from MARL orchestrator"

echo "=================================================="
echo "AGENT SPECIFIC TOPICS (Optional - for direct agent communication)"
echo "=================================================="
create_topic "transaction.pattern.analysis" "Transaction pattern agent results"
create_topic "customer.risk.analysis" "Customer risk agent results"
create_topic "network.topology.analysis" "Network analysis agent results"

echo "=================================================="
echo "PAYMENT LIFECYCLE EVENT TOPICS"
echo "=================================================="
create_topic "payment.completed.events" "Payment completion events"
create_topic "payment.blocked.events" "Payment blocked events"
create_topic "payment.review.required.events" "Payments requiring manual review"

echo "=================================================="
echo "ACCOUNT SERVICE TOPICS"
echo "=================================================="
create_topic "account.balance.updated" "Account balance update events"
create_topic "account.created" "Account creation events"

echo "=================================================="
echo "NETWORK TOPOLOGY TOPICS"
echo "=================================================="
create_topic "network.topology.updated" "Network topology update events"
create_topic "network.graph.computed" "Computed network graphs"

echo ""
echo "=================================================="
echo "✅ ALL TOPICS CREATED SUCCESSFULLY!"
echo "=================================================="
echo ""
echo "List all topics:"
echo "  docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list"
echo ""
echo "Describe a topic:"
echo "  docker exec kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic <topic-name>"
echo ""
echo "View topics in Kafka UI:"
echo "  http://localhost:8080"
echo ""
