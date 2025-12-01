#!/bin/bash
# Register Avro schemas to Confluent Schema Registry

set -e

# Configuration
SCHEMA_REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:8081}"
SCHEMAS_DIR="$(cd "$(dirname "$0")/../schemas" && pwd)"

echo "📝 Registering Avro schemas to Schema Registry"
echo "Schema Registry URL: $SCHEMA_REGISTRY_URL"
echo ""

# Function to register a schema
register_schema() {
    local schema_file=$1
    local subject=$2
    
    echo "Registering: $subject"
    
    # Read schema and escape for JSON
    schema_content=$(cat "$schema_file" | jq -c .)
    
    # Create JSON payload
    payload=$(jq -n \
        --arg schema "$schema_content" \
        '{schema: $schema}')
    
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

# Register fraud detection schemas
echo "Fraud Detection Schemas:"
register_schema "$SCHEMAS_DIR/fraud/FraudDetectionRequest.avsc" "fraud-detection-request-value"
register_schema "$SCHEMAS_DIR/fraud/FraudDetectionResponse.avsc" "fraud-detection-response-value"
echo ""

# Register payment event schemas
echo "Payment Event Schemas:"
register_schema "$SCHEMAS_DIR/payment/PaymentCreatedEvent.avsc" "payment-created-event-value"
register_schema "$SCHEMAS_DIR/payment/PaymentCompletedEvent.avsc" "payment-completed-event-value"
register_schema "$SCHEMAS_DIR/payment/PaymentBlockedEvent.avsc" "payment-blocked-event-value"
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
