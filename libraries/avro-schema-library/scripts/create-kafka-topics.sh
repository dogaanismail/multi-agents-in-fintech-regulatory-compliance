#!/bin/bash
# Create all required Kafka topics for the Banking Solution

set -e

# Configuration
KAFKA_BROKER="${KAFKA_BROKER:-localhost:9092}"
PARTITIONS="${PARTITIONS:-3}"
REPLICATION_FACTOR="${REPLICATION_FACTOR:-1}"

echo "🚀 Creating Kafka Topics for Banking Solution"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Kafka Broker: $KAFKA_BROKER"
echo "Partitions: $PARTITIONS"
echo "Replication Factor: $REPLICATION_FACTOR"
echo ""

# Function to create a topic
create_topic() {
    local topic_name=$1
    local description=$2

    echo "📝 Creating: $topic_name"
    echo "   $description"

    if docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
        --create \
        --topic "$topic_name" \
        --partitions "$PARTITIONS" \
        --replication-factor "$REPLICATION_FACTOR" \
        --if-not-exists 2>/dev/null; then
        echo "   ✅ Created successfully"
    else
        echo "   ℹ️  Already exists or creation skipped"
    fi
    echo ""
}

# Check if Kafka is running
echo "🔍 Checking Kafka connectivity..."
if ! docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    echo "❌ Error: Cannot connect to Kafka at $KAFKA_BROKER"
    echo ""
    echo "Please start Kafka first:"
    echo "  cd libraries/avro-schema-library"
    echo "  docker compose up -d"
    exit 1
fi
echo "✅ Kafka is reachable"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📦 PAYMENT LIFECYCLE TOPICS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
create_topic "payment-created-events" "PaymentCreatedEvent from payment-svc"
create_topic "payment-completed-events" "PaymentCompletedEvent - consumed by customer-profile-svc & network-topology-svc"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🛡️  RISK ASSESSMENT TOPICS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
create_topic "risk.assessment.requested" "RiskAssessmentRequestedEvent from payment-engine-svc to risk-engine-svc"
create_topic "risk.assessment.completed" "RiskAssessmentCompletedEvent from risk-engine-svc to payment-engine-svc"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🤖 FRAUD DETECTION TOPICS (MARL)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
create_topic "fraud.analysis.requested" "FraudAnalysisRequestedEvent from risk-engine-svc to marl-orchestrator"
create_topic "fraud.analysis.completed" "FraudAnalysisCompletedEvent from marl-orchestrator to risk-engine-svc"
create_topic "agent.manual.feedback" "ComplianceAgentManualFeedbackEvent from payment-engine-svc to marl-orchestrator (compliance officer decisions)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🏦 ACCOUNT SERVICE TOPICS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
create_topic "account-created-events" "AccountCreatedEvent from account-svc"
create_topic "account-balance-updated-events" "AccountBalanceUpdatedEvent from account-svc"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "👤 CUSTOMER SERVICE TOPICS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
create_topic "customer-created-events" "CustomerCreatedEvent from customer-svc"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🕸️  NETWORK TOPOLOGY TOPICS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
create_topic "network-topology-updated-events" "Network graph updates from network-topology-svc"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔄 AXON FRAMEWORK EVENT SOURCING TOPICS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
create_topic "payment-events" "Payment aggregate domain events (Axon event store)"
create_topic "payment-snapshots" "Payment aggregate snapshots (Axon snapshots)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "💀 DEAD-LETTER TOPICS (poison / retry-exhausted messages)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
create_topic "account.charge.requested.DLT" "Dead-letter for account-svc consumer"
create_topic "payment-created-events.DLT" "Dead-letter for payment-engine-svc consumer"
create_topic "account.charge.completed.DLT" "Dead-letter for payment-engine-svc consumer"
create_topic "risk.assessment.completed.DLT" "Dead-letter for payment-engine-svc consumer"
create_topic "risk.assessment.requested.DLT" "Dead-letter for risk-engine-svc consumer"
create_topic "fraud.analysis.completed.DLT" "Dead-letter for risk-engine-svc consumer"
create_topic "payment-completed-events.DLT" "Dead-letter for customer-profile-svc & network-topology-svc consumers"
create_topic "payment-snapshot-events.DLT" "Dead-letter for payment-history-svc consumer"
create_topic "fraud.analysis.requested.DLT" "Dead-letter for marl-orchestrator consumer"
create_topic "agent.manual.feedback.DLT" "Dead-letter for marl-orchestrator consumer"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ ALL TOPICS CREATED SUCCESSFULLY!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 Useful Commands:"
echo ""
echo "List all topics:"
echo "  docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list"
echo ""
echo "Describe a specific topic:"
echo "  docker exec kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic payment-completed-events"
echo ""
echo "View messages in a topic (from beginning):"
echo "  docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment-completed-events --from-beginning"
echo ""
echo "View messages with Avro deserialization:"
echo "  docker exec kafka kafka-avro-console-consumer --bootstrap-server localhost:9092 --topic payment-completed-events --property schema.registry.url=http://schema-registry:8081 --from-beginning"
echo ""
echo "Check Kafka UI:"
echo "  http://localhost:8080"
echo ""
echo "Check Schema Registry:"
echo "  http://localhost:8081"
echo ""