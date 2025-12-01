#!/bin/bash
# Start Kafka, Schema Registry, and supporting services

set -e

cd "$(dirname "$0")/.."

echo "🚀 Starting Kafka Infrastructure..."
echo ""

# Start services
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
echo ""

# Wait for Schema Registry
max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if curl -s http://localhost:8081/ > /dev/null 2>&1; then
        echo "✅ Schema Registry is ready!"
        break
    fi
    
    attempt=$((attempt + 1))
    echo "   Attempt $attempt/$max_attempts - waiting for Schema Registry..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Schema Registry failed to start"
    exit 1
fi

echo ""
echo "=================================================="
echo "✅ INFRASTRUCTURE READY!"
echo "=================================================="
echo ""
echo "Services:"
echo "  • Kafka Broker:      localhost:9092"
echo "  • Schema Registry:   http://localhost:8081"
echo "  • Kafka UI:          http://localhost:8080"
echo ""
echo "Next steps:"
echo "  1. Register schemas: ./scripts/register-schemas.sh"
echo "  2. View logs: docker-compose logs -f"
echo "  3. Stop services: docker-compose down"
