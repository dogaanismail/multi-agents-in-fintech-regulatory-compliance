#!/bin/bash
# Register Avro schemas to Confluent Schema Registry

set -e

# Configuration
SCHEMA_REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:8081}"
SCHEMAS_DIR="$(cd "$(dirname "$0")/../schemas" && pwd)"

echo "📝 Registering Avro schemas to Schema Registry"
echo "Schema Registry URL: $SCHEMA_REGISTRY_URL"
echo ""

# Function to register a schema with references
register_schema_with_refs() {
    local schema_file=$1
    local subject=$2
    shift 2
    local refs_json="$@"
    
    echo "Registering: $subject"
    
    # Read schema
    schema_content=$(cat "$schema_file" | jq -c .)
    
    # Build payload with references
    if [ -z "$refs_json" ]; then
        payload=$(jq -n --arg schema "$schema_content" '{schema: $schema, references: []}')
    else
        payload=$(jq -n --arg schema "$schema_content" --argjson refs "$refs_json" '{schema: $schema, references: $refs}')
    fi
    
    # Register to Schema Registry
    response=$(curl -s -X POST \
        -H "Content-Type: application/vnd.schemaregistry.v1+json" \
        --data "$payload" \
        "$SCHEMA_REGISTRY_URL/subjects/$subject/versions")
    
    # Check response
    if echo "$response" | jq -e .id > /dev/null 2>&1; then
        schema_id=$(echo "$response" | jq -r .id)
        echo "  ✅ Registered with ID: $schema_id"
    else
        echo "  ❌ Failed: $response"
        return 1
    fi
}

# Function to register a schema (backward compatibility)
register_schema() {
    register_schema_with_refs "$1" "$2" ""
}

# Check if Schema Registry is available
echo "Checking Schema Registry connectivity..."
if ! curl -s "$SCHEMA_REGISTRY_URL" > /dev/null 2>&1; then
    echo "❌ Error: Cannot connect to Schema Registry at $SCHEMA_REGISTRY_URL"
    echo ""
    echo "Make sure Schema Registry is running:"
    echo "  docker-compose up -d schema-registry"
    exit 1
fi
echo "✅ Schema Registry is reachable"
echo ""

# Register base/dependency schemas first (in order)
echo "Base Schemas (Dependencies):"
register_schema "$SCHEMAS_DIR/transaction/TransactionFeatures.avsc" "transaction-features-value"
register_schema "$SCHEMAS_DIR/customer/CustomerFeatures.avsc" "customer-features-value"
register_schema "$SCHEMAS_DIR/network/NetworkFeatures.avsc" "network-features-value"
register_schema "$SCHEMAS_DIR/agents/AgentObservation.avsc" "agent-observation-value"
echo ""

# Register fraud detection schemas (using dot notation to match Kafka topic names)
echo "Fraud Detection Schemas:"
register_schema "$SCHEMAS_DIR/fraud/FraudDetectionRequest.avsc" "fraud.detection.request-value"
register_schema "$SCHEMAS_DIR/fraud/FraudDetectionResponse.avsc" "fraud.detection.response-value"
echo ""

# Register payment event schemas
echo "Payment Event Schemas:"
register_schema "$SCHEMAS_DIR/payment/PaymentCreatedEvent.avsc" "payment-created-event-value"
register_schema "$SCHEMAS_DIR/payment/PaymentCompletedEvent.avsc" "payment-completed-event-value"
register_schema "$SCHEMAS_DIR/payment/PaymentBlockedEvent.avsc" "payment-blocked-event-value"
register_schema "$SCHEMAS_DIR/payment/PaymentReviewRequiredEvent.avsc" "payment-review-required-event-value"
echo ""

echo "=================================================="
echo "✅ ALL SCHEMAS REGISTERED SUCCESSFULLY!"
echo "=================================================="
echo ""
echo "View registered schemas:"
echo "  curl $SCHEMA_REGISTRY_URL/subjects"
echo ""
echo "Schema Registry UI:"
echo "  http://localhost:8081"
