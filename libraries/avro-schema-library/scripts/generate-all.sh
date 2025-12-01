#!/bin/bash
# Generate both Java and Python code from Avro schemas

set -e

echo "🚀 Generating code for all languages..."
echo ""

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Generate Java
echo "=================================================="
echo "1. JAVA CODE GENERATION"
echo "=================================================="
bash "$SCRIPT_DIR/generate-java.sh"

echo ""
echo "=================================================="
echo "2. PYTHON PACKAGE BUILD"
echo "=================================================="
bash "$SCRIPT_DIR/generate-python.sh"

echo ""
echo "=================================================="
echo "✅ ALL CODE GENERATED SUCCESSFULLY!"
echo "=================================================="
echo ""
echo "Next steps:"
echo "  1. For Java services:"
echo "     - Add dependency: implementation project(':libraries:avro-schema-library:java')"
echo "  2. For Python services:"
echo "     - Install: pip install -e libraries/avro-schema-library/python"
echo "  3. Register schemas to Schema Registry:"
echo "     - Run: ./scripts/register-schemas.sh"
