#!/bin/bash
# Complete setup script for Avro Schemas, Kafka Topics, and Infrastructure

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=================================================="
echo "🚀 COMPLETE KAFKA & SCHEMA SETUP"
echo "=================================================="
echo ""

# Step 1: Start Kafka Infrastructure
echo "📦 Step 1: Starting Kafka Infrastructure..."
echo "---------------------------------------------------"
cd "$PROJECT_ROOT"
if ! docker ps | grep -q kafka; then
    echo "Starting Kafka, Zookeeper, and Schema Registry..."
    docker-compose up -d zookeeper kafka schema-registry
    echo "Waiting for services to be ready (30 seconds)..."
    sleep 30
else
    echo "✅ Kafka infrastructure already running"
fi
echo ""

# Step 2: Validate Avro Schemas
echo "✅ Step 2: Validating Avro Schemas..."
echo "---------------------------------------------------"
bash "$SCRIPT_DIR/validate-schemas.sh"
echo ""

# Step 3: Generate Java and Python Classes
echo "🔧 Step 3: Generating Java and Python Classes from Avro Schemas..."
echo "---------------------------------------------------"
bash "$SCRIPT_DIR/generate-all.sh"
echo ""

# Step 4: Register Schemas to Schema Registry
echo "📝 Step 4: Registering Schemas to Schema Registry..."
echo "---------------------------------------------------"
bash "$SCRIPT_DIR/register-schemas.sh"
echo ""

# Step 5: Create Kafka Topics
echo "📬 Step 5: Creating Kafka Topics..."
echo "---------------------------------------------------"
bash "$SCRIPT_DIR/create-kafka-topics.sh"
echo ""

# Step 6: Verify Setup
echo "🔍 Step 6: Verifying Setup..."
echo "---------------------------------------------------"
echo ""

echo "Registered Schemas:"
curl -s http://localhost:8081/subjects | jq -r '.[]' | sort
echo ""

echo "Created Topics:"
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list | sort
echo ""

echo "=================================================="
echo "✅ SETUP COMPLETE!"
echo "=================================================="
echo ""
echo "🎯 Access Points:"
echo "  • Schema Registry API: http://localhost:8081"
echo "  • Kafka UI: http://localhost:8080"
echo "  • Kafdrop: http://localhost:9000"
echo ""
echo "📚 Next Steps:"
echo "  1. Start backend services:"
echo "     cd $PROJECT_ROOT/../../bank-solution-backend"
echo "     docker-compose up -d"
echo ""
echo "  2. Start AI services:"
echo "     cd $PROJECT_ROOT/../../ai-services"
echo "     docker-compose up -d"
echo ""
echo "  3. Run payment-engine-svc locally for testing:"
echo "     cd $PROJECT_ROOT/../../bank-solution-backend/payment-engine-service/payment-engine-svc"
echo "     ./gradlew bootRun"
echo ""
echo "     Axon tables will be auto-created in payment_engine_db:"
echo "       • domain_event_entry"
echo "       • snapshot_event_entry"
echo "       • association_value_entry"
echo "       • saga_entry"
echo "       • token_entry"
echo ""
