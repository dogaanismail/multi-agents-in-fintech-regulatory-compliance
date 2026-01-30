#!/bin/bash
set -e

echo "======================================"
echo "🔄 FULL REBUILD AND REDEPLOY SCRIPT"
echo "======================================"

# Step 1: Regenerate Avro Java classes
echo ""
echo "📦 Step 1: Regenerating Avro schemas..."
cd /Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/libraries/avro-schema-library/scripts
./generate-java.sh

# Step 2: Deregister all schemas from Schema Registry
echo ""
echo "🗑️ Step 2: Deregistering all schemas from Schema Registry..."
SCHEMA_REGISTRY_URL="http://localhost:8081"

# Get all subjects
SUBJECTS=$(curl -s "$SCHEMA_REGISTRY_URL/subjects" | tr -d '[]"' | tr ',' '\n')

for SUBJECT in $SUBJECTS; do
    if [ -n "$SUBJECT" ]; then
        echo "  Deleting subject: $SUBJECT"
        curl -s -X DELETE "$SCHEMA_REGISTRY_URL/subjects/$SUBJECT?permanent=true" > /dev/null 2>&1 || true
    fi
done

echo "  All schemas deregistered."

# Step 3: Re-register schemas
echo ""
echo "📝 Step 3: Re-registering schemas..."
cd /Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/libraries/avro-schema-library/scripts
./register-schemas.sh

# Step 4: Rebuild bank-solution-backend services
echo ""
echo "🔨 Step 4: Building bank-solution-backend services..."
cd /Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/bank-solution-backend

./gradlew clean build -x test --parallel

# Step 5: Rebuild Docker images
echo ""
echo "🐳 Step 5: Rebuilding Docker images..."

docker-compose build --no-cache \
    payment-service \
    payment-engine-service \
    customer-profile-svc \
    network-topology-svc \
    risk-engine-service \
    account-service \
    customer-service \
    payment-history-service

# Step 6: Restart all services
echo ""
echo "🚀 Step 6: Restarting all services..."

docker-compose up -d \
    payment-service \
    payment-engine-service \
    customer-profile-svc \
    network-topology-svc \
    risk-engine-service \
    account-service \
    customer-service \
    payment-history-service

# Step 7: Rebuild AI services (if needed)
echo ""
echo "🤖 Step 7: Rebuilding AI services..."
cd /Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/ai-services

docker-compose build --no-cache marl-orchestrator
docker-compose up -d marl-orchestrator

echo ""
echo "======================================"
echo "✅ REBUILD AND REDEPLOY COMPLETE!"
echo "======================================"
echo ""
echo "📊 Checking service status..."
cd /Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance/bank-solution-backend
docker-compose ps